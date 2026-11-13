import os
import re
import shutil
import sys

def patch_java_files(root_dir):
    system_exit_pattern = re.compile(r'(System\.exit\s*\([^)]*\)\s*;?)')
    
    chunker_block_identifier_found = False
    font_util_found = False

    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            if not filename.endswith('.java'):
                continue
                
            filepath = os.path.join(dirpath, filename)
            
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
            except Exception as e:
                print(f"Failed to read file {filepath}: {e}", file=sys.stderr)
                continue
                
            modified_content = content
            
            # Comment out System.exit
            if 'System.exit' in modified_content:
                modified_content = system_exit_pattern.sub(r'// \1', modified_content)
                
            # Fix ChunkerBlockIdentifier.java
            if filename == 'ChunkerBlockIdentifier.java':
                chunker_block_identifier_found = True
                modified_content = re.sub(r'!builder\.isEmpty\(\)', 'builder.length() > 0', modified_content)
                modified_content = re.sub(r'builder\.isEmpty\(\)', 'builder.length() == 0', modified_content)
                
            # Fix FontUtil.java
            if filename == 'FontUtil.java':
                font_util_found = True
                modified_content = re.sub(r'!currentLine\.isEmpty\(\)', 'currentLine.length() > 0', modified_content)
                modified_content = re.sub(r'currentLine\.isEmpty\(\)', 'currentLine.length() == 0', modified_content)
                modified_content = re.sub(r'!currentWord\.isEmpty\(\)', 'currentWord.length() > 0', modified_content)
                modified_content = re.sub(r'currentWord\.isEmpty\(\)', 'currentWord.length() == 0', modified_content)
                
            if modified_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(modified_content)
                print(f"Patched: {filepath}")

    if not chunker_block_identifier_found:
        print("Error: ChunkerBlockIdentifier.java not found", file=sys.stderr)
        sys.exit(1)
        
    if not font_util_found:
        print("Error: FontUtil.java not found", file=sys.stderr)
        sys.exit(1)
        
    print("Java source regex patching complete!")

def copy_overlay(src, dst):
    if not os.path.exists(src):
        print(f"Overlay directory does not exist: {src}")
        return
        
    print(f"Copying {src} over to {dst}...")
    
    # Recursively copy files and overwrite
    for root, _, files in os.walk(src):
        for file in files:
            src_file = os.path.join(root, file)
            # Compute relative path to preserve directory structure in destination
            rel_path = os.path.relpath(src_file, src)
            dst_file = os.path.join(dst, rel_path)
            
            # Ensure destination directory exists
            os.makedirs(os.path.dirname(dst_file), exist_ok=True)
            shutil.copy2(src_file, dst_file)
            print(f"Overwrote: {dst_file}")
            
    print("Overlay copy complete!")

if __name__ == '__main__':
    project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
    print("=== Starting Chunker Patch ===")
    
    # 1. Copy overlay
    overlay_dir = os.path.join(project_root, 'patches', 'overlay')
    cli_dir = os.path.join(project_root, 'cli')
    copy_overlay(overlay_dir, cli_dir)
    
    # 2. Patch Java sources
    patch_java_files(project_root)
    
    print(" Chunker Patch finished ")