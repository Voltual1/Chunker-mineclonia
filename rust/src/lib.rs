pub mod mc_map;
pub mod convert; 

use jni::objects::{JClass, JObject, JString};
use jni::sys::jboolean;
use jni::JNIEnv;
use log::{error, info};
use std::sync::atomic::{AtomicI64, Ordering};

use crate::mc_map::MCMap;

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

    let _output: String = match env.get_string(&output_path) {
        Ok(s) => s.into(),
        Err(_) => return jni::sys::JNI_FALSE,
    };

    info!("Starting Rust MCMap integration...");

    // 1. 初始化 MC 地图元数据
    let mc_map = match MCMap::new(&input) {
        Ok(m) => m,
        Err(e) => {
            error!("MCMap initialization failed: {}", e);
            return jni::sys::JNI_FALSE;
        }
    };

    // 2. 扫描区块组 (.mca 列表)
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
    info!("Found {} regions to convert", total_groups);

    // 重置全局进度
    GROUPS_DONE.store(0, Ordering::SeqCst);
    BLOCKS_DONE.store(0, Ordering::SeqCst);

    for (i, group) in groups.iter().enumerate() {
        let step = i as i64;
        GROUPS_DONE.store(step, Ordering::SeqCst);
        
        // 扫描并读取有效的 Chunk 坐标
        if let Ok(chunk_positions) = mc_map.list_chunks(group) {
            for pos in chunk_positions {
                if let Ok(blocks) = mc_map.load_chunk(group, pos) {
                    BLOCKS_DONE.fetch_add(blocks.len() as i64, Ordering::SeqCst);
                }
            }
        }

        let current_blocks = BLOCKS_DONE.load(Ordering::SeqCst);
        report_progress(&mut env, &callback, step, total_groups, current_blocks);
    }

    // 完成最后一次进度汇报
    let final_blocks = BLOCKS_DONE.load(Ordering::SeqCst);
    report_progress(&mut env, &callback, total_groups, total_groups, final_blocks);

    info!("Conversion complete!");
    jni::sys::JNI_TRUE
}