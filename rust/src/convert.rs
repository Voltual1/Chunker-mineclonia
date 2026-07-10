use std::collections::HashMap;
use std::sync::Mutex;
use once_cell::sync::Lazy;
use crate::mc_map::NODES_PER_BLOCK; 

// 定义 Minetest content_t 类型为 u16
pub type ContentT = u16;

pub const CONTENT_IGNORE: ContentT = 0;
pub const CONTENT_AIR: ContentT = 1;
pub const CONTENT_FIRST: ContentT = 2;

// 全局注册的 Minetest 字符串到 ID 的双向映射
pub struct ContentRegistry {
    pub name_to_id: HashMap<String, ContentT>,
    pub id_to_name: Vec<String>,
    pub next_id: ContentT,
}

impl ContentRegistry {
    fn new() -> Self {
        let mut reg = ContentRegistry {
            name_to_id: HashMap::new(),
            id_to_name: Vec::new(),
            next_id: CONTENT_FIRST,
        };
        reg.register("ignore", CONTENT_IGNORE);
        reg.register("air", CONTENT_AIR);
        reg
    }

    fn register(&mut self, name: &str, id: ContentT) {
        self.name_to_id.insert(name.to_string(), id);
        if id as usize >= self.id_to_name.len() {
            self.id_to_name.resize(id as usize + 1, String::new());
        }
        self.id_to_name[id as usize] = name.to_string();
    }

    pub fn get_or_create(&mut self, name: &str) -> ContentT {
        if let Some(&id) = self.name_to_id.get(name) {
            return id;
        }
        let id = self.next_id;
        self.next_id += 1;
        self.register(name, id);
        id
    }
}

// 全局唯一的注册表，受互斥锁保护以确保并发安全
pub static REGISTRY: Lazy<Mutex<ContentRegistry>> = Lazy::new(|| {
    Mutex::new(ContentRegistry::new())
});

// 记录未知的 Minecraft 方块
pub static UNKNOWN_BLOCKS: Lazy<Mutex<std::collections::HashSet<String>>> = Lazy::new(|| {
    Mutex::new(std::collections::HashSet::new())
});

#[derive(Clone, Copy, Debug)]
pub enum PostProcessType {
    None,
    UpdateNodeLight,
    DoorBottom,
    DoorTop,
}

#[derive(Clone, Debug)]
pub struct ConversionData {
    pub tool: bool,
    pub param2: u8,
    pub cid: ContentT,
    pub post_process: PostProcessType,
}

// 转换表管理结构
pub struct ConversionTable {
    // 数字化转换：通过 (ID, Data) 组合检索
    numeric_table: HashMap<u32, ConversionData>,
    // 命名化转换：通过 "minecraft:stone:1" 字符串检索 (1.13+)
    string_table: HashMap<String, ConversionData>,
}

impl ConversionTable {
    pub fn get_numeric(&self, id: u16, data: u16) -> Option<&ConversionData> {
        let key = ((id as u32) << 16) | (data as u32);
        self.numeric_table.get(&key)
            .or_else(|| self.numeric_table.get(&((id as u32) << 16))) // 回退到 Data 为 0 的默认转换
    }

    pub fn get_string(&self, name: &str, data: u16) -> Option<&ConversionData> {
        let key_with_data = format!("{}:{}", name, data);
        self.string_table.get(&key_with_data)
            .or_else(|| self.string_table.get(name))
    }
}

