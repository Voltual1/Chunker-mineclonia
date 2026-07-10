use std::fs::File;
use std::io::{Read, Seek, SeekFrom};
use crate::convert::post_process_blocks; // 导入后处理函数
use std::path::{Path, PathBuf};
use byteorder::{BigEndian, ReadBytesExt};
use flate2::read::{GzDecoder, ZlibDecoder};

// 原项目 Map.hpp 中的配置
pub const MAP_BLOCK_SIZE: usize = 16;
pub const NODES_PER_BLOCK: usize = MAP_BLOCK_SIZE * MAP_BLOCK_SIZE * MAP_BLOCK_SIZE;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MCFormat {
    Regions,
    Anvil,
}

#[derive(Debug, Clone, Copy)]
pub struct MCChunkPos {
    pub x: i32,
    pub z: i32,
}

pub struct MCGroup {
    pub name: String,
    pub x: i32,
    pub z: i32,
    pub format: MCFormat,
    pub file_path: PathBuf,
}

/// 模拟 C++ 的 MCBlock，存储解包后的 16x16x16 区域内 node 属性
pub struct MCBlock {
    pub pos_x: i32,
    pub pos_y: u8,
    pub pos_z: i32,
    pub blocks: Vec<u16>,
    pub data: Vec<u8>,
    pub sky_light: Vec<u8>,
    pub block_light: Vec<u8>,
    pub tile_entities: Vec<NbtTag>, // 暂存对应 Y 轴切片的 TileEntity NBT 节点
}

pub struct MCMap {
    path: PathBuf,
    // 元数据保留供后续提取世界选项
    pub level_dat: NbtTag, 
}

impl MCMap {
    pub fn new<P: AsRef<Path>>(path: P) -> Result<Self, String> {
        let path = path.as_ref().to_path_buf();
        let level_dat_path = path.join("level.dat");
        
        if !level_dat_path.exists() {
            return Err("level.dat not found".to_string());
        }

        let file = File::open(&level_dat_path)
            .map_err(|e| format!("Failed to open level.dat: {}", e))?;
        
        // level.dat 通常是 Gzip 压缩的 NBT
        let mut decoder = GzDecoder::new(file);
        let mut decompressed_data = Vec::new();
        decoder.read_to_end(&mut decompressed_data)
            .map_err(|e| format!("Failed to decompress level.dat: {}", e))?;

        let mut cursor = std::io::Cursor::new(decompressed_data);
        let level_dat = parse_nbt(&mut cursor)
            .map_err(|e| format!("Failed to parse level.dat NBT: {}", e))?;

        Ok(MCMap { path, level_dat })
    }

    /// 获取所有待转换的 Region 区域组
    pub fn list_groups(&self) -> Result<Vec<MCGroup>, String> {
        let region_dir = self.path.join("region");
        if !region_dir.exists() {
            return Err("region directory not found".to_string());
        }

        let mut groups = Vec::new();
        let entries = std::fs::read_dir(region_dir)
            .map_err(|e| format!("Failed to read region dir: {}", e))?;

        for entry in entries {
            let entry = entry.map_err(|e| e.to_string())?;
            let filename = entry.file_name().to_string_lossy().into_owned();
            
            // 解析类似 r.0.-1.mca 或 r.1.2.mcr 格式的文件名
            let parts: Vec<&str> = filename.split('.').collect();
            if parts.len() == 4 && parts[0] == "r" {
                if let (Ok(x), Ok(z)) = (parts[1].parse::<i32>(), parts[2].parse::<i32>()) {
                    let format = match parts[3] {
                        "mca" => MCFormat::Anvil,
                        "mcr" => MCFormat::Regions,
                        _ => continue,
                    };
                    
                    // 原项目限制：太远则跳过，防数值溢出
                    const MAX_DISTANCE: i32 = 30900;
                    let x_nodes = x * 32 * MAP_BLOCK_SIZE as i32;
                    let z_nodes = z * 32 * MAP_BLOCK_SIZE as i32;
                    if x_nodes.abs() > MAX_DISTANCE || z_nodes.abs() > MAX_DISTANCE {
                        continue;
                    }

                    groups.push(MCGroup {
                        name: filename,
                        x,
                        z,
                        format,
                        file_path: entry.path(),
                    });
                }
            }
        }
        Ok(groups)
    }

