use std::collections::HashMap;
use std::path::Path;
use rusqlite::{params, Connection};
use byteorder::{BigEndian, WriteBytesExt};
use flate2::write::ZlibEncoder;
use flate2::Compression;
use std::io::Write;
use serde::{Serialize, Deserialize};

use crate::mc_map::{MCBlock, NODES_PER_BLOCK};
use crate::convert::{get_conversion, CONTENT_AIR, REGISTRY};

pub const SER_FMT_VER_HIGHEST_WRITE: u8 = 25;
pub const BLOCK_Y_OFFSET: i32 = 4;

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
    z * -0x1000000 + y * 0x1000 + x * -1
}

pub struct MTMap {
    conn: Connection,
}

impl MTMap {
    /// 纯粹的数据库初始化通道：供 JVM/Kotlin 分片写入（MclSqliteSaver）使用
    pub fn new_from_db_path(db_path: &Path, _spawn_pos: (i32, i32, i32)) -> Result<Self, String> {
        let parent = db_path.parent().unwrap_or(Path::new("."));
        std::fs::create_dir_all(parent).map_err(|e| format!("Failed to create output dir: {}", e))?;

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
    /// 包含 world.mt、单节点世界生成器和 init.lua 出生点保护脚本的完整建立
    pub fn new<P: AsRef<Path>>(path: P, spawn_pos: (i32, i32, i32)) -> Result<Self, String> {
        let path = path.as_ref();
        
        // 1. 确保世界输出根目录存在
        std::fs::create_dir_all(path).map_err(|e| format!("Failed to create output dir: {}", e))?;

        // 2. 写入 world.mt 配置文件 (包含后端定义和静态出生点)
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

        // 3. 写入强制单节点生成器和出生点劫持 Lua 脚本 (放置在世界专属 mods 目录)
        let mod_dir = path.join("worldmods").join("__mc2mt");
        std::fs::create_dir_all(&mod_dir).map_err(|e| e.to_string())?;
        
        let init_lua_path = mod_dir.join("init.lua");
        let init_lua_content = format!(
            "minetest.set_mapgen_params({{chunksize = 1}})\n\
             minetest.set_mapgen_params({{mgname = 'singlenode'}})\n\
             \n\
             -- 强制出生点保护，防止初次加载掉落虚空\n\
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

        // 4. 调用底层的数据库连接建立流程
        let db_path = path.join("map.sqlite");
        Self::new_from_db_path(&db_path, spawn_pos)
    }

    /// JNI 直接单条快速高并发无阻塞安全缓冲
    pub fn save_block_direct(&mut self, pos: MTPos, data: &[u8]) -> Result<(), String> {
        let key = encode_pos(pos);
        self.conn.execute(
            "INSERT OR REPLACE INTO blocks (pos, data) VALUES (?, ?)",
            params![key, data],
        ).map_err(|e| format!("SQLite direct insert failed: {}", e))?;
        Ok(())
    }

    /// JNI 的冲刷与事务重置
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

// =========================================================================
// 高并发 Fast-JNI 区块压缩并封装 Minetest v25 序列化协议的方法
// =========================================================================

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

/// 直接使用来自 Java 物理内存边界的安全序列化打包函数
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
    // 【核心修正 4】：完全 1:1 无损坐标映射，抛弃旧 C++ 错误的反转逻辑，杜绝左右颠倒和区块错位
    let mt_pos = MTPos {
        x: cx as i16, 
        y: (cy - BLOCK_Y_OFFSET) as i16,
        z: cz as i16,
    };

    let mut data = Vec::with_capacity(8192);
    data.write_u8(SER_FMT_VER_HIGHEST_WRITE).unwrap();

    let mut flags = 0x02u8; // day_night_differs
    if mt_pos.y < 0 {
        flags |= 0x01; // is_underground
    }
    data.write_u8(flags).unwrap();
    data.write_u8(2).unwrap(); // content_width
    data.write_u8(2).unwrap(); // params_width

    // 2. 压缩节点流 (此时由于 Kotlin 已经按照 ZYX 排序了，我们直接写入，内存访问极为高效！)
    let mut node_buffer = Vec::with_capacity(NODES_PER_BLOCK * 4);
    for &id in block_ids {
        node_buffer.write_u16::<BigEndian>(id as u16).unwrap();
    }
    node_buffer.write_all(param1).unwrap();
    node_buffer.write_all(param2).unwrap();

    let mut encoder = ZlibEncoder::new(Vec::new(), Compression::default());
    encoder.write_all(&node_buffer).unwrap();
    data.write_all(&encoder.finish().unwrap()).unwrap();

    // 3. 反序列化 JVM 写入的 BlockEntity 缓存并编码为 Minetest 原生二进制 Metadata
    let metadata: HashMap<i32, JniBlockEntity> = serde_json::from_slice(metadata_json_bytes)
        .unwrap_or_else(|_| HashMap::new());

    let mut meta_buffer = Vec::new();
    if metadata.is_empty() {
        meta_buffer.write_u8(0).unwrap(); // 空元数据版本标志
    } else {
        meta_buffer.write_u8(1).unwrap(); // Version = 1
        meta_buffer.write_u16::<BigEndian>(metadata.len() as u16).unwrap();
        for (idx, m_val) in metadata {
            // 【核心修正 5】：因为 Kotlin 传过来的 idx 已经是完美的 Minetest [Z][Y][X] 索引
            // 我们直接把它强转回 u16 供存储引擎使用，无需重新推算错位
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

    // 4. 写入静态实体
    data.write_u8(0).unwrap();
    data.write_u16::<BigEndian>(0).unwrap();

    // 5. 写入时间戳
    data.write_u32::<BigEndian>(0xFFFFFFFF).unwrap();

    // 6. 写入 Name-ID 字典
    data.write_u8(0).unwrap();
    data.write_u16::<BigEndian>(local_names.len() as u16).unwrap();
    for (i, name) in local_names.iter().enumerate() {
        data.write_u16::<BigEndian>(i as u16).unwrap();
        data.write_u16::<BigEndian>(name.len() as u16).unwrap();
        data.write_all(name.as_bytes()).unwrap();
    }

    // 7. 写入定时器
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

/// 保持对原有 MCBlock 纯 Rust 解析支持，完美防止 Cli 构建报错破坏
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