// 全局静态转换表
pub static CONVERSION_TABLE: Lazy<ConversionTable> = Lazy::new(|| {
    let mut numeric_table = HashMap::new();
    let mut string_table = HashMap::new();
    let mut reg = REGISTRY.lock().unwrap();

    // 辅助闭包：快速插入数值化映射
    let mut add_conv = |mc_id: u16, mc_name: &str, datas: &[u16], mt_name: &str, param2: u8, tool: bool, pp: PostProcessType| {
        let cid = reg.get_or_create(mt_name);
        let conv = ConversionData {
            tool,
            param2,
            cid,
            post_process: pp,
        };

        if datas.is_empty() {
            let key = (mc_id as u32) << 16;
            numeric_table.insert(key, conv.clone());
            string_table.insert(mc_name.to_string(), conv);
        } else {
            for &data in datas {
                let key = ((mc_id as u32) << 16) | (data as u32);
                numeric_table.insert(key, conv.clone());
                string_table.insert(format!("{}:{}", mc_name, data), conv.clone());
            }
        }
    };

    // ==========================================
    // 复刻 conversions.h 中的方块材质转换关系
    // ==========================================
    
    // 空气
    add_conv(0, "minecraft:air", &[], "air", 0, false, PostProcessType::None);

    // 石头系列
    add_conv(1, "minecraft:stone", &[0], "mcl_core:stone", 0, false, PostProcessType::None);
    add_conv(1, "minecraft:stone", &[1], "mcl_core:granite", 0, false, PostProcessType::None);
    add_conv(1, "minecraft:stone", &[2], "mcl_core:granite_smooth", 0, false, PostProcessType::None);
    add_conv(1, "minecraft:stone", &[3], "mcl_core:diorite", 0, false, PostProcessType::None);
    add_conv(1, "minecraft:stone", &[4], "mcl_core:diorite_smooth", 0, false, PostProcessType::None);
    add_conv(1, "minecraft:stone", &[5], "mcl_core:andesite", 0, false, PostProcessType::None);
    add_conv(1, "minecraft:stone", &[6], "mcl_core:andesite_smooth", 0, false, PostProcessType::None);

    // 草方块与泥土
    add_conv(2, "minecraft:grass", &[], "mcl_core:dirt_with_grass", 0, false, PostProcessType::None);
    add_conv(3, "minecraft:dirt", &[0], "mcl_core:dirt", 0, false, PostProcessType::None);
    add_conv(3, "minecraft:dirt", &[1], "mcl_core:coarse_dirt", 0, false, PostProcessType::None);
    add_conv(3, "minecraft:dirt", &[2], "mcl_core:podzol", 0, false, PostProcessType::None);
    add_conv(4, "minecraft:cobble", &[], "mcl_core:cobble", 0, false, PostProcessType::None);

    // 木板系列
    add_conv(5, "minecraft:planks", &[0], "mcl_trees:wood_oak", 0, false, PostProcessType::None);
    add_conv(5, "minecraft:planks", &[1], "mcl_trees:wood_spruce", 0, false, PostProcessType::None);
    add_conv(5, "minecraft:planks", &[2], "mcl_trees:wood_birch", 0, false, PostProcessType::None);
    add_conv(5, "minecraft:planks", &[3], "mcl_trees:wood_jungle", 0, false, PostProcessType::None);
    add_conv(5, "minecraft:planks", &[4], "mcl_trees:wood_acacia", 0, false, PostProcessType::None);
    add_conv(5, "minecraft:planks", &[5], "mcl_trees:wood_dark_oak", 0, false, PostProcessType::None);

    // 基岩、水、岩浆
    add_conv(7, "minecraft:bedrock", &[], "mcl_core:bedrock", 0, false, PostProcessType::None);
    add_conv(8, "minecraft:flowing_water", &[], "mcl_core:water_flowing", 0, false, PostProcessType::None);
    add_conv(9, "minecraft:water", &[], "mcl_core:water_source", 0, false, PostProcessType::None);
    add_conv(10, "minecraft:flowing_lava", &[], "mcl_core:lava_flowing", 0, false, PostProcessType::None);
    add_conv(11, "minecraft:lava", &[], "mcl_core:lava_source", 0, false, PostProcessType::None);

    // 矿石
    add_conv(14, "minecraft:gold_ore", &[], "mcl_core:stone_with_gold", 0, false, PostProcessType::None);
    add_conv(15, "minecraft:iron_ore", &[], "mcl_core:stone_with_iron", 0, false, PostProcessType::None);
    add_conv(16, "minecraft:coal_ore", &[], "mcl_core:stone_with_coal", 0, false, PostProcessType::None);
    add_conv(21, "minecraft:lapis_ore", &[], "mcl_core:stone_with_lapis", 0, false, PostProcessType::None);
    add_conv(56, "minecraft:diamond_ore", &[], "mcl_core:stone_with_diamond", 0, false, PostProcessType::None);
    add_conv(73, "minecraft:redstone_ore", &[], "mcl_core:stone_with_redstone", 0, false, PostProcessType::None);

    // 常用原木与树叶
    add_conv(17, "minecraft:log", &[0, 4, 8, 12], "mcl_trees:tree_oak", 0, false, PostProcessType::None);
    add_conv(17, "minecraft:log", &[1, 5, 9, 13], "mcl_trees:tree_spruce", 0, false, PostProcessType::None);
    add_conv(17, "minecraft:log", &[2, 6, 10, 14], "mcl_trees:tree_birch", 0, false, PostProcessType::None);
    add_conv(17, "minecraft:log", &[3, 7, 11, 15], "mcl_trees:tree_jungle", 0, false, PostProcessType::None);

    add_conv(18, "minecraft:leaves", &[0, 8, 4, 12], "mcl_trees:leaves_oak", 0, false, PostProcessType::None);
    add_conv(18, "minecraft:leaves", &[1, 9, 5, 13], "mcl_trees:leaves_spruce", 0, false, PostProcessType::None);
    add_conv(18, "minecraft:leaves", &[2, 10, 6, 14], "mcl_trees:leaves_birch", 0, false, PostProcessType::None);
    add_conv(18, "minecraft:leaves", &[3, 11, 7, 15], "mcl_trees:leaves_jungle", 0, false, PostProcessType::None);

    // 羊毛系列 (35)
    let wool_colors = [
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "grey",
        "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"
    ];
    for (data_val, color) in wool_colors.iter().enumerate() {
        let mt_wool = format!("mcl_wool:{}", color);
        add_conv(35, "minecraft:wool", &[data_val as u16], &mt_wool, 0, false, PostProcessType::None);
    }

    // 火把 (50)
    add_conv(50, "minecraft:torch", &[0, 5], "mcl_torches:torch", 1, false, PostProcessType::None);
    add_conv(50, "minecraft:torch", &[1], "mcl_torches:torch_wall", 3, false, PostProcessType::None);
    add_conv(50, "minecraft:torch", &[2], "mcl_torches:torch_wall", 2, false, PostProcessType::None);
    add_conv(50, "minecraft:torch", &[3], "mcl_torches:torch_wall", 4, false, PostProcessType::None);
    add_conv(50, "minecraft:torch", &[4], "mcl_torches:torch_wall", 5, false, PostProcessType::None);

    // 楼梯 (以橡木楼梯 53 为代表，进行 facedir 映射)
    let stair_params = [(0, 1), (1, 3), (2, 2), (3, 0), (4, 23), (5, 21), (6, 22), (7, 20)];
    for &(mc_data, param2) in &stair_params {
        add_conv(53, "minecraft:oak_stairs", &[mc_data], "mcl_stairs:stair_oak", param2, false, PostProcessType::UpdateNodeLight);
        add_conv(67, "minecraft:stone_stairs", &[mc_data], "mcl_stairs:stair_cobble", param2, false, PostProcessType::UpdateNodeLight);
    }

    // 箱子 (54)
    add_conv(54, "minecraft:chest", &[2], "mcl_chests:chest", 2, false, PostProcessType::None);
    add_conv(54, "minecraft:chest", &[3], "mcl_chests:chest", 0, false, PostProcessType::None);
    add_conv(54, "minecraft:chest", &[4], "mcl_chests:chest", 1, false, PostProcessType::None);
    add_conv(54, "minecraft:chest", &[5], "mcl_chests:chest", 3, false, PostProcessType::None);

    // 门 (以木门 64 为例)
    add_conv(64, "minecraft:wooden_door", &[0, 1, 2, 3], "mcl_doors:door_oak", 0, false, PostProcessType::DoorBottom);
    add_conv(64, "minecraft:wooden_door", &[8, 9, 10, 11, 12, 13, 14, 15], "mcl_doors:door_oak_t_1", 0, false, PostProcessType::DoorTop);

    ConversionTable {
        numeric_table,
        string_table,
    }
});

