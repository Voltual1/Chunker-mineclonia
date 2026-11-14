import os
import re
import shutil
import sys

def copy_overlay(src, dst):
    if not os.path.exists(src):
        print(f"[Chunker] Overlay 目录不存在: {src}")
        return
        
    print(f"[Chunker] 正在将 {src} 覆盖到 {dst}...")
    for root, _, files in os.walk(src):
        for file in files:
            src_file = os.path.join(root, file)
            rel_path = os.path.relpath(src_file, src)
            dst_file = os.path.join(dst, rel_path)
            
            os.makedirs(os.path.dirname(dst_file), exist_ok=True)
            shutil.copy2(src_file, dst_file)
            print(f"[Chunker] 已覆盖: {dst_file}")
            
    print("[Chunker] Overlay 覆盖完成！")

def patch_java_source(target_dir):
    system_exit_pattern = re.compile(r'(System\.exit\s*\([^)]*\)\s*;?)')
    
    chunker_block_identifier_found = False
    font_util_found = False

    for dirpath, _, filenames in os.walk(target_dir):
        for filename in filenames:
            if not filename.endswith('.java'):
                continue
                
            filepath = os.path.join(dirpath, filename)
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
            except Exception as e:
                print(f"[Chunker] 读取文件 {filepath} 失败: {e}", file=sys.stderr)
                continue
                
            modified_content = content
            
            # 注释 System.exit
            if 'System.exit' in modified_content:
                modified_content = system_exit_pattern.sub(r'// \1', modified_content)
                
            # 修复 ChunkerBlockIdentifier.java
            if filename == 'ChunkerBlockIdentifier.java':
                chunker_block_identifier_found = True
                modified_content = re.sub(r'!builder\.isEmpty\(\)', 'builder.length() > 0', modified_content)
                modified_content = re.sub(r'builder\.isEmpty\(\)', 'builder.length() == 0', modified_content)
                
            # 修复 FontUtil.java
            if filename == 'FontUtil.java':
                font_util_found = True
                modified_content = re.sub(r'!currentLine\.isEmpty\(\)', 'currentLine.length() > 0', modified_content)
                modified_content = re.sub(r'currentLine\.isEmpty\(\)', 'currentLine.length() == 0', modified_content)
                modified_content = re.sub(r'!currentWord\.isEmpty\(\)', 'currentWord.length() > 0', modified_content)
                modified_content = re.sub(r'currentWord\.isEmpty\(\)', 'currentWord.length() == 0', modified_content)
                
            if modified_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(modified_content)
                print(f"[Chunker] 已修补: {filepath}")

    if not chunker_block_identifier_found:
        print("[Chunker] 错误: 未找到 ChunkerBlockIdentifier.java", file=sys.stderr)
        sys.exit(1)
        
    if not font_util_found:
        print("[Chunker] 错误: 未找到 FontUtil.java", file=sys.stderr)
        sys.exit(1)
        
    print("[Chunker] Java 源码修补完成！")

def apply_patch():
    patches_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(patches_dir, '..'))
    
    print("=== 开始执行 Chunker 模块 Patch ===")
    overlay_dir = os.path.join(patches_dir, 'chunker_overlay')
    cli_dir = os.path.join(project_root, 'cli')
    
    copy_overlay(overlay_dir, cli_dir)
    patch_java_source(cli_dir)
    print("=== Chunker 模块 Patch 完成 ===")

if __name__ == '__main__':
    apply_patch()