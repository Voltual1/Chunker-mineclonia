use std::collections::HashMap;
use std::path::Path;
use rusqlite::{params, Connection};
use byteorder::{BigEndian, WriteBytesExt};
use flate2::write::ZlibEncoder;
use flate2::Compression;
use std::io::Write;
use serde::{Serialize, Deserialize};
use std::ffi::{CString, CStr};

use crate::mc_map::{MCBlock, NODES_PER_BLOCK};
use crate::convert::{get_conversion, CONTENT_AIR, REGISTRY};

pub const SER_FMT_VER_HIGHEST_WRITE: u8 = 25;
pub const BLOCK_Y_OFFSET: i32 = 4;

// =========================================================================
// SQLite Recover 扩展底层 C FFI 声明
// =========================================================================
extern "C" {
    pub fn sqlite3_recover_init(
        db: *mut std::ffi::c_void,
        zDb: *const std::os::raw::c_char,
        zLostAndFound: *const std::os::raw::c_char,
    ) -> *mut std::ffi::c_void;

    pub fn sqlite3_recover_config(
        p: *mut std::ffi::c_void,
        op: std::os::raw::c_int,
        pArg: *mut std::ffi::c_void,
    ) -> std::os::raw::c_int;

    pub fn sqlite3_recover_step(p: *mut std::ffi::c_void) -> std::os::raw::c_int;

    pub fn sqlite3_recover_errcode(p: *mut std::ffi::c_void) -> std::os::raw::c_int;

    pub fn sqlite3_recover_errmsg(p: *mut std::ffi::c_void) -> *const std::os::raw::c_char;

    pub fn sqlite3_recover_clean(p: *mut std::ffi::c_void) -> std::os::raw::c_int;
}

/// 快速检测 SQLite 文件的完整性。
/// 采用 quick_check(1)，只捕获第一个错误，极速响应，适合每次启动前快速扫描。
pub fn check_db_integrity(db_path: &Path) -> bool {
    if !db_path.exists() {
        return true; // 文件不存在意味着无需修复，直接新建即可
    }

    let conn = match Connection::open(db_path) {
        Ok(c) => c,
        Err(_) => return false, // 无法正常 Open 句柄说明头部已损坏
    };

    let mut stmt = match conn.prepare("PRAGMA quick_check(1);") {
        Ok(s) => s,
        Err(_) => return false,
    };

    let mut rows = match stmt.query([]) {
        Ok(r) => r,
        Err(_) => return false,
    };

    if let Ok(Some(row)) = rows.next() {
        if let Ok(res) = row.get::<_, String>(0) {
            return res.eq_ignore_ascii_case("ok");
        }
    }

    false
}