/// 执行转换核心匹配
pub fn get_conversion(id: u16, data: u16) -> Option<ConversionData> {
    if let Some(data) = CONVERSION_TABLE.get_numeric(id, data) {
        return Some(data.clone());
    }

    // 记录未匹配成功的未知方块
    let mut unknown = UNKNOWN_BLOCKS.lock().unwrap();
    let label = format!("{}:{}", id, data);
    if unknown.insert(label.clone()) {
        log::warn!("Unknown block encountered: {}", label);
    }
    None
}

// ... 保持原有 Lazy 和 HashMap 转换定义不变 ...

/// 针对 Minetest 块内节点索引运算的辅助宏或内联函数
/// 对应 Map.hpp: #define BLOCK_NODE_IDX(x, y, z) (((y & 0xF) << 8) | ((z & 0xF) << 4) | (x & 0xF))
#[inline]
pub fn block_node_idx(x: usize, y: usize, z: usize) -> usize {
    ((y & 0xF) << 8) | ((z & 0xF) << 4) | (x & 0xF)
}

#[inline]
pub fn idx_to_xyz(idx: usize) -> (usize, usize, usize) {
    let x = idx & 0xF;
    let z = (idx >> 4) & 0xF;
    let y = (idx >> 8) & 0xF;
    (x, y, z)
}

