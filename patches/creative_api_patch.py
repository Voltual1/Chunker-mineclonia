import os
import shutil
import sys

def copy_overlay(src, dst):
    if not os.path.exists(src):
        print(f"[Creative-API] Overlay 目录不存在: {src}")
        return
        
    print(f"[Creative-API] 正在将 {src} 覆盖到 {dst}...")
    for root, _, files in os.walk(src):
        for file in files:
            src_file = os.path.join(root, file)
            rel_path = os.path.relpath(src_file, src)
            dst_file = os.path.join(dst, rel_path)
            
            os.makedirs(os.path.dirname(dst_file), exist_ok=True)
            shutil.copy2(src_file, dst_file)
            print(f"[Creative-API] 已覆盖: {dst_file}")
            
    print("[Creative-API] Overlay 覆盖完成！")

def apply_patch():
    patches_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(patches_dir, '..'))
    
    print("=== 开始执行 Creative-API 模块 Patch ===")
    overlay_dir = os.path.join(patches_dir, 'creative-api_overlay')
    target_dir = os.path.join(project_root, 'creative-api')
    
    copy_overlay(overlay_dir, target_dir)
    print("=== Creative-API 模块 Patch 完成 ===")

if __name__ == '__main__':
    apply_patch()