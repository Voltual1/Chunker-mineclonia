import os
import re

def patch_file(file_path, search_pattern, replace_content, multi_line=False):
    if not os.path.exists(file_path):
        print(f"跳过: 未找到 {file_path}")
        return
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    flags = re.MULTILINE | re.DOTALL if multi_line else re.MULTILINE
    if re.search(search_pattern, content, flags):
        new_content = re.sub(search_pattern, replace_content, content, flags=flags)
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"成功: 已修改 {file_path}")
    else:
        print(f"警告: 在 {file_path} 中未匹配到目标模式")

def main():
    # 1. 注释掉所有的 System.exit
    print("正在注释 System.exit...")
    for root, _, files in os.walk("."):
        for file in files:
            if file.endswith(".java"):
                full_path = os.path.join(root, file)
                patch_file(full_path, r'(System\.exit\s*\([^)]*\)\s*;?)', r'// \1')

    # 2. 修复 ChunkerBlockIdentifier 的 StringBuilder.isEmpty (针对低版本 Android API)
    print("正在修复 StringBuilder.isEmpty...")
    target_identifier = ""
    for root, _, files in os.walk("."):
        if "ChunkerBlockIdentifier.java" in files:
            target_identifier = os.path.join(root, "ChunkerBlockIdentifier.java")
            break
    
    if target_identifier:
        patch_file(target_identifier, r'!builder\.isEmpty\(\)', r'builder.length() > 0')
        patch_file(target_identifier, r'builder\.isEmpty\(\)', r'builder.length() == 0')

    # 3. 核心优化：WorldConverter.java (内存与并行度)
    print("正在应用 WorldConverter 内存优化补丁...")
    target_converter = ""
    for root, _, files in os.walk("."):
        if "WorldConverter.java" in files:
            target_converter = os.path.join(root, "WorldConverter.java")
            break

    if target_converter:
        # 插入线程字段
        patch_file(target_converter, 
                  r'private boolean cancelled = false;', 
                  r'private boolean cancelled = false;\n    \n    // JVM / Android optimized thread count setting\n    private int threadCount = Math.max(1, Runtime.getRuntime().availableProcessors() / 4);')
        
        # 插入 setThreadCount 方法 (在构造函数后)
        patch_file(target_converter, 
                  r'this\.sessionID = sessionID;\s*\}', 
                  r'this.sessionID = sessionID;\n    }\n\n    /**\n     * Set the thread count used for concurrent tasks.\n     * @param threadCount thread count (suggested 1 or 2 for mobile).\n     */\n    public void setThreadCount(int threadCount) {\n        this.threadCount = Math.max(1, threadCount);\n    }', multi_line=True)

        # 替换 Task.environment 调用
        patch_file(target_converter,
                  r'environment = Task\.environment\("World Conversion", 8, this::logFatalException, this::handleSignal\);',
                  r'// Android OOM Backpressure / Concurrency tuning: Use the dynamic threadCount\n        environment = Task.environment("World Conversion", this.threadCount, this::logFatalException, this::handleSignal);')

        # 在 LevelHandler 中插入 System.gc()
        patch_file(target_converter,
                  r'public Task<WorldConversionHandler> convertLevel\(ChunkerLevel level\) \{',
                  r'public Task<WorldConversionHandler> convertLevel(ChunkerLevel level) {\n            // JVM Backpressure: triggers GC right before loading worlds to clean up map loading residual overhead\n            System.gc();', multi_line=True)

if __name__ == "__main__":
    main()