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

/// 接收来自 JVM 物理边界推送的 Chunk 级平面原始数据，通过强类型安全映射进行无拷贝处理
#[no_mangle]
pub extern "system" fn Java_me_voltual_mcl_core_MclSqliteSaver_writeChunkFast(
    mut env: JNIEnv,
    _class: JClass,
    cx: jint,
    cy: jint,
    cz: jint,
    block_ids: JShortArray,      // 修改点：直接使用强类型包装器类型
    param1: JByteArray,          // 修改点：直接使用强类型包装器类型
    param2: JByteArray,          // 修改点：直接使用强类型包装器类型
    local_names_json: JByteArray,// 修改点：直接使用强类型包装器类型
    metadata_json: JByteArray,   // 修改点：直接使用强类型包装器类型
) -> jboolean {
    // 1. 安全转换字节数组 (通过隐式强类型借用 &JByteArray)
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

    // 2. 利用强类型借用 `&JShortArray` / `&JByteArray` 获取原生临界区指针（Critical Lock）
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
    // 安全构造 Rust 内存切片（100% 堆上零拷贝！）
    // 提示：Java 的 byte 是有符号 i8，我们需要通过原生指针对齐转换为 Rust 期待的无符号 u8
    let ids_slice = unsafe { std::slice::from_raw_parts(ids_ptr.as_ptr() as *const i16, 4096) };
    let p1_slice = unsafe { std::slice::from_raw_parts(p1_ptr.as_ptr() as *const u8, 4096) };
    let p2_slice = unsafe { std::slice::from_raw_parts(p2_ptr.as_ptr() as *const u8, 4096) };

    // 执行多线程高并发压缩与 Minetest 区块组协议组装
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
            error!("Failed to lock JVM memory array pointers.");
            (false, None)
        }
    };

    // 显式释放 JNI 临界区锁定（Critical Lock），防止虚拟机由于 GC 暂停而挂起
    drop(raw_ids);
    drop(raw_p1);
    drop(raw_p2);

    if !ok_status {
        return jni::sys::JNI_FALSE;
    }

    // 3. 提交至全局的 SQLite 物理事务中
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
    *global_map = None; // 触发析构并执行 Connection 自动关闭
}