    /// 扫描区域文件，通过读取头部 4096 字节偏移表检测有效的 Chunk 坐标
    pub fn list_chunks(&self, group: &MCGroup) -> Result<Vec<MCChunkPos>, String> {
        let mut file = File::open(&group.file_path)
            .map_err(|e| format!("Failed to open region file: {}", e))?;

        let mut chunks = Vec::new();
        let chunk_x_start = group.x * 32;
        let chunk_z_start = group.z * 32;

        // 区域文件头：1024个 4-byte 偏移值
        for cx in 0..32 {
            for cz in 0..32 {
                let offset_pos = ((cx & 31) + (cz & 31) * 32) * 4;
                file.seek(SeekFrom::Start(offset_pos as u64)).map_err(|e| e.to_string())?;
                
                let mut offset_buf = [0u8; 4];
                file.read_exact(&mut offset_buf).map_err(|e| e.to_string())?;
                
                // 前3个字节表示扇区偏移量，最后1个字节表示扇区长度
                let sector_offset = ((offset_buf[0] as u32) << 16) | ((offset_buf[1] as u32) << 8) | (offset_buf[2] as u32);
                if sector_offset != 0 {
                    chunks.push(MCChunkPos {
                        x: chunk_x_start + cx,
                        z: chunk_z_start + cz,
                    });
                }
            }
        }
        Ok(chunks)
    }

    /// 核心反序列化：加载具体 Chunk，将其切分为 Y 轴高度上的一组 MCBlock
    pub fn load_chunk(&self, group: &MCGroup, pos: MCChunkPos) -> Result<Vec<MCBlock>, String> {
        let mut file = File::open(&group.file_path)
            .map_err(|e| format!("Failed to open region file: {}", e))?;

        let offset_pos = (((pos.x & 31) + (pos.z & 31) * 32) * 4) as u64;
        file.seek(SeekFrom::Start(offset_pos)).map_err(|e| e.to_string())?;

        let mut header = [0u8; 4];
        file.read_exact(&mut header).map_err(|e| e.to_string())?;

        let sector_offset = ((header[0] as u64) << 16) | ((header[1] as u64) << 8) | (header[2] as u64);
        if sector_offset == 0 {
            return Err("Chunk not generated/saved".to_string());
        }

        // 定位到具体扇区 (每个扇区 4096 字节)
        file.seek(SeekFrom::Start(sector_offset * 4096)).map_err(|e| e.to_string())?;

        // 读取长度(4 bytes)和压缩类型(1 byte)
        let length = file.read_u32::<BigEndian>().map_err(|e| e.to_string())?;
        let compression_type = file.read_u8().map_err(|e| e.to_string())?;

        let mut compressed_data = vec![0u8; (length - 1) as usize];
        file.read_exact(&mut compressed_data).map_err(|e| e.to_string())?;

        // 解压数据
        let decompressed = match compression_type {
            1 => {
                let mut decoder = GzDecoder::new(&compressed_data[..]);
                let mut buf = Vec::new();
                decoder.read_to_end(&mut buf).map_err(|e| e.to_string())?;
                buf
            }
            2 => {
                let mut decoder = ZlibDecoder::new(&compressed_data[..]);
                let mut buf = Vec::new();
                decoder.read_to_end(&mut buf).map_err(|e| e.to_string())?;
                buf
            }
            _ => return Err(format!("Unsupported compression type: {}", compression_type)),
        };

        // ... 保持 load_chunk 顶部解压部分不变 ...

        let mut cursor = std::io::Cursor::new(decompressed);
        let nbt_root = parse_nbt(&mut cursor)?;

        let mut blocks = Vec::new();
        
        if let Some(level_nbt) = nbt_root.get_compound_child("")
            .and_then(|root| root.get_compound_child("Level")) 
        {
            // 提取 Chunk 顶层所有的 TileEntities 列表
            let empty_vec = Vec::new();
            let tile_entities = level_nbt.get_list_child("TileEntities").unwrap_or(&empty_vec);

            match group.format {
                MCFormat::Anvil => {
                    if let Some(sections) = level_nbt.get_list_child("Sections") {
                        for section in sections {
                            if let Some(y) = section.get_byte("Y") {
                                // 解析时，顺便把属于该 Y 高度切片 (y_slice) 的实体滤出来传进去
                                let mut section_blocks = parse_anvil_section(section, pos, y)?;
                                
                                for te in tile_entities {
                                    if let Some(te_y) = te.get("y").and_then(|t| t.as_i32()) {
                                        if (te_y >> 4) == y as i32 {
                                            section_blocks.tile_entities.push(te.clone());
                                        }
                                    }
                                }
                                blocks.push(section_blocks);
                            }
                        }
                    }
                }
                MCFormat::Regions => {
                    for y_slice in 0..8 {
                        let mut section_blocks = parse_region_slice(level_nbt, pos, y_slice)?;
                        for te in tile_entities {
                            if let Some(te_y) = te.get("y").and_then(|t| t.as_i32()) {
                                if (te_y >> 4) == y_slice as i32 {
                                    section_blocks.tile_entities.push(te.clone());
                                }
                            }
                        }
                        blocks.push(section_blocks);
                    }
                }
            }
        }

        Ok(blocks)
    }

