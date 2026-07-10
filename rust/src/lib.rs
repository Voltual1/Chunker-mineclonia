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

/// 接收来自 JVM 的 Chunk 数据。采用高效的 Region Copy 模式，完美兼容 Rust 借用检查器。
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

    // 2. 批量拷贝 Chunk 核心数据到 Rust 内存空间
    // 虽然这里发生了拷贝，但 16KB 的内存拷贝对现代 CPU 而言仅需几百纳秒，
    // 相比于 JNI Critical Section 锁定 JVM GC 的风险，这种方式更安全且利于并发。
    let ids_vec: Vec<i16> = match env.convert_short_array(&block_ids) {
        Ok(v) => v,
        Err(_) => return jni::sys::JNI_FALSE,
    };
    
    let p1_vec_signed: Vec<i8> = match env.convert_byte_array(&param1) {
        Ok(v) => v,
        Err(_) => return jni::sys::JNI_FALSE,
    };

    let p2_vec_signed: Vec<i8> = match env.convert_byte_array(&param2) {
        Ok(v) => v,
        Err(_) => return jni::sys::JNI_FALSE,
    };

    // 3. 将有符号的 Java Byte 转换为无符号的 Rust Byte (零开销内存映射)
    let p1_slice = unsafe { std::slice::from_raw_parts(p1_vec_signed.as_ptr() as *const u8, 4096) };
    let p2_slice = unsafe { std::slice::from_raw_parts(p2_vec_signed.as_ptr() as *const u8, 4096) };

    // 4. 调用原生序列化逻辑
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

    // 5. 写入高速 SQLite 事务
    let mut global_map = GLOBAL_MT_MAP.lock().unwrap();
    if let Some(ref mut map) = *global_map {
        if let Err(e) = map.save_block_direct(pos, &serialized_data) {
            error!("SQLite insert failed: {}", e);
            return jni::sys::JNI_FALSE;
        }
    } else {
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