/// 运行底层 `recover` 引擎，将损坏的数据库修复并写入新的数据库。
pub fn run_recovery(corrupted_path: &Path, recovered_path: &Path) -> Result<(), String> {
    // 1. 创建并打开全新的、干净的目标数据库句柄
    let db_out = Connection::open(recovered_path)
        .map_err(|e| format!("Failed to open empty output DB for recovery: {}", e))?;

    // 2. 转换损坏数据库的文件路径为 CString
    let corrupted_str = corrupted_path.to_str()
        .ok_or_else(|| "Invalid non-UTF8 database path".to_string())?;
    let c_corrupted_path = CString::new(corrupted_str)
        .map_err(|e| e.to_string())?;

    // 3. 提取 rusqlite 托管的底层 raw sqlite3* 句柄
    let raw_db_out = db_out.handle() as *mut std::ffi::c_void;

    unsafe {
        // 4. 初始化恢复器。zLostAndFound 传入 NULL，代表使用默认配置
        let recover_ptr = sqlite3_recover_init(raw_db_out, c_corrupted_path.as_ptr(), std::ptr::null());
        if recover_ptr.is_null() {
            return Err("Failed to initialize SQLite recover instance (returned NULL)".to_string());
        }

        log::info!("SQLite recover engine initialized. Restructuring database...");

        // 5. 循环执行恢复步骤
        loop {
            let rc = sqlite3_recover_step(recover_ptr);
            if rc == 101 { // SQLITE_DONE
                break;
            } else if rc == 0 { // SQLITE_OK
                continue;
            } else {
                // 发生内部异常，捕获详细错误信息
                let err_code = sqlite3_recover_errcode(recover_ptr);
                let err_msg_ptr = sqlite3_recover_errmsg(recover_ptr);
                let err_msg = if !err_msg_ptr.is_null() {
                    CStr::from_ptr(err_msg_ptr).to_string_lossy().into_owned()
                } else {
                    "No diagnostic error message".to_string()
                };

                sqlite3_recover_clean(recover_ptr);
                return Err(format!("Step-recovery failed (rc: {}, code: {}): {}", rc, err_code, err_msg));
            }
        }

        // 6. 清理恢复器句柄
        let clean_rc = sqlite3_recover_clean(recover_ptr);
        if clean_rc != 0 {
            return Err(format!("SQLite recover clean failed with code {}", clean_rc));
        }
    }

    Ok(())
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct MTPos {
    pub x: i16,
    pub y: i16,
    pub z: i16,
}

#[inline]
pub fn encode_pos(pos: MTPos) -> i64 {
    let z = pos.z as i64;
    let y = pos.y as i64;
    let x = pos.x as i64;
    z * 0x1000000 + y * 0x1000 + x
}

pub struct MTMap {
    conn: Connection,
}

impl MTMap {
    /// 纯粹的数据库初始化通道：供 JVM/Kotlin 分片写入（MclSqliteSaver）使用
    pub fn new_from_db_path(db_path: &Path, _spawn_pos: (i32, i32, i32)) -> Result<Self, String> {
        let parent = db_path.parent().unwrap_or(Path::new("."));
        std::fs::create_dir_all(parent).map_err(|e| format!("Failed to create output dir: {}", e))?;

        // =========================================================================
        // 核心改动：原子损坏检测与自动恢复逻辑
        // =========================================================================
        if db_path.exists() {
            log::info!("Starting database integrity scan on {:?}", db_path);
            if !check_db_integrity(db_path) {
                log::warn!("CRITICAL: Database {:?} is corrupted! Activating auto-recovery pipeline...", db_path);

                let corrupted_backup = db_path.with_extension("sqlite.corrupted");
                if corrupted_backup.exists() {
                    std::fs::remove_file(&corrupted_backup)
                        .map_err(|e| format!("Failed to clean stale backup: {}", e))?;
                }

                // 备份损坏现场
                std::fs::rename(db_path, &corrupted_backup)
                    .map_err(|e| format!("Failed to isolate corrupted database: {}", e))?;

                // 启动安全提取与修复
                match run_recovery(&corrupted_backup, db_path) {
                    Ok(_) => {
                        log::info!("Database recover succeeded. Cleaning up corrupted isolated data.");
                        let _ = std::fs::remove_file(&corrupted_backup);
                    }
                    Err(err_msg) => {
                        log::error!("Database recover failed: {}. Restoring safe-snapshot...", err_msg);
                        // 还原现场，以保留物理文件让外部工具介入诊断
                        let _ = std::fs::rename(&corrupted_backup, db_path);
                        return Err(format!("SQLite programmatic recover failed: {}", err_msg));
                    }
                }
            } else {
                log::info!("Database integrity verified successfully.");
            }
        }

        let conn = Connection::open(&db_path)
            .map_err(|e| format!("Failed to open SQLite database: {}", e))?;

        conn.execute_batch(
            "PRAGMA synchronous = OFF;
             PRAGMA journal_mode = MEMORY;
             CREATE TABLE IF NOT EXISTS blocks (
                pos INT PRIMARY KEY,
                data BLOB
             );
             BEGIN TRANSACTION;" // 显式开启高速批量事务
        ).map_err(|e| format!("Failed to initialize database: {}", e))?;

        Ok(MTMap { conn })
    }

    /// 完整世界目录初始化通道：供 Rust 顶层 convertMap（MC2MTLib）调用
    pub fn new<P: AsRef<Path>>(path: P, spawn_pos: (i32, i32, i32)) -> Result<Self, String> {
        let path = path.as_ref();
        
        std::fs::create_dir_all(path).map_err(|e| format!("Failed to create output dir: {}", e))?;

        let world_mt_path = path.join("world.mt");
        let world_mt_content = format!(
            "backend = sqlite3\n\
             player_backend = sqlite3\n\
             auth_backend = sqlite3\n\
             mod_storage_backend = sqlite3\n\
             gameid = mineclonia\n\
             static_spawnpoint = ({}, {}, {})\n",
            spawn_pos.0, spawn_pos.1, spawn_pos.2
        );
        std::fs::write(&world_mt_path, world_mt_content).map_err(|e| e.to_string())?;

        let mod_dir = path.join("worldmods").join("__mc2mt");
        std::fs::create_dir_all(&mod_dir).map_err(|e| e.to_string())?;
        
        let init_lua_path = mod_dir.join("init.lua");
        let init_lua_content = format!(
            "minetest.set_mapgen_params({{chunksize = 1}})\n\
             minetest.set_mapgen_params({{mgname = 'singlenode'}})\n\
             \n\
             local spawn_pos = {{x={}, y={}, z={}}}\n\
             minetest.register_on_newplayer(function(player)\n\
                 player:set_pos(spawn_pos)\n\
             end)\n\
             minetest.register_on_respawnplayer(function(player)\n\
                 player:set_pos(spawn_pos)\n\
                 return true\n\
             end)\n",
            spawn_pos.0, spawn_pos.1, spawn_pos.2
        );
        std::fs::write(&init_lua_path, init_lua_content).map_err(|e| e.to_string())?;

        let db_path = path.join("map.sqlite");
        Self::new_from_db_path(&db_path, spawn_pos)
    }

    pub fn save_block_direct(&mut self, pos: MTPos, data: &[u8]) -> Result<(), String> {
        let key = encode_pos(pos);
        self.conn.execute(
            "INSERT OR REPLACE INTO blocks (pos, data) VALUES (?, ?)",
            params![key, data],
        ).map_err(|e| format!("SQLite direct insert failed: {}", e))?;
        Ok(())
    }

    pub fn flush_transaction(&mut self) -> Result<(), String> {
        self.conn.execute_batch(
            "COMMIT;
             PRAGMA shrink_memory;
             BEGIN TRANSACTION;"
        ).map_err(|e| format!("SQLite commit/restart transaction failed: {}", e))?;
        Ok(())
    }

    pub fn save_blocks(&mut self, blocks: Vec<(MTPos, Vec<u8>)>) -> Result<(), String> {
        for (pos, data) in blocks {
            self.save_block_direct(pos, &data)?;
        }
        self.flush_transaction()?;
        Ok(())
    }
}


#[derive(Serialize, Deserialize)]
struct JniInventory {
    width: i32,
    items: Vec<JniItemStack>,
}

#[derive(Serialize, Deserialize)]
struct JniItemStack {
    name: String,
    count: i32,
    wear: i32,
}

#[derive(Serialize, Deserialize)]
struct JniBlockEntity {
    fields: HashMap<String, String>,
    inventories: HashMap<String, JniInventory>,
}

pub fn serialize_raw_chunk(
    cx: i32,
    cy: i32,
    cz: i32,
    block_ids: &[i16],
    param1: &[u8],
    param2: &[u8],
    local_names: Vec<String>,
    metadata_json_bytes: &[u8],
) -> Result<(MTPos, Vec<u8>), String> {    
    let mt_pos = MTPos {
        x: cx as i16, 
        y: (cy - BLOCK_Y_OFFSET) as i16,
        z: cz as i16,
    };

    let mut data = Vec::with_capacity(8192);
    data.write_u8(SER_FMT_VER_HIGHEST_WRITE).unwrap();

    let mut flags = 0x02u8;
    if mt_pos.y < 0 {
        flags |= 0x01;
    }
    data.write_u8(flags).unwrap();
    data.write_u8(2).unwrap();
    data.write_u8(2).unwrap();

    let mut node_buffer = Vec::with_capacity(NODES_PER_BLOCK * 4);
    for &id in block_ids {
        node_buffer.write_u16::<BigEndian>(id as u16).unwrap();
    }
    node_buffer.write_all(param1).unwrap();
    node_buffer.write_all(param2).unwrap();

    let mut encoder = ZlibEncoder::new(Vec::new(), Compression::default());
    encoder.write_all(&node_buffer).unwrap();
    data.write_all(&encoder.finish().unwrap()).unwrap();

    let metadata: HashMap<i32, JniBlockEntity> = serde_json::from_slice(metadata_json_bytes)
        .unwrap_or_else(|_| HashMap::new());

    let mut meta_buffer = Vec::new();
    if metadata.is_empty() {
        meta_buffer.write_u8(0).unwrap();
    } else {
        meta_buffer.write_u8(1).unwrap();
        meta_buffer.write_u16::<BigEndian>(metadata.len() as u16).unwrap();
        for (idx, m_val) in metadata {
            let mt_idx = idx as u16;
            meta_buffer.write_u16::<BigEndian>(mt_idx).unwrap();
            meta_buffer.write_i32::<BigEndian>(m_val.fields.len() as i32).unwrap();
            for (k, v) in m_val.fields {
                write_meta_string(&mut meta_buffer, &k);
                write_meta_long_string(&mut meta_buffer, &v);
            }
            serialize_inventories_to_binary(&mut meta_buffer, m_val.inventories);
        }
    }
    let mut meta_encoder = ZlibEncoder::new(Vec::new(), Compression::default());
    meta_encoder.write_all(&meta_buffer).unwrap();
    data.write_all(&meta_encoder.finish().unwrap()).unwrap();

    data.write_u8(0).unwrap();
    data.write_u16::<BigEndian>(0).unwrap();
    data.write_u32::<BigEndian>(0xFFFFFFFF).unwrap();

    data.write_u8(0).unwrap();
    data.write_u16::<BigEndian>(local_names.len() as u16).unwrap();
    for (i, name) in local_names.iter().enumerate() {
        data.write_u16::<BigEndian>(i as u16).unwrap();
        data.write_u16::<BigEndian>(name.len() as u16).unwrap();
        data.write_all(name.as_bytes()).unwrap();
    }

    data.write_u8(10).unwrap();
    data.write_u16::<BigEndian>(0).unwrap();

    Ok((mt_pos, data))
}

fn write_meta_string(buf: &mut Vec<u8>, s: &str) {
    buf.write_u16::<BigEndian>(s.len() as u16).unwrap();
    buf.write_all(s.as_bytes()).unwrap();
}

fn write_meta_long_string(buf: &mut Vec<u8>, s: &str) {
    buf.write_u32::<BigEndian>(s.len() as u32).unwrap();
    buf.write_all(s.as_bytes()).unwrap();
}

fn serialize_inventories_to_binary(buf: &mut Vec<u8>, invs: HashMap<String, JniInventory>) {
    let mut text_inv = Vec::new();
    for (name, inv) in invs {
        text_inv.write_all(format!("List {} {}\n", name, inv.items.len()).as_bytes()).unwrap();
        text_inv.write_all(format!("Width {}\n", inv.width).as_bytes()).unwrap();
        for item in inv.items {
            if item.count == 0 {
                text_inv.write_all(b"Empty\n").unwrap();
            } else {
                text_inv.write_all(format!("Item {} {} {}\n", item.name, item.count, item.wear).as_bytes()).unwrap();
            }
        }
        text_inv.write_all(b"EndInventoryList\n").unwrap();
    }
    text_inv.write_all(b"EndInventory\n").unwrap();

    buf.write_all(&text_inv).unwrap();
}

pub fn serialize_block(mcb: &MCBlock) -> Result<(MTPos, Vec<u8>), String> {
    let mt_pos = MTPos {
        x: mcb.pos_x as i16,
        y: (mcb.pos_y as i32 - BLOCK_Y_OFFSET) as i16,
        z: mcb.pos_z as i16,
    };

    let mut data = Vec::new();
    data.write_u8(SER_FMT_VER_HIGHEST_WRITE).unwrap();

    let mut flags = 0u8;
    if mt_pos.y < -1 {
        flags |= 0x01;
    }
    flags |= 0x02;
    data.write_u8(flags).unwrap();
    data.write_u8(2).unwrap();
    data.write_u8(2).unwrap();

    let mut local_blocks = vec![0u16; NODES_PER_BLOCK];
    let mut local_param2 = vec![0u8; NODES_PER_BLOCK];
    let mut local_param1 = vec![0u8; NODES_PER_BLOCK];

    let mut sbi_content = Vec::new();
    let mut content_sbi = HashMap::new();
    let mut next_local_id = 0u16;

    for i in 0..NODES_PER_BLOCK {
        let mc_id = mcb.blocks[i];
        let mc_data = mcb.data[i] as u16;

        let conv = get_conversion(mc_id, mc_data).unwrap_or_else(|| {
            crate::convert::ConversionData {
                tool: false,
                param2: 0,
                cid: CONTENT_AIR,
                post_process: crate::convert::PostProcessType::None,
            }
        });

        let global_cid = conv.cid;
        let local_id = *content_sbi.entry(global_cid).or_insert_with(|| {
            let id = next_local_id;
            sbi_content.push(global_cid);
            next_local_id += 1;
            id
        });

        local_blocks[i] = local_id;
        local_param2[i] = conv.param2;
        
        let raw_block_light = mcb.block_light[i];
        let raw_sky_light = mcb.sky_light[i];
        local_param1[i] = (raw_block_light << 4) | raw_block_light.max(raw_sky_light);
    }

    let mut node_buffer = Vec::new();
    for &id in &local_blocks {
        node_buffer.write_u16::<BigEndian>(id).unwrap();
    }
    node_buffer.write_all(&local_param1).unwrap();
    node_buffer.write_all(&local_param2).unwrap();

    let mut encoder = ZlibEncoder::new(Vec::new(), Compression::default());
    encoder.write_all(&node_buffer).unwrap();
    data.write_all(&encoder.finish().unwrap()).unwrap();

    let mut meta_buffer = Vec::new();
    meta_buffer.write_u8(0).unwrap();
    let mut encoder_meta = ZlibEncoder::new(Vec::new(), Compression::default());
    encoder_meta.write_all(&meta_buffer).unwrap();
    data.write_all(&encoder_meta.finish().unwrap()).unwrap();

    data.write_u8(0).unwrap();
    data.write_u16::<BigEndian>(0).unwrap();
    data.write_u32::<BigEndian>(0xFFFFFFFF).unwrap();

    data.write_u8(0).unwrap();
    data.write_u16::<BigEndian>(sbi_content.len() as u16).unwrap();
    
    let registry = REGISTRY.lock().unwrap();
    for (local_id, &global_cid) in sbi_content.iter().enumerate() {
        data.write_u16::<BigEndian>(local_id as u16).unwrap();
        let global_name = registry.id_to_name.get(global_cid as usize)
            .cloned()
            .unwrap_or_else(|| "ignore".to_string());
        data.write_u16::<BigEndian>(global_name.len() as u16).unwrap();
        data.write_all(global_name.as_bytes()).unwrap();
    }

    data.write_u8(2 + 4 + 4).unwrap();
    data.write_u16::<BigEndian>(0).unwrap();

    Ok((mt_pos, data))
}