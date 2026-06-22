# ==============================================================================
# R8 NPE 排查专用混淆规则 (二分法查找专用)
# 核心原理：保持类名和成员名不被混淆/移除，逐步注释掉半数规则来缩小范围。
# ==============================================================================

# ---- 组 5：项目自研/特定业务与第三方工具 (Minecraft/HiveMC/Termux 等) ----
-keepnames class me.voltual.** { *; }            # 看起来是你的核心业务包
-keepnames class com.hivemc.chunker.** { *; }      # Chunker 地图转换库
-keepnames class com.anggrayudi.storage.** { *; }  # 存储权限/文件工具
-keepnames class com.termux.** { *; }              # Termux 终端相关
-keepnames class picocli.** { *; }                 # 命令行解析器

# ---- 组 6：其他底层和辅助依赖库 (Org, Net, It, Ro, Java 兼容层) ----
-keepnames class org.apache.commons.imaging.** { *; }
-keepnames class org.harmony.** { *; }
-keepnames class org.intellij.markdown.** { *; }
-keepnames class org.iq80.leveldb.** { *; }
-keepnames class org.jetbrains.compose.** { *; }
-keepnames class org.koin.** { *; }
-keepnames class org.slf4j.** { *; }
-keepnames class net.jpountz.** { *; }              # LZ4/XXHash 压缩
-keepnames class it.unimi.dsi.fastutil.** { *; }    # 高性能集合
-keepnames class ro.andob.awtcompat.** { *; }       # AWT 兼容层
-keepnames class j$.** { *; }                       # Java 8+ API 脱糖支持
-keepnames class java.awt.** { *; }
-keepnames class javax.imageio.** { *; }