/// 核心后处理流水线：
/// 传入当前正在构建的区块内所有 Block 数据，对其进行原位（In-place）修改和修补。
pub fn post_process_blocks(
    blocks: &mut [u16],
    data: &mut [u8],
    param1: &mut [u8],
    param2: &mut [u8],
) {
    // 遍历整个 16x16x16 的节点树
    for idx in 0..NODES_PER_BLOCK {
        let block_id = blocks[idx];
        
        // 1. 光照修正：针对楼梯和半砖节点
        // 根据转换表查询该节点是否需要修补光照
        if let Some(conv_data) = get_conversion_by_cid(block_id) {
            match conv_data.post_process {
                PostProcessType::UpdateNodeLight => {
                    update_node_light_rust(idx, param1);
                }
                PostProcessType::DoorBottom => {
                    // 处理双层门的下半部分
                    finish_door_rust(idx, blocks, param2, true);
                }
                PostProcessType::DoorTop => {
                    // 处理双层门的上半部分
                    finish_door_rust(idx, blocks, param2, false);
                }
                _ => {}
            }
        }
    }
}

/// 辅助检索：通过已经转换后的 Minetest 内部 ID (cid) 找回原始的配置动作
fn get_conversion_by_cid(cid: ContentT) -> Option<ConversionData> {
    CONVERSION_TABLE.numeric_table.values()
        .find(|data| data.cid == cid)
        .cloned()
}

/// 1. 光照辐射修补算法
/// 对应 C++ 的 update_node_light
fn update_node_light_rust(idx: usize, param1: &mut [u8]) {
    let (x, y, z) = idx_to_xyz(idx);
    
    // 6 个方向的偏移坐标
    let directions = [
        (0, 1, 0),  // 上
        (0, -1, 0), // 下
        (1, 0, 0),  // 东
        (-1, 0, 0), // 西
        (0, 0, 1),  // 南
        (0, 0, -1), // 北
    ];

    let mut max_light_day = 0i8;
    let mut max_light_night = 0i8;

    for &(dx, dy, dz) in &directions {
        let nx = x as i32 + dx;
        let ny = y as i32 + dy;
        let nz = z as i32 + dz;

        // 如果超出当前 16x16x16 边界，默认采用白天阳光最大值
        if nx < 0 || nx >= 16 || ny < 0 || ny >= 16 || nz < 0 || nz >= 16 {
            max_light_day = 14; // LIGHT_MAX (15 - 1)
            continue;
        }

        let neighbor_idx = block_node_idx(nx as usize, ny as usize, nz as usize);
        let light = param1[neighbor_idx];

        // 低4位为白天光照，高4位为夜间光照
        let l_day = (light & 0x0F) as i8 - 1;
        let l_night = ((light & 0xF0) >> 4) as i8 - 1;

        if l_day > max_light_day {
            max_light_day = l_day;
        }
        if l_night > max_light_night {
            max_light_night = l_night;
        }
    }

    // 更新回 param1
    let final_light = ((max_light_night.max(0) as u8) << 4) | (max_light_day.max(0) as u8);
    param1[idx] = final_light;
}

/// 2. 门联动状态补全算法
/// 对应 C++ 中的 finish_door 联动更新
fn finish_door_rust(idx: usize, blocks: &mut [u16], param2: &mut [u8], is_bottom: bool) {
    let (x, y, z) = idx_to_xyz(idx);
    
    if is_bottom {
        // 如果是下半部分，它的上半部分应该在 y + 1 处
        if y < 15 {
            let top_idx = block_node_idx(x, y + 1, z);
            let bottom_p2 = param2[idx];
            let top_p2 = param2[top_idx];

            let open = (bottom_p2 & 4) != 0;
            let mut dir = (bottom_p2 & 3) as i8; // 0:北, 1:东, 2:南, 3:西
            let hinge_right = (top_p2 & 1) == 0;

            let mut door_type = false;

            if hinge_right {
                door_type = !door_type;
                dir += 2;
            }

            if open {
                door_type = !door_type;
                dir += if hinge_right { -1 } else { 1 };
            }

            // 保持 [0, 3] facedir 环形取模
            dir = (dir + 4) % 4;

            // 更新下半部分和上半部分的朝向 (param2)
            param2[idx] = dir as u8;
            param2[top_idx] = dir as u8;

            // 获取注册表的锁动态构建开启/关闭材质ID
            let mut reg = REGISTRY.lock().unwrap();
            let suffix = if door_type { "_b_2" } else { "_b_1" };
            let top_suffix = if door_type { "_t_2" } else { "_t_1" };
            
            // 默认橡木门替换
            blocks[idx] = reg.get_or_create(&format!("mcl_doors:door_oak{}", suffix));
            blocks[top_idx] = reg.get_or_create(&format!("mcl_doors:door_oak{}", top_suffix));
        }
    }
}