    /// 从 level.dat 中提取真实的 Minecraft 出生点坐标
    pub fn get_spawn_point(&self) -> (i32, i32, i32) {
        // level_dat 根节点名通常为空字符串 ""
        let root = self.level_dat.get_compound_child("").unwrap_or(&self.level_dat);
        
        if let Some(data) = root.get_compound_child("Data") {
            let x = data.get("SpawnX").and_then(|t| t.as_i32()).unwrap_or(0);
            let y = data.get("SpawnY").and_then(|t| t.as_i32()).unwrap_or(64);
            let z = data.get("SpawnZ").and_then(|t| t.as_i32()).unwrap_or(0);
            return (x, y, z);
        }
        // 如果找不到，返回一个合理的默认安全高度
        (0, 64, 0)
    }
}

// ==========================================
// 辅助解析算法：逆转坐标系和处理半字节 (4-bit)
// ==========================================

fn parse_anvil_section(section: &NbtTag, cp: MCChunkPos, y_slice: u8) -> Result<MCBlock, String> {
    // 镜像反转 X 轴对齐 Minetest 的坐标系统
    let pos_x = -cp.x - 1;
    let pos_y = y_slice;
    let pos_z = cp.z;

    let mut blocks = vec![0u16; NODES_PER_BLOCK];
    let mut data = vec![0u8; NODES_PER_BLOCK];
    let mut sky_light = vec![0u8; NODES_PER_BLOCK];
    let mut block_light = vec![0u8; NODES_PER_BLOCK];

    if let Some(blocks_array) = section.get_byte_array("Blocks") {
        reverse_x_axis(&mut blocks, blocks_array);
    }

    if let Some(add_array) = section.get_byte_array("Add") {
        let mut blocks_add = vec![0u8; NODES_PER_BLOCK];
        expand_half_bytes(&mut blocks_add, add_array);
        for i in 0..NODES_PER_BLOCK {
            blocks[i] |= (blocks_add[i] as u16) << 8;
        }
    }

    if let Some(data_array) = section.get_byte_array("Data") {
        expand_half_bytes(&mut data, data_array);
    }

    if let Some(sky_array) = section.get_byte_array("SkyLight") {
        expand_half_bytes(&mut sky_light, sky_array);
    }

    if let Some(bl_array) = section.get_byte_array("BlockLight") {
        expand_half_bytes(&mut block_light, bl_array);
    } else {
        zero_bytes(&mut block_light);
    }

    // ============== 在此处插入后处理 ==============
    post_process_blocks(&mut blocks, &mut data, &mut sky_light, &mut block_light);
    // =============================================

    Ok(MCBlock {
        pos_x,
        pos_y,
        pos_z,
        blocks,
        data,
        sky_light,
        block_light,
        tile_entities: Vec::new(),
    })
}

