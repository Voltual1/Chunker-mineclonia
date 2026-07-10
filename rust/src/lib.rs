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

/// 接收来自 JVM 物理边界推送的 Chunk 级平面原始数据，通过分段锁与强类型安全映射进行无拷贝处理
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

    // 2. 利用局部变量生命周期拆分，顺序获取 JVM 内存指针，绕过 mutable borrow 独占限制
    let (ids_ptr, p1_ptr, p2_ptr) = unsafe {
        // 第一段：提取 Block IDs 并复制其原始裸指针
        let ids_gate = match env.get_array_elements_critical(&block_ids, jni::objects::ReleaseMode::NoCopyBack) {
            Ok(g) => g,
            Err(_) => return jni::sys::JNI_FALSE,
        };
        let ids_raw = ids_gate.as_ptr() as *const i16;
        
        // 第二段：提取 Param1 并复制其原始裸指针
        let p1_gate = match env.get_array_elements_critical(&param1, jni::objects::ReleaseMode::NoCopyBack) {
            Ok(g) => g,
            Err(_) => {
                drop(ids_gate);
                return jni::sys::JNI_FALSE;
            }
        };
        let p1_raw = p1_gate.as_ptr() as *const u8;

        // 第三段：提取 Param2 并复制其原始裸指针
        let p2_gate = match env.get_array_elements_critical(&param2, jni::objects::ReleaseMode::NoCopyBack) {
            Ok(g) => g,
            Err(_) => {
                drop(ids_gate);
                drop(p1_gate);
                return jni::sys::JNI_FALSE;
            }
        };
        let p2_raw = p2_gate.as_ptr() as *const u8;

        // 将临界区物理锁定包装器作为守卫临时保留在外部，确保在 Rust 序列化完成之前，物理内存不被 JVM 释放或垃圾回收
        (ids_gate, p1_gate, p2_gate, ids_raw, p1_raw, p2_raw)
    };

    // 3. 在完全安全的原生上下文中构造内存切片（100% 零拷贝，完美规避生命周期借用冲突）
    let ids_slice = unsafe { std::slice::from_raw_parts(p2_ptr.3, 4096) };
    let p1_slice = unsafe { std::slice::from_raw_parts(p2_ptr.4, 4096) };
    let p2_slice = unsafe { std::slice::from_raw_parts(p2_ptr.5, 4096) };

    // 4. 执行多线程高并发压缩与 Minetest 区块组协议组装
    let chunk_result = match serialize_raw_chunk(
        cx as i32,
        cy as i32,
        cz as i32,
        ids_slice,
        p1_slice,
        p2_slice,
        local_names,
        &metadata_bytes,
    ) {
        Ok(res) => Some(res),
        Err(e) => {
            error!("Raw chunk serialization error: {}", e);
            None
        }
    };

    // 5. 显式释放 JNI 临界区锁定（保证即使发生 panic 也能在垃圾回收恢复之前安全归还 JVM）
    drop(ids_ptr);
    drop(p1_ptr);
    drop(p2_ptr);

    let chunk_data = match chunk_result {
        Some(data) => data,
        None => return jni::sys::JNI_FALSE,
    };

    // 6. 将转换好的高密度压缩区块提交至原生高速 SQLite 事务
    let mut global_map = GLOBAL_MT_MAP.lock().unwrap();
    if let Some(ref mut map) = *global_map {
        if let Err(e) = map.save_block_direct(chunk_data.0, &chunk_data.1) {
            error!("Native SQLite save block direct failed: {}", e);
            return jni::sys::JNI_FALSE;
        }
    } else {
        error!("Native global map engine has not been initialized yet.");
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