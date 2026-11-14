import os
import sys

# 引入同目录下的各个 patch 模块
from chunker_patch import apply_patch as apply_chunker_patch
from creative_api_patch import apply_patch as apply_creative_api_patch
from creative_serializer_minecraft_patch import apply_patch as apply_creative_serializer_patch
from packconverter_patch import apply_patch as apply_packconverter_patch

def main():
    print("========================================")
    print("      开始执行项目全局补丁修补流程       ")
    print("========================================")
    
    try:
        # 1. 执行 Chunker 补丁
        apply_chunker_patch()
        
        # 2. 执行 Creative-API 补丁
        apply_creative_api_patch()
        
        # 3. 执行 Creative-Serializer-Minecraft 补丁
        apply_creative_serializer_patch()
        
        # 4. 执行 PackConverter 补丁
        apply_packconverter_patch()
        
        print("========================================")
        print("      所有模块补丁均已成功修补完成！     ")
        print("========================================")
    except Exception as e:
        print(f"修补过程中发生异常中断: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == '__main__':
    main()