fn parse_region_slice(chunk_level: &NbtTag, cp: MCChunkPos, y_slice: u8) -> Result<MCBlock, String> {
    let mut blocks = vec![0u16; NODES_PER_BLOCK];
    let mut data = vec![0u8; NODES_PER_BLOCK];
    let mut sky_light = vec![0u8; NODES_PER_BLOCK];
    let mut block_light = vec![0u8; NODES_PER_BLOCK];

    if let Some(blocks_array) = chunk_level.get_byte_array("Blocks") {
        extract_slice(&mut blocks, blocks_array, y_slice);
    }
    if let Some(data_array) = chunk_level.get_byte_array("Data") {
        extract_slice_half_bytes(&mut data, data_array, y_slice);
    }
    if let Some(sky_array) = chunk_level.get_byte_array("SkyLight") {
        extract_slice_half_bytes(&mut sky_light, sky_array, y_slice);
    }
    if let Some(bl_array) = chunk_level.get_byte_array("BlockLight") {
        extract_slice_half_bytes(&mut block_light, bl_array, y_slice);
    }

    Ok(MCBlock {
        pos_x: cp.x,
        pos_y: y_slice,
        pos_z: cp.z,
        blocks,
        data,
        sky_light,
        block_light,
        tile_entities: Vec::new(),
    })
}

// 对齐 C++ 中的 reverseXAxis (YZX 格式重排)
fn reverse_x_axis(data: &mut [u16], raw: &[u8]) {
    let mut data_key = 0;
    for y in 0..16 {
        for z in 0..16 {
            for x in 0..16 {
                let i = (y << 8) | ((15 - z) << 4) | x;
                if i < raw.len() {
                    data[data_key] = raw[i] as u16;
                }
                data_key += 1;
            }
        }
    }
}

// 对齐 C++ 中的 expandHalfBytes (将每个字节拆分为 2 个 4-bit 属性)
fn expand_half_bytes(data: &mut [u8], raw: &[u8]) {
    let mut data_key = 0;
    for y in 0..16 {
        for z in 0..16 {
            for x in 0..8 {
                let i = (y << 7) | ((15 - z) << 3) | x;
                if i < raw.len() {
                    let b = raw[i];
                    data[data_key] = b & 0xF;
                    data[data_key + 1] = (b >> 4) & 0xF;
                }
                data_key += 2;
            }
        }
    }
}

fn extract_slice(data: &mut [u16], raw: &[u8], y_slice: u8) {
    let mut key = (y_slice as usize) << 4;
    let mut data_key = 0;
    for _y in 0..16 {
        for _z in 0..16 {
            for _x in 0..16 {
                if key < raw.len() {
                    data[data_key] = raw[key] as u16;
                }
                data_key += 1;
                key += 2048;
            }
            key = (key & 0x7FF) + 128;
        }
        key = (key & 0x7F) + 1;
    }
}

fn extract_slice_half_bytes(data: &mut [u8], raw: &[u8], y_slice: u8) {
    let mut key = (y_slice as usize) << 3;
    let mut data_key_1 = 0;
    let mut data_key_2 = 256;
    for _y in (0..16).step_by(2) {
        for _z in 0..16 {
            for _x in 0..16 {
                if key < raw.len() {
                    let b = raw[key];
                    data[data_key_1] = b & 0xF;
                    data[data_key_2] = (b >> 4) & 0xF;
                }
                data_key_1 += 1;
                data_key_2 += 1;
                key += 1024;
            }
            key = (key & 0x3FF) + 64;
        }
        key = (key & 0x3F) + 1;
        data_key_1 += 256;
        data_key_2 += 256;
    }
}

