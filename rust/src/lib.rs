pub mod mc_map;
pub mod convert;
pub mod mt_map;

use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jint, jshortArray, jbyteArray, jlong};
use jni::JNIEnv;
use log::{error, info};
use std::sync::Mutex;
use once_cell::sync::Lazy;
use std::path::Path;

use crate::mt_map::{MTMap, MTPos, serialize_raw_chunk};

// 使用全局的全局锁安全托管 MTMap 实例，使 Kotlin/Java 能够任意驱动转换生存期
static GLOBAL_MT_MAP: Lazy<Mutex<Option<MTMap>>> = Lazy::new(|| Mutex::new(None));

#[no_mangle]
pub extern "system" fn JNI_OnLoad(_vm: jni::JavaVM, _reserved: *mut std::ffi::c_void) -> jni::sys::jint {
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Info)
            .with_tag("MC2MT_Rust"),
    );
    info!("MC2MT Fast-JNI Bridge Loaded");
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

/// 接收来自 JVM 物理边界推送的 Chunk 级高密度平面原始数据，直接执行零拷贝处理与数据库落盘
#[no_mangle]
pub extern "system" fn Java_me_voltual_mcl_core_MclSqliteSaver_writeChunkFast(
    mut env: JNIEnv,
    _class: JClass,
    cx: jint,
    cy: jint,
    cz: jint,
    block_ids: jshortArray,
    param1: jbyteArray,
    param2: jbyteArray,
    local_names_json: jbyteArray,
    metadata_json: jbyteArray,
) -> jboolean {
    // 1. 获取本地映射名字表与 BlockEntity NBT JSON
    let names_bytes = match env.convert_byte_array(&local_names_json) {
        Ok(b) => b,
        Err(_) => return jni::sys::JNI_FALSE,
    };
    let local_names: Vec<String> = match serde_json::from_slice(&names_bytes) {
        Ok(n) => n,
        Err(_) => return jni::sys::JNI_FALSE,
    };

    let metadata_bytes = match env.convert_byte_array(&metadata_json) {
        Ok(b) => b,
        Err(_) => return jni::sys::JNI_FALSE,
    };

    // 2. 利用 Critical 锁提取超大物理内存指针，绕过 JVM 的 GC 性能开销
    let raw_ids = unsafe {
        env.get_array_elements_critical(&block_ids, jni::objects::ReleaseMode::NoCopyBack)
    };
    let raw_p1 = unsafe {
        env.get_array_elements_critical(&param1, jni::objects::ReleaseMode::NoCopyBack)
    };
    let raw_p2 = unsafe {
        env.get_array_elements_critical(&param2, jni::objects::ReleaseMode::NoCopyBack)
    };

    let (ok_status, chunk_result) = match (&raw_ids, &raw_p1, &raw_p2) {
        (Ok(ids_ptr), Ok(p1_ptr), Ok(p2_ptr)) => {
            // 安全构造 Rust 切片映射（完全零拷贝！）
            let ids_slice = unsafe { std::slice::from_raw_parts(ids_ptr.as_ptr(), 4096) };
            let p1_slice = unsafe { std::slice::from_raw_parts(p1_ptr.as_ptr(), 4096) };
            let p2_slice = unsafe { std::slice::from_raw_parts(p2_ptr.as_ptr(), 4096) };

            // 执行高度并发优化的原生 Zlib 与数据打包
            match serialize_raw_chunk(
                cx as i32,
                cy as i32,
                cz as i32,
                ids_slice,
                p1_slice,
                p2_slice,
                local_names,
                &metadata_bytes,
            ) {
                Ok(res) => (true, Some(res)),
                Err(e) => {
                    error!("Raw chunk serialization error: {}", e);
                    (false, None)
                }
            }
        }
        _ => {
            error!("Failed to locking Java memory arrays safely.");
            (false, None)
        }
    };

    // 保证在出错或成功时都释放 JVM 指针以防内存锁泄漏
    drop(raw_ids);
    drop(raw_p1);
    drop(raw_p2);

    if !ok_status {
        return jni::sys::JNI_FALSE;
    }

    // 3. 直接在原生安全上下文中，将转换好的高密度区块刷入本地高速事务中
    if let Some((pos, serialized_data)) = chunk_result {
        let mut global_map = GLOBAL_MT_MAP.lock().unwrap();
        if let Some(ref mut map) = *global_map {
            if let Err(e) = map.save_block_direct(pos, &serialized_data) {
                error!("Native SQLite save block direct failed: {}", e);
                return jni::sys::JNI_FALSE;
            }
        } else {
            error!("Native global map engine has not been initialized yet.");
            return jni::sys::JNI_FALSE;
        }
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
                error!("Failed to flush transaction natively: {}", e);
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
    *global_map = None; // 原生结构析构，自动安全触发 Rust 线程安全的 connection.close()
}