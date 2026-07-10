use std::env;
use std::path::Path;
use std::time::Instant;
use std::sync::atomic::Ordering;

// 引入我们 lib 库里已经实现的模块
use mc2mt::mc_map::MCMap;
use mc2mt::mt_map::{serialize_block, MTMap};
use mc2mt::convert::UNKNOWN_BLOCKS;

fn main() {
    let args: Vec<String> = env::args().collect();
    if args.len() != 3 {
        eprintln!("Usage: {} <input_minecraft_world> <output_minetest_world>", args[0]);
        std::process::exit(1);
    }

    let input_path = &args[1];
    let output_path = &args[2];

    println!("===============================================================");
    println!("             MC2MT - High Performance Rust Edition             ");
    println!("===============================================================");
    println!("Input path:  {}", input_path);
    println!("Output path: {}", output_path);
    println!("---------------------------------------------------------------");

    let start_time = Instant::now();

    // 1. 初始化 MC 地图元数据
    let mc_map = match MCMap::new(input_path) {
        Ok(m) => m,
        Err(e) => {
            eprintln!("Error: MCMap initialization failed: {}", e);
            std::process::exit(1);
        }
    };

    // 2. 初始化输出 Minetest SQLite 数据库
    let mut mt_map = match MTMap::new(output_path) {
        Ok(m) => m,
        Err(e) => {
            eprintln!("Error: MTMap initialization failed: {}", e);
            std::process::exit(1);
        }
    };

    // 3. 扫描区块组 (.mca 列表)
    let groups = match mc_map.list_groups() {
        Ok(g) => g,
        Err(e) => {
            eprintln!("Error: Listing groups failed: {}", e);
            std::process::exit(1);
        }
    };

    if groups.is_empty() {
        eprintln!("Error: No valid Region files found in {}", input_path);
        std::process::exit(1);
    }

    let total_groups = groups.len();
    println!("Found {} region groups to convert. Starting multithreading pipeline...", total_groups);

    let mut blocks_done = 0usize;

    for (i, group) in groups.iter().enumerate() {
        // 读取区块内所有的 Chunk 坐标
        if let Ok(chunk_positions) = mc_map.list_chunks(group) {
            
            // 使用 Rayon 极速并行解析
            use rayon::prelude::*;
            let transformed_blocks: Vec<_> = chunk_positions
                .par_iter()
                .filter_map(|&pos| mc_map.load_chunk(group, pos).ok())
                .flat_map(|mc_blocks| mc_blocks)
                .filter_map(|mcb| serialize_block(&mcb).ok())
                .collect();

            let count = transformed_blocks.len();

            // 批量高速刷入 SQLite 事务
            if !transformed_blocks.is_empty() {
                if let Err(e) = mt_map.save_blocks(transformed_blocks) {
                    eprintln!("\n[Error] Database write failed in {}: {}", group.name, e);
                } else {
                    blocks_done += count;
                }
            }
        }

        // 计算实时指标
        let elapsed = start_time.elapsed().as_secs();
        let blocks_sec = if elapsed > 0 { blocks_done / elapsed as usize } else { blocks_done };
        print!(
            "\rConversions progress: [{}/{}] groups | Converted {} blocks | Speed: {} blocks/sec",
            i + 1,
            total_groups,
            blocks_done,
            blocks_sec
        );
        use std::io::Write;
        std::io::stdout().flush().unwrap();
    }

    let total_s = start_time.elapsed().as_secs();
    println!("\n---------------------------------------------------------------");
    println!("Conversion completed in {}s!", total_s);
    println!("Total converted blocks: {}", blocks_done);

    // 输出未识别的方块（若有）
    if let Ok(unknown) = UNKNOWN_BLOCKS.lock() {
        if !unknown.is_empty() {
            println!("---------------------------------------------------------------");
            println!("Unrecognized Minecraft blocks encountered ({} types):", unknown.len());
            for name in unknown.iter() {
                print!(" {} ", name);
            }
            println!();
        }
    }
    println!("===============================================================");
}