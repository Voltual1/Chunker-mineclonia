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
    // Rust 声明式宏：复刻 C++ conversions.h 的展开体系
    // ==========================================

    // 1. 楼梯宏展开 (自动生成 8 种朝向的光照和 param2 映射)
    macro_rules! conv_stair {
        ($id:expr, $mcn:expr, $mtn:expr) => {
            add_conv($id, $mcn, &[0], $mtn, 1, false, PostProcessType::UpdateNodeLight);
            add_conv($id, $mcn, &[1], $mtn, 3, false, PostProcessType::UpdateNodeLight);
            add_conv($id, $mcn, &[2], $mtn, 2, false, PostProcessType::UpdateNodeLight);
            add_conv($id, $mcn, &[3], $mtn, 0, false, PostProcessType::UpdateNodeLight);
            add_conv($id, $mcn, &[4], $mtn, 23, false, PostProcessType::UpdateNodeLight);
            add_conv($id, $mcn, &[5], $mtn, 21, false, PostProcessType::UpdateNodeLight);
            add_conv($id, $mcn, &[6], $mtn, 22, false, PostProcessType::UpdateNodeLight);
            add_conv($id, $mcn, &[7], $mtn, 20, false, PostProcessType::UpdateNodeLight);
        };
    }

    // 2. 半砖宏展开 (自动生成顶部和底部半砖映射)
    macro_rules! conv_slab {
        ($id:expr, $mcn:expr, $dbottom:expr, $dtop:expr, $mtn:expr) => {
            add_conv($id, $mcn, &[$dbottom], $mtn, 0, false, PostProcessType::UpdateNodeLight);
            add_conv($id, $mcn, &[$dtop], &format!("{}_top", $mtn), 0, false, PostProcessType::UpdateNodeLight);
        };
    }

    // 3. 活板门宏展开
    macro_rules! conv_trapdoor {
        ($id:expr, $mcn:expr, $mtn:expr) => {
            add_conv($id, $mcn, &[0], $mtn, 2, false, PostProcessType::None);
            add_conv($id, $mcn, &[1], $mtn, 0, false, PostProcessType::None);
            add_conv($id, $mcn, &[2], $mtn, 1, false, PostProcessType::None);
            add_conv($id, $mcn, &[3], $mtn, 3, false, PostProcessType::None);
            add_conv($id, $mcn, &[4], &format!("{}_open", $mtn), 2, false, PostProcessType::None);
            add_conv($id, $mcn, &[5], &format!("{}_open", $mtn), 0, false, PostProcessType::None);
            add_conv($id, $mcn, &[6], &format!("{}_open", $mtn), 1, false, PostProcessType::None);
            add_conv($id, $mcn, &[7], &format!("{}_open", $mtn), 3, false, PostProcessType::None);
            add_conv($id, $mcn, &[8], $mtn, 22, false, PostProcessType::None);
            add_conv($id, $mcn, &[9], $mtn, 20, false, PostProcessType::None);
            add_conv($id, $mcn, &[10], $mtn, 23, false, PostProcessType::None);
            add_conv($id, $mcn, &[11], $mtn, 21, false, PostProcessType::None);
            add_conv($id, $mcn, &[12], &format!("{}_open", $mtn), 22, false, PostProcessType::None);
            add_conv($id, $mcn, &[13], &format!("{}_open", $mtn), 20, false, PostProcessType::None);
            add_conv($id, $mcn, &[14], &format!("{}_open", $mtn), 23, false, PostProcessType::None);
            add_conv($id, $mcn, &[15], &format!("{}_open", $mtn), 21, false, PostProcessType::None);
        };
    }

    // 4. 原木旋转宏展开
    macro_rules! conv_log {
        ($id:expr, $mcn:expr, $d:expr, $mtn:expr) => {
            add_conv($id, $mcn, &[$d], $mtn, 0, false, PostProcessType::None);
            add_conv($id, $mcn, &[($d + 4)], $mtn, 12, false, PostProcessType::None);
            add_conv($id, $mcn, &[($d + 8)], $mtn, 4, false, PostProcessType::None);
            add_conv($id, $mcn, &[($d + 12)], $mtn, 0, false, PostProcessType::None);
        };
    }

    // 5. 栅栏门旋转宏展开
    macro_rules! conv_gate {
        ($id:expr, $mcn:expr, $mtn:expr) => {
            add_conv($id, $mcn, &[0], $mtn, 0, false, PostProcessType::None);
            add_conv($id, $mcn, &[1], $mtn, 3, false, PostProcessType::None);
            add_conv($id, $mcn, &[2], $mtn, 2, false, PostProcessType::None);
            add_conv($id, $mcn, &[3], $mtn, 1, false, PostProcessType::None);
            add_conv($id, $mcn, &[4], &format!("{}_open", $mtn), 0, false, PostProcessType::None);
            add_conv($id, $mcn, &[5], &format!("{}_open", $mtn), 2, false, PostProcessType::None);
            add_conv($id, $mcn, &[6], &format!("{}_open", $mtn), 3, false, PostProcessType::None);
            add_conv($id, $mcn, &[7], &format!("{}_open", $mtn), 1, false, PostProcessType::None);
        };
    }

    // ==========================================
    // 补全所有方块数据映射
    // ==========================================

    // 空气与基础石头系列
    add_conv(0, "minecraft:air", &[], "air", 0, false, PostProcessType::None);
    add_conv(1, "minecraft:stone", &[0], "mcl_core:stone", 0, false, PostProcessType::None);
    add_conv(1, "minecraft:stone", &[1], "mcl_core:granite", 0, false, PostProcessType::None);
    add_conv(1, "minecraft:stone", &[2], "mcl_core:granite_smooth", 0, false, PostProcessType::None);
    add_conv(1, "minecraft:stone", &[3], "mcl_core:diorite", 0, false, PostProcessType::None);
    add_conv(1, "minecraft:stone", &[4], "mcl_core:diorite_smooth", 0, false, PostProcessType::None);
    add_conv(1, "minecraft:stone", &[5], "mcl_core:andesite", 0, false, PostProcessType::None);
    add_conv(1, "minecraft:stone", &[6], "mcl_core:andesite_smooth", 0, false, PostProcessType::None);

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

    // 基岩、流体、沙、碎石
    add_conv(7, "minecraft:bedrock", &[], "mcl_core:bedrock", 0, false, PostProcessType::None);
    add_conv(8, "minecraft:flowing_water", &[], "mcl_core:water_flowing", 0, false, PostProcessType::None);
    add_conv(9, "minecraft:water", &[], "mcl_core:water_source", 0, false, PostProcessType::None);
    add_conv(10, "minecraft:flowing_lava", &[], "mcl_core:lava_flowing", 0, false, PostProcessType::None);
    add_conv(11, "minecraft:lava", &[], "mcl_core:lava_source", 0, false, PostProcessType::None);
    add_conv(12, "minecraft:sand", &[0], "mcl_core:sand", 0, false, PostProcessType::None);
    add_conv(12, "minecraft:sand", &[1], "mcl_core:redsand", 0, false, PostProcessType::None);
    add_conv(13, "minecraft:gravel", &[], "mcl_core:gravel", 0, false, PostProcessType::None);

    // 矿石与原木系列
    add_conv(14, "minecraft:gold_ore", &[], "mcl_core:stone_with_gold", 0, false, PostProcessType::None);
    add_conv(15, "minecraft:iron_ore", &[], "mcl_core:stone_with_iron", 0, false, PostProcessType::None);
    add_conv(16, "minecraft:coal_ore", &[], "mcl_core:stone_with_coal", 0, false, PostProcessType::None);

    conv_log!(17, "minecraft:log", 0, "mcl_trees:tree_oak");
    conv_log!(17, "minecraft:log", 1, "mcl_trees:tree_spruce");
    conv_log!(17, "minecraft:log", 2, "mcl_trees:tree_birch");
    conv_log!(17, "minecraft:log", 3, "mcl_trees:tree_jungle");

    // 树叶
    add_conv(18, "minecraft:leaves", &[0, 8], "mcl_trees:leaves_oak", 0, false, PostProcessType::None);
    add_conv(18, "minecraft:leaves", &[1, 9], "mcl_trees:leaves_spruce", 0, false, PostProcessType::None);
    add_conv(18, "minecraft:leaves", &[2, 10], "mcl_trees:leaves_birch", 0, false, PostProcessType::None);
    add_conv(18, "minecraft:leaves", &[3, 11], "mcl_trees:leaves_jungle", 0, false, PostProcessType::None);

    add_conv(20, "minecraft:glass", &[], "mcl_core:glass", 0, false, PostProcessType::None);
    add_conv(21, "minecraft:lapis_ore", &[], "mcl_core:stone_with_lapis", 0, false, PostProcessType::None);
    add_conv(22, "minecraft:lapis_block", &[], "mcl_core:lapisblock", 0, false, PostProcessType::None);

    // 活塞
    add_conv(33, "minecraft:piston", &[0], "mesecons_piston:piston_down_normal_off", 0, false, PostProcessType::None);
    add_conv(33, "minecraft:piston", &[1], "mesecons_piston:piston_up_normal_off", 0, false, PostProcessType::None);
    add_conv(33, "minecraft:piston", &[2], "mesecons_piston:piston_normal_off", 2, false, PostProcessType::None);
    add_conv(33, "minecraft:piston", &[3], "mesecons_piston:piston_normal_off", 0, false, PostProcessType::None);
    add_conv(33, "minecraft:piston", &[4], "mesecons_piston:piston_normal_off", 1, false, PostProcessType::None);
    add_conv(33, "minecraft:piston", &[5], "mesecons_piston:piston_normal_off", 3, false, PostProcessType::None);

    // 羊毛系列 (35)
    let wool_colors = [
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "grey",
        "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"
    ];
    for (data_val, color) in wool_colors.iter().enumerate() {
        let mt_wool = format!("mcl_wool:{}", color);
        add_conv(35, "minecraft:wool", &[data_val as u16], &mt_wool, 0, false, PostProcessType::None);
    }

    // 花卉、红石、铁轨、楼梯、门
    add_conv(37, "minecraft:yellow_flower", &[], "mcl_flowers:dandelion", 0, false, PostProcessType::None);
    add_conv(39, "minecraft:brown_mushroom", &[], "mcl_mushrooms:mushroom_brown", 0, false, PostProcessType::None);
    add_conv(40, "minecraft:red_mushroom", &[], "mcl_mushrooms:mushroom_red", 0, false, PostProcessType::None);
    add_conv(41, "minecraft:gold_block", &[], "mcl_core:goldblock", 0, false, PostProcessType::None);
    add_conv(42, "minecraft:iron_block", &[], "mcl_core:ironblock", 0, false, PostProcessType::None);

    conv_slab!(44, "minecraft:stone_slab", 0, 8, "mcl_stairs:slab_stone");
    conv_slab!(44, "minecraft:stone_slab", 1, 9, "mcl_stairs:slab_sandstone");
    conv_slab!(44, "minecraft:stone_slab", 2, 10, "mcl_stairs:slab_oak");
    conv_slab!(44, "minecraft:stone_slab", 3, 11, "mcl_stairs:slab_cobble");
    conv_slab!(44, "minecraft:stone_slab", 4, 12, "mcl_stairs:slab_brick_block");
    conv_slab!(44, "minecraft:stone_slab", 5, 13, "mcl_stairs:slab_stonebrick");
    conv_slab!(44, "minecraft:stone_slab", 6, 14, "mcl_stairs:slab_nether_brick");
    conv_slab!(44, "minecraft:stone_slab", 7, 15, "mcl_stairs:slab_quartzblock");

    add_conv(45, "minecraft:brick_block", &[], "mcl_core:brick_block", 0, false, PostProcessType::None);
    add_conv(46, "minecraft:tnt", &[], "mcl_tnt:tnt", 0, false, PostProcessType::None);
    add_conv(47, "minecraft:bookshelf", &[], "mcl_books:bookshelf", 0, false, PostProcessType::None);
    add_conv(48, "minecraft:mossy_cobblestone", &[], "mcl_core:mossycobble", 0, false, PostProcessType::None);
    add_conv(49, "minecraft:obsidian", &[], "mcl_core:obsidian", 0, false, PostProcessType::None);

    // 火把与楼梯
    add_conv(50, "minecraft:torch", &[0, 5], "mcl_torches:torch", 1, false, PostProcessType::None);
    add_conv(50, "minecraft:torch", &[1], "mcl_torches:torch_wall", 3, false, PostProcessType::None);
    add_conv(50, "minecraft:torch", &[2], "mcl_torches:torch_wall", 2, false, PostProcessType::None);
    add_conv(50, "minecraft:torch", &[3], "mcl_torches:torch_wall", 4, false, PostProcessType::None);
    add_conv(50, "minecraft:torch", &[4], "mcl_torches:torch_wall", 5, false, PostProcessType::None);

    conv_stair!(53, "minecraft:oak_stairs", "mcl_stairs:stair_oak");

    // 箱子与红石
    add_conv(54, "minecraft:chest", &[2], "mcl_chests:chest", 2, false, PostProcessType::None);
    add_conv(54, "minecraft:chest", &[3], "mcl_chests:chest", 0, false, PostProcessType::None);
    add_conv(54, "minecraft:chest", &[4], "mcl_chests:chest", 1, false, PostProcessType::None);
    add_conv(54, "minecraft:chest", &[5], "mcl_chests:chest", 3, false, PostProcessType::None);

    add_conv(56, "minecraft:diamond_ore", &[], "mcl_core:stone_with_diamond", 0, false, PostProcessType::None);
    add_conv(57, "minecraft:diamond_block", &[], "mcl_core:diamondblock", 0, false, PostProcessType::None);
    add_conv(58, "minecraft:crafting_table", &[], "mcl_crafting_table:crafting_table", 0, false, PostProcessType::None);

    // 熔炉与门
    add_conv(61, "minecraft:furnace", &[2], "mcl_furnaces:furnace", 2, false, PostProcessType::None);
    add_conv(61, "minecraft:furnace", &[3], "mcl_furnaces:furnace", 0, false, PostProcessType::None);
    add_conv(61, "minecraft:furnace", &[4], "mcl_furnaces:furnace", 1, false, PostProcessType::None);
    add_conv(61, "minecraft:furnace", &[5], "mcl_furnaces:furnace", 3, false, PostProcessType::None);

    add_conv(64, "minecraft:wooden_door", &[0, 1, 2, 3], "mcl_doors:door_oak", 0, false, PostProcessType::DoorBottom);
    add_conv(64, "minecraft:wooden_door", &[8, 9, 10, 11, 12, 13, 14, 15], "mcl_doors:door_oak_t_1", 0, false, PostProcessType::DoorTop);

    add_conv(65, "minecraft:ladder", &[2], "mcl_core:ladder", 5, false, PostProcessType::None);
    add_conv(65, "minecraft:ladder", &[3], "mcl_core:ladder", 4, false, PostProcessType::None);
    add_conv(65, "minecraft:ladder", &[4], "mcl_core:ladder", 2, false, PostProcessType::None);
    add_conv(65, "minecraft:ladder", &[5], "mcl_core:ladder", 3, false, PostProcessType::None);

    add_conv(66, "minecraft:rail", &[], "mcl_minecarts:rail", 0, false, PostProcessType::None);
    conv_stair!(67, "minecraft:stone_stairs", "mcl_stairs:stair_cobble");

    add_conv(73, "minecraft:redstone_ore", &[], "mcl_core:stone_with_redstone", 0, false, PostProcessType::None);
    add_conv(79, "minecraft:ice", &[], "mcl_core:ice", 0, false, PostProcessType::None);
    add_conv(80, "minecraft:snow", &[], "mcl_core:snowblock", 0, false, PostProcessType::None);
    add_conv(81, "minecraft:cactus", &[], "mcl_core:cactus", 0, false, PostProcessType::None);
    add_conv(82, "minecraft:clay", &[], "mcl_core:clay", 0, false, PostProcessType::None);
    add_conv(83, "minecraft:reeds", &[], "mcl_core:reeds", 0, false, PostProcessType::None);
    add_conv(85, "minecraft:fence", &[], "mcl_fences:oak_fence", 0, false, PostProcessType::None);

    add_conv(87, "minecraft:netherrack", &[], "mcl_nether:netherrack", 0, false, PostProcessType::None);
    add_conv(88, "minecraft:soul_sand", &[], "mcl_nether:soul_sand", 0, false, PostProcessType::None);
    add_conv(89, "minecraft:glowstone", &[], "mcl_nether:glowstone", 0, false, PostProcessType::None);

    conv_trapdoor!(96, "minecraft:trapdoor", "mcl_doors:trapdoor");
    add_conv(98, "minecraft:stonebrick", &[0], "mcl_core:stonebrick", 0, false, PostProcessType::None);
    add_conv(98, "minecraft:stonebrick", &[1], "mcl_core:stonebrickmossy", 0, false, PostProcessType::None);
    add_conv(98, "minecraft:stonebrick", &[2], "mcl_core:stonebrickcracked", 0, false, PostProcessType::None);
    add_conv(98, "minecraft:stonebrick", &[3], "mcl_core:stonebrickcarved", 0, false, PostProcessType::None);

    add_conv(101, "minecraft:iron_bars", &[], "mcl_panes:bar", 0, false, PostProcessType::None);
    add_conv(103, "minecraft:melon_block", &[], "mcl_farming:melon", 0, false, PostProcessType::None);
    conv_gate!(107, "minecraft:fence_gate", "mcl_fences:oak_fence_gate");

    conv_stair!(108, "minecraft:brick_stairs", "mcl_stairs:stair_brick_block");
    conv_stair!(109, "minecraft:stone_brick_stairs", "mcl_stairs:stair_stonebrick");
    add_conv(110, "minecraft:mycelium", &[], "mcl_core:mycelium", 0, false, PostProcessType::None);
    add_conv(111, "minecraft:waterlily", &[], "mcl_flowers:waterlily", 0, false, PostProcessType::None);
    add_conv(112, "minecraft:nether_brick", &[], "mcl_nether:nether_brick", 0, false, PostProcessType::None);
    add_conv(113, "minecraft:nether_brick_fence", &[], "mcl_fences:nether_brick_fence", 0, false, PostProcessType::None);
    conv_stair!(114, "minecraft:nether_brick_stairs", "mcl_stairs:stair_nether_brick");

    add_conv(121, "minecraft:end_stone", &[], "mcl_end:end_stone", 0, false, PostProcessType::None);
    add_conv(123, "minecraft:redstone_lamp", &[], "mcl_redstone_lamp:lamp_off", 0, false, PostProcessType::None);
    add_conv(124, "minecraft:lit_redstone_lamp", &[], "mcl_redstone_lamp:lamp_on", 0, false, PostProcessType::None);

    add_conv(125, "minecraft:double_wooden_slab", &[0], "mcl_stairs:slab_oak_double", 0, false, PostProcessType::None);
    add_conv(125, "minecraft:double_wooden_slab", &[1], "mcl_stairs:slab_spruce_double", 0, false, PostProcessType::None);
    add_conv(125, "minecraft:double_wooden_slab", &[2], "mcl_stairs:slab_birch_double", 0, false, PostProcessType::None);
    add_conv(125, "minecraft:double_wooden_slab", &[3], "mcl_stairs:slab_jungle_double", 0, false, PostProcessType::None);
    add_conv(125, "minecraft:double_wooden_slab", &[4], "mcl_stairs:slab_acacia_double", 0, false, PostProcessType::None);
    add_conv(125, "minecraft:double_wooden_slab", &[5], "mcl_stairs:slab_dark_oak_double", 0, false, PostProcessType::None);

    conv_slab!(126, "minecraft:wooden_slab", 0, 8, "mcl_stairs:slab_oak");
    conv_slab!(126, "minecraft:wooden_slab", 1, 9, "mcl_stairs:slab_spruce");
    conv_slab!(126, "minecraft:wooden_slab", 2, 10, "mcl_stairs:slab_birch");
    conv_slab!(126, "minecraft:wooden_slab", 3, 11, "mcl_stairs:slab_jungle");
    conv_slab!(126, "minecraft:wooden_slab", 4, 12, "mcl_stairs:slab_acacia");
    conv_slab!(126, "minecraft:wooden_slab", 5, 13, "mcl_stairs:slab_dark_oak");

    conv_stair!(128, "minecraft:sandstone_stairs", "mcl_stairs:stair_sandstone");
    add_conv(129, "minecraft:emerald_ore", &[], "mcl_core:stone_with_emerald", 0, false, PostProcessType::None);
    add_conv(133, "minecraft:emerald_block", &[], "mcl_core:emeraldblock", 0, false, PostProcessType::None);

    conv_stair!(134, "minecraft:spruce_stairs", "mcl_stairs:stair_spruce");
    conv_stair!(135, "minecraft:birch_stairs", "mcl_stairs:stair_birch");
    conv_stair!(136, "minecraft:jungle_stairs", "mcl_stairs:stair_jungle");
    add_conv(138, "minecraft:beacon", &[], "mcl_beacons:beacon_beam", 0, false, PostProcessType::None);
    add_conv(145, "minecraft:anvil", &[], "mcl_anvils:anvil", 0, false, PostProcessType::None);

    add_conv(152, "minecraft:redstone_block", &[], "mesecons_torch:redstoneblock", 0, false, PostProcessType::None);
    add_conv(153, "minecraft:quartz_ore", &[], "mcl_nether:quartz_ore", 0, false, PostProcessType::None);
    conv_stair!(156, "minecraft:quartz_stairs", "mcl_stairs:stair_quartzblock");

    // 混凝土与混凝土粉末
    for color_id in 0..16 {
        let color = wool_colors[color_id];
        add_conv(251, "minecraft:concrete", &[color_id as u16], &format!("mcl_colorblocks:concrete_{}", color), 0, false, PostProcessType::None);
        add_conv(252, "minecraft:concrete_powder", &[color_id as u16], &format!("mcl_colorblocks:concrete_powder_{}", color), 0, false, PostProcessType::None);
    }

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
    // 内存序：z * 256 + y * 16 + x
    (z << 8) | (y << 4) | x
}


#[inline]
pub fn idx_to_xyz(idx: usize) -> (usize, usize, usize) {
    let x = idx & 0xF;
    let y = (idx >> 4) & 0xF;
    let z = (idx >> 8) & 0xF;
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