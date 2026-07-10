use std::fs::File;
use std::io::{Read, Seek, SeekFrom};
use crate::convert::post_process_blocks;
use std::path::{Path, PathBuf};
use byteorder::{BigEndian, ReadBytesExt};
use flate2::read::{GzDecoder, ZlibDecoder};

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

pub struct MCBlock {
    pub pos_x: i32,
    pub pos_y: u8,
    pub pos_z: i32,
    pub blocks: Vec<u16>,
    pub data: Vec<u8>,
    pub sky_light: Vec<u8>,
    pub block_light: Vec<u8>,
    pub tile_entities: Vec<NbtTag>,
}

pub struct MCMap {
    path: PathBuf,
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
        
        let mut decoder = GzDecoder::new(file);
        let mut decompressed_data = Vec::new();
        decoder.read_to_end(&mut decompressed_data)
            .map_err(|e| format!("Failed to decompress level.dat: {}", e))?;

        let mut cursor = std::io::Cursor::new(decompressed_data);
        let level_dat = parse_nbt(&mut cursor)
            .map_err(|e| format!("Failed to parse level.dat NBT: {}", e))?;

        Ok(MCMap { path, level_dat })
    }

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
            
            let parts: Vec<&str> = filename.split('.').collect();
            if parts.len() == 4 && parts[0] == "r" {
                if let (Ok(x), Ok(z)) = (parts[1].parse::<i32>(), parts[2].parse::<i32>()) {
                    let format = match parts[3] {
                        "mca" => MCFormat::Anvil,
                        "mcr" => MCFormat::Regions,
                        _ => continue,
                    };
                    
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

    pub fn list_chunks(&self, group: &MCGroup) -> Result<Vec<MCChunkPos>, String> {
        let mut file = File::open(&group.file_path)
            .map_err(|e| format!("Failed to open region file: {}", e))?;

        let mut chunks = Vec::new();
        let chunk_x_start = group.x * 32;
        let chunk_z_start = group.z * 32;

        for cx in 0..32 {
            for cz in 0..32 {
                let offset_pos = ((cx & 31) + (cz & 31) * 32) * 4;
                file.seek(SeekFrom::Start(offset_pos as u64)).map_err(|e| e.to_string())?;
                
                let mut offset_buf = [0u8; 4];
                file.read_exact(&mut offset_buf).map_err(|e| e.to_string())?;
                
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

        file.seek(SeekFrom::Start(sector_offset * 4096)).map_err(|e| e.to_string())?;

        let length = file.read_u32::<BigEndian>().map_err(|e| e.to_string())?;
        let compression_type = file.read_u8().map_err(|e| e.to_string())?;

        let mut compressed_data = vec![0u8; (length - 1) as usize];
        file.read_exact(&mut compressed_data).map_err(|e| e.to_string())?;

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

        let mut cursor = std::io::Cursor::new(decompressed);
        let nbt_root = parse_nbt(&mut cursor)?;

        let mut blocks = Vec::new();
        
        if let Some(level_nbt) = nbt_root.get_compound_child("")
            .and_then(|root| root.get_compound_child("Level")) 
        {
            let empty_vec = Vec::new();
            let tile_entities = level_nbt.get_list_child("TileEntities").unwrap_or(&empty_vec);

            match group.format {
                MCFormat::Anvil => {
                    if let Some(sections) = level_nbt.get_list_child("Sections") {
                        for section in sections {
                            if let Some(y) = section.get_byte("Y") {
                                let mut section_blocks = parse_anvil_section(section, pos, y)?;
                                
                                for te in tile_entities {
                                    if let Some(te_y) = te.get("y").and_then(|t| t.as_i32()) {
                                        if (te_y >> 4) == y as i32 {
                                            let mut te_cloned = te.clone();
                                            if let Some(te_x) = te_cloned.get_mut_map().and_then(|m| m.get_mut("x")) {
                                                if let NbtTag::Int(x_val) = te_x {
                                                    *x_val = section_blocks.pos_x * 16 + 15 - (*x_val % 16);
                                                }
                                            }
                                            section_blocks.tile_entities.push(te_cloned);
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

    pub fn get_spawn_point(&self) -> (i32, i32, i32) {
        let root = self.level_dat.get_compound_child("").unwrap_or(&self.level_dat);
        
        if let Some(data) = root.get_compound_child("Data") {
            let x = data.get("SpawnX").and_then(|t| t.as_i32()).unwrap_or(0);
            let y = data.get("SpawnY").and_then(|t| t.as_i32()).unwrap_or(64);
            let z = data.get("SpawnZ").and_then(|t| t.as_i32()).unwrap_or(0);
            
            return (x, y, -z);
        }
        (0, 64, 0)
    }
}

// ==========================================
// 辅助解析算法：逆转坐标系和处理半字节 (4-bit)
// ==========================================

fn parse_anvil_section(section: &NbtTag, cp: MCChunkPos, y_slice: u8) -> Result<MCBlock, String> {
    let pos_x = -cp.x - 1;
    let pos_y = y_slice;
    let pos_z = -cp.z - 1; 

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

    post_process_blocks(&mut blocks, &mut data, &mut sky_light, &mut block_light);

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
        pos_z: -cp.z - 1, 
        blocks,
        data,
        sky_light,
        block_light,
        tile_entities: Vec::new(),
    })
}

/// 【终极修复点】：将源数据的 Anvil 布局 (YZX) 重映射并转换到标准的 Minetest (ZYX) 排布。
/// 这样在 Rust 内存中解开直接就是标准的 ZYX 序，后面直接线性遍历输出即完美对齐，世界彻底回正！
fn reverse_x_axis(data: &mut [u16], raw: &[u8]) {
    for z in 0..16 {
        for y in 0..16 {
            for x in 0..16 {
                // Minetest 目标索引 (ZYX)
                let mt_idx = z * 256 + y * 16 + x;
                // Minecraft 源数据索引 (YZX)，带上 X轴反转 (15-x) 以及 Z 轴的镜像位移以修正偏角
                let mc_idx = (y << 8) | (((15 - z) & 0xF) << 4) | (x & 0xF);
                if mc_idx < raw.len() {
                    data[mt_idx] = raw[mc_idx] as u16;
                }
            }
        }
    }
}

/// 同理，对 param1 & param2 等半字节 (4-bit) 数据也采用标准 (ZYX) 的空间轴射转换
fn expand_half_bytes(data: &mut [u8], raw: &[u8]) {
    for z in 0..16 {
        for y in 0..16 {
            for x in (0..16).step_by(2) {
                // ZYX 的两个相邻坐标
                let mt_idx1 = z * 256 + y * 16 + x;
                let mt_idx2 = z * 256 + y * 16 + (x + 1);

                // 根据原始 YZX 在 raw 中提取一个字节中的两个半字节值
                let mc_x_half = x >> 1;
                let mc_idx = (y << 7) | (((15 - z) & 0xF) << 3) | mc_x_half;
                
                if mc_idx < raw.len() {
                    let b = raw[mc_idx];
                    data[mt_idx1] = b & 0xF;
                    data[mt_idx2] = (b >> 4) & 0xF;
                }
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

fn zero_bytes(data: &mut [u8]) {
    for byte in data.iter_mut() {
        *byte = 0;
    }
}

// ==========================================
// 工业级强类型 NBT 树实现
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
    pub fn get(&self, key: &str) -> Option<&NbtTag> {
        match self {
            NbtTag::Compound(map) => map.get(key),
            _ => None,
        }
    }

    pub fn get_mut_map(&mut self) -> Option<&mut std::collections::HashMap<String, NbtTag>> {
        match self {
            NbtTag::Compound(map) => Some(map),
            _ => None,
        }
    }

    pub fn as_i64(&self) -> Option<i64> {
        match self {
            &NbtTag::Byte(val) => Some(val as i64),
            &NbtTag::Short(val) => Some(val as i64),
            &NbtTag::Int(val) => Some(val as i64),
            &NbtTag::Long(val) => Some(val),
            _ => None,
        }
    }

    pub fn as_i32(&self) -> Option<i32> {
        match self {
            &NbtTag::Byte(val) => Some(val as i32),
            &NbtTag::Short(val) => Some(val as i32),
            &NbtTag::Int(val) => Some(val),
            &NbtTag::Long(val) => Some(val as i32),
            _ => None,
        }
    }

    pub fn as_f64(&self) -> Option<f64> {
        match self {
            &NbtTag::Float(val) => Some(val as f64),
            &NbtTag::Double(val) => Some(val),
            _ => None,
        }
    }

    pub fn as_str(&self) -> Option<&str> {
        match self {
            NbtTag::String(s) => Some(s.as_str()),
            _ => None,
        }
    }

    pub fn as_bytes(&self) -> Option<&[u8]> {
        match self {
            NbtTag::ByteArray(arr) => Some(&arr[..]),
            _ => None,
        }
    }

    pub fn as_list(&self) -> Option<&Vec<NbtTag>> {
        match self {
            NbtTag::List(list) => Some(list),
            _ => None,
        }
    }

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
    
    let name_len = reader.read_u16::<BigEndian>().map_err(|e| e.to_string())?;
    let mut name_buf = vec![0u8; name_len as usize];
    reader.read_exact(&mut name_buf).map_err(|e| e.to_string())?;
    let root_name = String::from_utf8_lossy(&name_buf).into_owned();

    let tag = read_tag_payload(reader, tag_type)?;
    
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