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

use crate::mt_map::{MTMap, serialize_raw_chunk};

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

/// 初始化全局的 Minetest 数据库写出引擎
#[no_mangle]
pub extern "system" fn Java_me_voltual_mcl_core_MclSqliteSaver_initNativeEngine(
    mut env: JNIEnv,
    _class: JClass,
    db_path: JString,
    spawn_x: jint,
    spawn_y: jint,
    spawn_z: jint,
) -> jboolean {
    let path_str: String = match env.get_string(&db_path) {
        Ok(s) => s.into(),
        Err(_) => return jni::sys::JNI_FALSE,
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
            jni::sys::JNI_FALSE
        }
    }
}

/// 接收来自 JVM 的 Chunk 数据。通过对齐 jni-rs 2.0+ 标准，提供超快的高安全无拷贝机制
#[no_mangle]
pub extern "system" fn Java_me_voltual_mcl_core_MclSqliteSaver_writeChunkFast(
    mut env: JNIEnv,
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
    // 1. 转换基础元数据 (JSON)
    let local_names: Vec<String> = match env.convert_byte_array(&local_names_json) {
        Ok(bytes) => serde_json::from_slice(&bytes).unwrap_or_default(),
        Err(_) => return jni::sys::JNI_FALSE,
    };

    let metadata_bytes = env.convert_byte_array(&metadata_json).unwrap_or_default();

    // 2. 将 Short 数组快速拷贝至预先分配的原生 Vec
    let mut ids_vec = vec![0i16; 4096];
    if env.get_short_array_region(&block_ids, 0, &mut ids_vec).is_err() {
        error!("Failed to copy block_ids region from Java to Rust");
        return jni::sys::JNI_FALSE;
    }

    // 3. 提取无符号 Byte 数组，jni-rs 的 convert_byte_array 已经自动转换成了完美的 Vec<u8>！
    let p1_vec: Vec<u8> = match env.convert_byte_array(&param1) {
        Ok(v) => v,
        Err(_) => return jni::sys::JNI_FALSE,
    };

    let p2_vec: Vec<u8> = match env.convert_byte_array(&param2) {
        Ok(v) => v,
        Err(_) => return jni::sys::JNI_FALSE,
    };

    // 4. 执行完全安全的纯 Rust 并发序列化，避开一切指针漏洞
    let chunk_result = match serialize_raw_chunk(
        cx as i32,
        cy as i32,
        cz as i32,
        &ids_vec,
        &p1_vec,
        &p2_vec,
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

    // 5. 将块高速存入原生数据库
    let mut global_map = GLOBAL_MT_MAP.lock().unwrap();
    if let Some(ref mut map) = *global_map {
        if let Err(e) = map.save_block_direct(pos, &serialized_data) {
            error!("SQLite direct insert failed: {}", e);
            return jni::sys::JNI_FALSE;
        }
    } else {
        error!("Global native engine is uninitialized");
        return jni::sys::JNI_FALSE;
    }

    jni::sys::JNI_TRUE
}

/// 提交并冲刷当前的 SQLite 事务
#[no_mangle]
pub extern "system" fn Java_me_voltual_mcl_core_MclSqliteSaver_flushNativeEngine(
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
pub extern "system" fn Java_me_voltual_mcl_core_MclSqliteSaver_closeNativeEngine(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut global_map = GLOBAL_MT_MAP.lock().unwrap();
    *global_map = None; 
}