// ==========================================
// 工业级强类型 NBT 树实现 (完全复刻并超越原 C++ nbt.hpp / nbt.cpp)
// ==========================================

#[derive(Debug, Clone)]
pub enum NbtTag {
    End,
    Byte(i8),
    Short(i16),
    Int(i32),
    Long(i64),
    Float(f32),
    Double(f64),
    ByteArray(Vec<u8>),
    String(String),
    List(Vec<NbtTag>),
    Compound(std::collections::HashMap<String, NbtTag>),
    IntArray(Vec<i32>),
    LongArray(Vec<i64>),
}

impl NbtTag {
    /// 模拟 C++ 中的 operator[]，安全地通过 Key 检索 Compound 子节点
    pub fn get(&self, key: &str) -> Option<&NbtTag> {
        match self {
            NbtTag::Compound(map) => map.get(key),
            _ => None,
        }
    }

    /// 强类型转换：尝试转换为 i64 (兼容 Byte, Short, Int, Long)
    pub fn as_i64(&self) -> Option<i64> {
        match self {
            &NbtTag::Byte(val) => Some(val as i64),
            &NbtTag::Short(val) => Some(val as i64),
            &NbtTag::Int(val) => Some(val as i64),
            &NbtTag::Long(val) => Some(val),
            _ => None,
        }
    }

    /// 强类型转换：尝试转换为 i32
    pub fn as_i32(&self) -> Option<i32> {
        match self {
            &NbtTag::Byte(val) => Some(val as i32),
            &NbtTag::Short(val) => Some(val as i32),
            &NbtTag::Int(val) => Some(val),
            &NbtTag::Long(val) => Some(val as i32),
            _ => None,
        }
    }

    /// 强类型转换：尝试转换为 f64
    pub fn as_f64(&self) -> Option<f64> {
        match self {
            &NbtTag::Float(val) => Some(val as f64),
            &NbtTag::Double(val) => Some(val),
            _ => None,
        }
    }

    /// 强类型转换：安全转换为 String 引用
    pub fn as_str(&self) -> Option<&str> {
        match self {
            NbtTag::String(s) => Some(s.as_str()),
            _ => None,
        }
    }

    /// 强类型转换：安全转换为 ByteArray 引用
    pub fn as_bytes(&self) -> Option<&[u8]> {
        match self {
            NbtTag::ByteArray(arr) => Some(&arr[..]),
            _ => None,
        }
    }

    /// 强类型转换：安全转换为 List 引用
    pub fn as_list(&self) -> Option<&Vec<NbtTag>> {
        match self {
            NbtTag::List(list) => Some(list),
            _ => None,
        }
    }

    /// 兼容旧代码的方法定义
    pub fn get_compound_child(&self, name: &str) -> Option<&NbtTag> {
        self.get(name)
    }

    pub fn get_list_child(&self, name: &str) -> Option<&Vec<NbtTag>> {
        self.get(name).and_then(|tag| tag.as_list())
    }

    pub fn get_byte(&self, name: &str) -> Option<u8> {
        self.get(name).and_then(|tag| match tag {
            &NbtTag::Byte(b) => Some(b as u8),
            _ => None,
        })
    }

    pub fn get_byte_array(&self, name: &str) -> Option<&[u8]> {
        self.get(name).and_then(|tag| tag.as_bytes())
    }
}

pub fn parse_nbt<R: Read + Seek>(reader: &mut R) -> Result<NbtTag, String> {
    let tag_type = reader.read_u8().map_err(|e| e.to_string())?;
    if tag_type == 0 {
        return Ok(NbtTag::End);
    }
    
    // 读取 NBT 根节点的名称长度及内容
    let name_len = reader.read_u16::<BigEndian>().map_err(|e| e.to_string())?;
    let mut name_buf = vec![0u8; name_len as usize];
    reader.read_exact(&mut name_buf).map_err(|e| e.to_string())?;
    let root_name = String::from_utf8_lossy(&name_buf).into_owned();

    let tag = read_tag_payload(reader, tag_type)?;
    
    // 返回带根名称包裹的 Compound 字典
    let mut root_map = std::collections::HashMap::new();
    root_map.insert(root_name, tag);
    Ok(NbtTag::Compound(root_map))
}

