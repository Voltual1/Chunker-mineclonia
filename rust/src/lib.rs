pub mod mc_map;
pub mod convert;
pub mod mt_map;

use jni::objects::{JClass, JString, JByteArray, JShortArray};
use jni::sys::{jboolean, jint};
use jni::JNIEnv;
use log::{error, info};
use std::sync::Mutex;
use once_cell::sync::Lazy;
use std::path::Path;

use crate::mc_map::MCMap;
use crate::mt_map::{MTMap, serialize_raw_chunk, serialize_block};

// 使用全局锁安全托管 MTMap 实例
static GLOBAL_MT_MAP: Lazy<Mutex<Option<MTMap>>> = Lazy::new(|| Mutex::new(None));

#[no_mangle]
pub extern "system" fn JNI_OnLoad(_vm: jni::JavaVM, _reserved: *mut std::ffi::c_void) -> jni::sys::jint {
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Info)
            .with_tag("MC2MT_Rust"),
    );
    info!("MC2MT Fast-JNI Bridge Loaded Successfully");
    jni::sys::JNI_VERSION_1_6
}

// =========================================================================
// 统一命名空间：全部绑定 to me.voltual.mc2mt.MC2MTLib
// =========================================================================

#[no_mangle]
pub extern "system" fn Java_me_voltual_mc2mt_MC2MTLib_convertMap<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    input_path: JString<'local>,
    output_path: JString<'local>,
    callback: jni::objects::JObject<'local>,
) -> jboolean {
    let input: String = match env.get_string(&input_path) {
        Ok(s) => s.into(),
        Err(_) => return jni::sys::JNI_FALSE,
    };

    let output: String = match env.get_string(&output_path) {
        Ok(s) => s.into(),
        Err(_) => return jni::sys::JNI_FALSE,
    };

    info!("Starting raw JNI map convert from {} to {}", input, output);

    let mc_map = match MCMap::new(&input) {
        Ok(m) => m,
        Err(e) => {
            error!("MCMap initialization failed natively: {}", e);
            return jni::sys::JNI_FALSE;
        }
    };

    let mc_spawn = mc_map.get_spawn_point();
    let mt_spawn = (
        mc_spawn.0,
        mc_spawn.1 - crate::mt_map::BLOCK_Y_OFFSET + 1,
        mc_spawn.2
    );

    let mut mt_map = match MTMap::new(&output, mt_spawn) {
        Ok(m) => m,
        Err(e) => {
            error!("MTMap initialization failed natively: {}", e);
            return jni::sys::JNI_FALSE;
        }
    };

    let groups = match mc_map.list_groups() {
        Ok(g) => g,
        Err(e) => {
            error!("Listing groups failed natively: {}", e);
            return jni::sys::JNI_FALSE;
        }
    };

    if groups.is_empty() {
        error!("No valid Region files found natively in {}", input);
        return jni::sys::JNI_FALSE;
    }

    let total_groups = groups.len() as i64;

    let mut report_progress = |g_done: i64, b_done: i64| {
        if callback.is_null() {
            return;
        }
        let _ = env.call_method(
            &callback,
            "onProgress",
            "(JJJ)V",
            &[
                jni::objects::JValue::Long(g_done),
                jni::objects::JValue::Long(total_groups),
                jni::objects::JValue::Long(b_done),
            ],
        );
    };

    let mut blocks_done = 0i64;

    use rayon::prelude::*;

    for (i, group) in groups.iter().enumerate() {
        let step = i as i64;
        
        if let Ok(chunk_positions) = mc_map.list_chunks(group) {
            let transformed_blocks: Vec<(crate::mt_map::MTPos, Vec<u8>)> = chunk_positions
                .par_iter()
                .filter_map(|&pos| mc_map.load_chunk(group, pos).ok())
                .flat_map(|mc_blocks| mc_blocks)
                .filter_map(|mcb| serialize_block(&mcb).ok())
                .collect();

            let count = transformed_blocks.len() as i64;

            if !transformed_blocks.is_empty() {
                if let Err(e) = mt_map.save_blocks(transformed_blocks) {
                    error!("Database write failed in region group {}: {}", group.name, e);
                } else {
                    blocks_done += count;
                }
            }
        }
        report_progress(step, blocks_done);
    }

    let _ = mt_map.flush_transaction();
    report_progress(total_groups, blocks_done);

    jni::sys::JNI_TRUE
}

