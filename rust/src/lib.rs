use jni::objects::{JClass, JObject, JString};
use jni::sys::jboolean;
use jni::JNIEnv;
use log::{error, info};
use std::sync::atomic::{AtomicI64, Ordering};
use std::thread;
use std::time::Duration;

// 全局进度计数器（模拟原项目的 groups_done 和 blocks_done）
static GROUPS_DONE: AtomicI64 = AtomicI64::new(0);
static BLOCKS_DONE: AtomicI64 = AtomicI64::new(0);

#[no_mangle]
pub extern "system" fn JNI_OnLoad(vm: jni::JavaVM, _reserved: *mut std::ffi::c_void) -> jni::sys::jint {
    // 初始化 Android 系统的日志输出，对应原 android_logger
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Info)
            .with_tag("MC2MT_Rust"),
    );
    info!("MC2MT Rust Library loaded");
    jni::sys::JNI_VERSION_1_6
}

/// 发送进度到 Java 端
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

    info!("Starting conversion:");
    info!("Input path: {}", input);
    info!("Output path: {}", output);

    // 重置全局进度
    GROUPS_DONE.store(0, Ordering::SeqCst);
    BLOCKS_DONE.store(0, Ordering::SeqCst);

    // TODO: 实现以下核心逻辑
    // 1. MCMap::listGroups(&input)
    // 2. MTMap::new(&output)
    // 3. 多线程并行执行转换 (Rayon 线程池)
    // 4. 保存到 SQLite (Rusqlite)

    // ==== 这里暂时放一个模拟转换进度的循环 ====
    let total_groups: i64 = 10; // 模拟有 10 个 Group
    
    for i in 0..total_groups {
        GROUPS_DONE.store(i, Ordering::SeqCst);
        let current_blocks = BLOCKS_DONE.fetch_add(1024, Ordering::SeqCst);
        
        report_progress(&mut env, &callback, i, total_groups, current_blocks);
        
        // 模拟工作耗时
        thread::sleep(Duration::from_millis(200));
    }

    // 完成最后一次汇报
    let final_blocks = BLOCKS_DONE.load(Ordering::SeqCst);
    report_progress(&mut env, &callback, total_groups, total_groups, final_blocks);

    info!("Conversion finished.");
    jni::sys::JNI_TRUE
}