fn read_tag_payload<R: Read + Seek>(reader: &mut R, tag_type: u8) -> Result<NbtTag, String> {
    match tag_type {
        1 => Ok(NbtTag::Byte(reader.read_i8().map_err(|e| e.to_string())?)),
        2 => Ok(NbtTag::Short(reader.read_i16::<BigEndian>().map_err(|e| e.to_string())?)),
        3 => Ok(NbtTag::Int(reader.read_i32::<BigEndian>().map_err(|e| e.to_string())?)),
        4 => Ok(NbtTag::Long(reader.read_i64::<BigEndian>().map_err(|e| e.to_string())?)),
        5 => Ok(NbtTag::Float(reader.read_f32::<BigEndian>().map_err(|e| e.to_string())?)),
        6 => Ok(NbtTag::Double(reader.read_f64::<BigEndian>().map_err(|e| e.to_string())?)),
        7 => {
            let len = reader.read_u32::<BigEndian>().map_err(|e| e.to_string())? as usize;
            let mut buf = vec![0u8; len];
            reader.read_exact(&mut buf).map_err(|e| e.to_string())?;
            Ok(NbtTag::ByteArray(buf))
        }
        8 => {
            let len = reader.read_u16::<BigEndian>().map_err(|e| e.to_string())? as usize;
            let mut buf = vec![0u8; len];
            reader.read_exact(&mut buf).map_err(|e| e.to_string())?;
            Ok(NbtTag::String(String::from_utf8_lossy(&buf).into_owned()))
        }
        9 => {
            let sub_type = reader.read_u8().map_err(|e| e.to_string())?;
            let len = reader.read_u32::<BigEndian>().map_err(|e| e.to_string())? as usize;
            let mut list = Vec::with_capacity(len);
            for _ in 0..len {
                list.push(read_tag_payload(reader, sub_type)?);
            }
            Ok(NbtTag::List(list))
        }
        10 => {
            let mut map = std::collections::HashMap::new();
            loop {
                let sub_type = reader.read_u8().map_err(|e| e.to_string())?;
                if sub_type == 0 {
                    break;
                }
                let len = reader.read_u16::<BigEndian>().map_err(|e| e.to_string())? as usize;
                let mut name_buf = vec![0u8; len];
                reader.read_exact(&mut name_buf).map_err(|e| e.to_string())?;
                let name = String::from_utf8_lossy(&name_buf).into_owned();
                let val = read_tag_payload(reader, sub_type)?;
                map.insert(name, val);
            }
            Ok(NbtTag::Compound(map))
        }
        11 => {
            let len = reader.read_u32::<BigEndian>().map_err(|e| e.to_string())? as usize;
            let mut arr = Vec::with_capacity(len);
            for _ in 0..len {
                arr.push(reader.read_i32::<BigEndian>().map_err(|e| e.to_string())?);
            }
            Ok(NbtTag::IntArray(arr))
        }
        12 => {
            let len = reader.read_u32::<BigEndian>().map_err(|e| e.to_string())? as usize;
            let mut arr = Vec::with_capacity(len);
            for _ in 0..len {
                arr.push(reader.read_i64::<BigEndian>().map_err(|e| e.to_string())?);
            }
            Ok(NbtTag::LongArray(arr))
        }
        _ => Err(format!("Unknown NBT tag type: {}", tag_type)),
    }
}

/// 辅助函数：将字节切片内容全部清零，对应 C++ 的 zeroBytes
fn zero_bytes(data: &mut [u8]) {
    for byte in data.iter_mut() {
        *byte = 0;
    }
}