/// 初始化全局的 Minetest 数据库写出引擎
/// 增强：失败时向 JVM 抛出包含详细 Rust 错误上下文的 RuntimeException 异常
#[no_mangle]
pub extern "system" fn Java_me_voltual_mc2mt_MC2MTLib_initNativeEngine(
    mut env: JNIEnv,
    _class: JClass,
    db_path: JString,
    spawn_x: jint,
    spawn_y: jint,
    spawn_z: jint,
) -> jboolean {
    let path_str: String = match env.get_string(&db_path) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new("java/lang/RuntimeException", "Failed to resolve db_path parameter from JVM");
            return jni::sys::JNI_FALSE;
        }
    };

    let path = Path::new(&path_str);
    let spawn_pos = (spawn_x as i32, spawn_y as i32, spawn_z as i32);

    match MTMap::new_from_db_path(path, spawn_pos) {
        Ok(map) => {
            let mut global_map = GLOBAL_MT_MAP.lock().unwrap();
            *global_map = Some(map);
            jni::sys::JNI_TRUE
        }
        Err(e) => {
            error!("Failed to initialize native MTMap engine: {}", e);
            // 直接将 Rust 的错误穿透抛给 Java 层，诊断信息彻底透明
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                &format!("Failed to initialize native MTMap engine (Rust details): {}", e)
            );
            jni::sys::JNI_FALSE
        }
    }
}

/// 接收来自 JVM 的 Chunk 数据并高效拷贝合并
#[no_mangle]
pub extern "system" fn Java_me_voltual_mc2mt_MC2MTLib_writeChunkFast(
    env: JNIEnv,
    _class: JClass,
    cx: jint,
    cy: jint,
    cz: jint,
    block_ids: JShortArray,
    param1: JByteArray,
    param2: JByteArray,
    local_names_json: JByteArray,
    metadata_json: JByteArray,
) -> jboolean {
    let local_names: Vec<String> = match env.convert_byte_array(&local_names_json) {
        Ok(bytes) => serde_json::from_slice(&bytes).unwrap_or_default(),
        Err(_) => return jni::sys::JNI_FALSE,
    };

    let metadata_bytes = env.convert_byte_array(&metadata_json).unwrap_or_default();

    let mut ids_vec = vec![0i16; 4096];
    if env.get_short_array_region(&block_ids, 0, &mut ids_vec).is_err() {
        error!("Failed to copy block_ids region from Java to Rust");
        return jni::sys::JNI_FALSE;
    }

    let mut p1_vec_signed = vec![0i8; 4096];
    if env.get_byte_array_region(&param1, 0, &mut p1_vec_signed).is_err() {
        error!("Failed to copy param1 region from Java to Rust");
        return jni::sys::JNI_FALSE;
    }

    let mut p2_vec_signed = vec![0i8; 4096];
    if env.get_byte_array_region(&param2, 0, &mut p2_vec_signed).is_err() {
        error!("Failed to copy param2 region from Java to Rust");
        return jni::sys::JNI_FALSE;
    }

    let p1_slice = unsafe { std::slice::from_raw_parts(p1_vec_signed.as_ptr() as *const u8, 4096) };
    let p2_slice = unsafe { std::slice::from_raw_parts(p2_vec_signed.as_ptr() as *const u8, 4096) };

    let chunk_result = match serialize_raw_chunk(
        cx as i32,
        cy as i32,
        cz as i32,
        &ids_vec,
        p1_slice,
        p2_slice,
        local_names,
        &metadata_bytes,
    ) {
        Ok(res) => Some(res),
        Err(e) => {
            error!("Chunk serialization failed: {}", e);
            None
        }
    };

    let (pos, serialized_data) = match chunk_result {
        Some(d) => d,
        None => return jni::sys::JNI_FALSE,
    };

    let mut global_map = GLOBAL_MT_MAP.lock().unwrap();
    if let Some(ref mut map) = *global_map {
        if let Err(e) = map.save_block_direct(pos, &serialized_data) {
            error!("SQLite insert failed: {}", e);
            return jni::sys::JNI_FALSE;
        }
    } else {
        error!("Global native map engine uninitialized");
        return jni::sys::JNI_FALSE;
    }

    jni::sys::JNI_TRUE
}

/// 提交并冲刷当前的 SQLite 事务
#[no_mangle]
pub extern "system" fn Java_me_voltual_mc2mt_MC2MTLib_flushNativeEngine(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    let mut global_map = GLOBAL_MT_MAP.lock().unwrap();
    if let Some(ref mut map) = *global_map {
        match map.flush_transaction() {
            Ok(_) => jni::sys::JNI_TRUE,
            Err(e) => {
                error!("Native flush failed: {}", e);
                jni::sys::JNI_FALSE
            }
        }
    } else {
        jni::sys::JNI_FALSE
    }
}

/// 关闭 Native 资源并关闭 SQLite 连接
#[no_mangle]
pub extern "system" fn Java_me_voltual_mc2mt_MC2MTLib_closeNativeEngine(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut global_map = GLOBAL_MT_MAP.lock().unwrap();
    *global_map = None; 
}