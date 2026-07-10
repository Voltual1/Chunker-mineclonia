pub mod mc_map;
pub mod convert;
pub mod mt_map; // 新增映射注册

use jni::objects::{JClass, JObject, JString};
use jni::sys::jboolean;
use jni::JNIEnv;
use log::{error, info};
use std::sync::atomic::{AtomicI64, Ordering};
use rayon::prelude::*; // 引入高并发并行迭代器支持

use crate::mc_map::MCMap;
use crate::mt_map::{serialize_block, MTMap};

static GROUPS_DONE: AtomicI64 = AtomicI64::new(0);
static BLOCKS_DONE: AtomicI64 = AtomicI64::new(0);

#[no_mangle]
pub extern "system" fn JNI_OnLoad(_vm: jni::JavaVM, _reserved: *mut std::ffi::c_void) -> jni::sys::jint {
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Info)
            .with_tag("MC2MT_Rust"),
    );
    info!("MC2MT Rust Library loaded");
    jni::sys::JNI_VERSION_1_6
}

fn report_progress(env: &mut JNIEnv, callback: &JObject, groups_done: i64, total_groups: i64, blocks_done: i64) {
    if callback.is_null() {
        return;
    }
    let result = env.call_method(
        callback,
        "onProgress",
        "(JJJ)V",
        &[
            jni::objects::JValue::Long(groups_done),
            jni::objects::JValue::Long(total_groups),
            jni::objects::JValue::Long(blocks_done),
        ],
    );

    if let Err(e) = result {
        error!("Failed to call onProgress callback: {:?}", e);
    }
}

#[no_mangle]
pub extern "system" fn Java_me_voltual_mc2mt_MC2MTLib_convertMap(
    mut env: JNIEnv,
    _class: JClass,
    input_path: JString,
    output_path: JString,
    callback: JObject,
) -> jboolean {
    let input: String = match env.get_string(&input_path) {
        Ok(s) => s.into(),
        Err(_) => return jni::sys::JNI_FALSE,
    };

    let output: String = match env.get_string(&output_path) {
        Ok(s) => s.into(),
        Err(_) => return jni::sys::JNI_FALSE,
    };

    info!("Starting ultra-fast Rust Rayon database pipeline...");

    // 1. 初始化输入地图
    let mc_map = match MCMap::new(&input) {
        Ok(m) => m,
        Err(e) => {
            error!("MCMap initialization failed: {}", e);
            return jni::sys::JNI_FALSE;
        }
    };
    
    // 1. 初始化 MC 地图元数据
    let mc_map = match MCMap::new(&input) {
        Ok(m) => m,
        Err(e) => {
            error!("MCMap initialization failed: {}", e);
            return jni::sys::JNI_FALSE;
        }
    };

    // ++ 新增：提取并计算 Minetest 坐标系下的安全出生点 (Y轴高度+1防止卡地里)
    let mc_spawn = mc_map.get_spawn_point();
    let mt_spawn = (
        mc_spawn.0,
        mc_spawn.1 - crate::mt_map::BLOCK_Y_OFFSET + 1,
        mc_spawn.2
    );
    info!("Extracted MC Spawn Point: {:?}, translated to MT Spawn Point: {:?}", mc_spawn, mt_spawn);

    // 2. 初始化输出 Minetest SQLite 数据库
    let mut mt_map = match MTMap::new(&output, mt_spawn) {
        Ok(m) => m,
        Err(e) => {
            error!("MTMap initialization failed: {}", e);
            return jni::sys::JNI_FALSE;
        }
    };

    // 3. 扫描区块组 (.mca 列表)
    let groups = match mc_map.list_groups() {
        Ok(g) => g,
        Err(e) => {
            error!("Listing groups failed: {}", e);
            return jni::sys::JNI_FALSE;
        }
    };

    if groups.is_empty() {
        error!("No valid Region files found in {}", input);
        return jni::sys::JNI_FALSE;
    }

    let total_groups = groups.len() as i64;
    info!("Found {} regions. Executing multithreading pipeline...", total_groups);

    // 重置全局进度
    GROUPS_DONE.store(0, Ordering::SeqCst);
    BLOCKS_DONE.store(0, Ordering::SeqCst);

    for (i, group) in groups.iter().enumerate() {
        let step = i as i64;
        GROUPS_DONE.store(step, Ordering::SeqCst);
        
        // 读取区块内所有的 Chunk 坐标
        if let Ok(chunk_positions) = mc_map.list_chunks(group) {
            
            // ==================================================
            // 使用 Rayon 物理多核心并发地并行转换所有 Chunk 和 Block
            // ==================================================
            let transformed_blocks: Vec<(crate::mt_map::MTPos, Vec<u8>)> = chunk_positions
                .par_iter() // 转换成并行迭代器
                .filter_map(|&pos| {
                    // 读取并将单个 Chunk 切片成一组 MCBlock
                    mc_map.load_chunk(group, pos).ok()
                })
                .flat_map(|mc_blocks| mc_blocks) // 扁平化多级切片
                .filter_map(|mcb| {
                    // 执行序列化与压缩算法，生成 Minetest 数据
                    serialize_block(&mcb).ok()
                })
                .collect(); // 高并发地收集到主线程
            
            let count = transformed_blocks.len() as i64;
            
            // 4. 批量极速写入 SQLite
            if !transformed_blocks.is_empty() {
                if let Err(e) = mt_map.save_blocks(transformed_blocks) {
                    error!("Database write failed in region group {}: {}", group.name, e);
                } else {
                    BLOCKS_DONE.fetch_add(count, Ordering::SeqCst);
                }
            }
        }

        let current_blocks = BLOCKS_DONE.load(Ordering::SeqCst);
        report_progress(&mut env, &callback, step, total_groups, current_blocks);
    }

    // 完成最后一次进度汇报
    let final_blocks = BLOCKS_DONE.load(Ordering::SeqCst);
    report_progress(&mut env, &callback, total_groups, total_groups, final_blocks);

    info!("Database pipeline completed successfully.");
    jni::sys::JNI_TRUE
}