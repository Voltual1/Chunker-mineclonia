use std::collections::HashMap;
use std::path::Path;
use rusqlite::{params, Connection};
use byteorder::{BigEndian, WriteBytesExt};
use flate2::write::ZlibEncoder;
use flate2::Compression;
use std::io::Write;

use crate::mc_map::{MCBlock, NODES_PER_BLOCK};
use crate::convert::{get_conversion, CONTENT_AIR, REGISTRY};

// =========================================================
// 统一声明的全局常量（对外公开，供 lib.rs 和 main.rs 完美调用）
// =========================================================
pub const SER_FMT_VER_HIGHEST_WRITE: u8 = 25;
pub const BLOCK_Y_OFFSET: i32 = 4; // 转换坐标系统的偏移量，使水面高度对齐

/// 代表 Minetest 中的 3D 坐标
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct MTPos {
    pub x: i16,
    pub y: i16,
    pub z: i16,
}

/// 对齐 C++ 中的 encodePos，计算 SQLite 存储的主键
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
    pub fn new<P: AsRef<Path>>(path: P, spawn_pos: (i32, i32, i32)) -> Result<Self, String> {
        let path = path.as_ref();
        
        // 确保世界输出目录存在
        std::fs::create_dir_all(path).map_err(|e| format!("Failed to create output dir: {}", e))?;

        // 1. 写入 world.mt 配置文件 (包含静态出生点)
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

        // 2. 写入强制单节点生成器和出生点劫持 Lua 脚本
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

        // 3. 初始化 Minetest 格式的 SQLite 数据库
        let db_path = path.join("map.sqlite");
        let conn = Connection::open(&db_path)
            .map_err(|e| format!("Failed to open SQLite database: {}", e))?;

        conn.execute_batch(
            "PRAGMA synchronous = OFF;
             PRAGMA journal_mode = MEMORY;
             CREATE TABLE IF NOT EXISTS blocks (
                pos INT PRIMARY KEY,
                data BLOB
             );"
        ).map_err(|e| format!("Failed to initialize db tables: {}", e))?;

        Ok(MTMap { conn })
    }

    /// 批量安全持久化转换后的 Minetest 块
    pub fn save_blocks(&mut self, blocks: Vec<(MTPos, Vec<u8>)>) -> Result<(), String> {
        let tx = self.conn.transaction()
            .map_err(|e| format!("Failed to start transaction: {}", e))?;
        
        {
            let mut stmt = tx.prepare_cached("INSERT OR REPLACE INTO blocks (pos, data) VALUES (?, ?)")
                .map_err(|e| format!("Failed to prepare statement: {}", e))?;

            for (pos, data) in blocks {
                let key = encode_pos(pos);
                stmt.execute(params![key, data])
                    .map_err(|e| format!("Failed to write block at pos {:?}: {}", pos, e))?;
            }
        }

        tx.commit().map_err(|e| format!("Failed to commit transaction: {}", e))?;
        Ok(())
    }
}

/// 将 MCBlock 翻译序列化为 Minetest 硬盘序列化格式二进制块
pub fn serialize_block(mcb: &MCBlock) -> Result<(MTPos, Vec<u8>), String> {
    // 1. 进行坐标转换
    let mt_pos = MTPos {
        x: mcb.pos_x as i16,
        y: (mcb.pos_y as i32 - BLOCK_Y_OFFSET) as i16,
        z: mcb.pos_z as i16,
    };

    let mut data = Vec::new();
    
    // 写入协议版本
    data.write_u8(SER_FMT_VER_HIGHEST_WRITE).unwrap();

    // 2. 写入区块标志属性
    let mut flags = 0u8;
    if mt_pos.y < -1 {
        flags |= 0x01; // is_underground
    }
    flags |= 0x02; // day_night_differs
    data.write_u8(flags).unwrap();

    // content_width = 2, params_width = 2
    data.write_u8(2).unwrap();
    data.write_u8(2).unwrap();

    // 3. 将本地方块 ID 解析转换，并建立局部的 sbi_content
    let mut local_blocks = vec![0u16; NODES_PER_BLOCK];
    let mut local_param2 = vec![0u8; NODES_PER_BLOCK];
    let mut local_param1 = vec![0u8; NODES_PER_BLOCK];

    // 局部区块映射映射表
    let mut sbi_content = Vec::new();
    let mut content_sbi = HashMap::new();
    let mut next_local_id = 0u16;

    for i in 0..NODES_PER_BLOCK {
        let mc_id = mcb.blocks[i];
        let mc_data = mcb.data[i] as u16;

        let conv = get_conversion(mc_id, mc_data).unwrap_or_else(|| {
            // 回退为空气
            crate::convert::ConversionData {
                tool: false,
                param2: 0,
                cid: CONTENT_AIR,
                post_process: crate::convert::PostProcessType::None,
            }
        });

        // 获取转换后的全局 CID 并分配局部序列化 ID
        let global_cid = conv.cid;
        let local_id = *content_sbi.entry(global_cid).or_insert_with(|| {
            let id = next_local_id;
            sbi_content.push(global_cid);
            next_local_id += 1;
            id
        });

        local_blocks[i] = local_id;
        local_param2[i] = conv.param2;
        
        // param1 (光照处理)：混合日光照和方块光源
        let raw_block_light = mcb.block_light[i];
        let raw_sky_light = mcb.sky_light[i];
        local_param1[i] = (raw_block_light << 4) | raw_block_light.max(raw_sky_light);
    }

    // 4. 压缩并序列化所有 Node 节点 (使用 Zlib)
    let mut node_buffer = Vec::new();
    
    // 写入 param0 (两个字节)
    for &id in &local_blocks {
        node_buffer.write_u16::<BigEndian>(id).unwrap();
    }
    // 写入 param1
    node_buffer.write_all(&local_param1).unwrap();
    // 写入 param2
    node_buffer.write_all(&local_param2).unwrap();

    // 压缩写入主缓冲区
    let mut encoder = ZlibEncoder::new(Vec::new(), Compression::default());
    encoder.write_all(&node_buffer).unwrap();
    let compressed_nodes = encoder.finish().unwrap();
    data.write_all(&compressed_nodes).unwrap();

    // 5. 写入 metadata (由于本重构暂不支持复杂实体，直接写入空节点压缩层)
    let mut meta_buffer = Vec::new();
    meta_buffer.write_u8(0).unwrap(); // Version
    let mut encoder_meta = ZlibEncoder::new(Vec::new(), Compression::default());
    encoder_meta.write_all(&meta_buffer).unwrap();
    let compressed_meta = encoder_meta.finish().unwrap();
    data.write_all(&compressed_meta).unwrap();

    // 6. 静态实体 (Static Objects)
    data.write_u8(0).unwrap(); // Version
    data.write_u16::<BigEndian>(0).unwrap(); // Count = 0

    // 7. 时间戳 (Timestamp)
    data.write_u32::<BigEndian>(0xFFFFFFFF).unwrap(); // BLOCK_TIMESTAMP_UNDEFINED

    // 8. 写入局部映射表的 Name-ID 序列
    data.write_u8(0).unwrap(); // Version
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

    // 9. 节点计时器 (Node Timers)
    data.write_u8(2 + 4 + 4).unwrap(); // Timer length
    data.write_u16::<BigEndian>(0).unwrap(); // Count = 0

    Ok((mt_pos, data))
}