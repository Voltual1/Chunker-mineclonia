-keepnames class ** { *; }
-assumenosideeffects class **$$Lambda$* { *; }
 -assumenosideeffects class android.util.Log { *; }
-assumenosideeffects class kotlinx.coroutines.DebugStrings {
    public static *** toString(...);
}
# 忽略 Chunker 引用的桌面端 JDK 绘图相关类
-dontwarn java.awt.**
-dontwarn javax.imageio.**

# Caffeine 缓存库引用的 Java 9+ 系统日志类，Android 上也没有，直接忽略
-dontwarn java.lang.System$Logger**

# 强行保留并处理 com.android.tools.r8.RecordTag 标记
# 这会强制让编译链在任何时候都妥善闭环 Record 的脱糖处理
-keep class com.android.tools.r8.RecordTag { *; }
-dontwarn com.android.tools.r8.RecordTag
-dontoptimize

# 保持 VersionProvider 及其构造函数不被混淆和移除
-keep class com.hivemc.chunker.cli.VersionProvider {
    public <init>();
}
# 防止 picocli 反射失效
-keep class picocli.** { *; }

-keep public class com.hivemc.chunker.cli.JsonObjectOrFile$Converter {
    public <init>();
    public com.hivemc.chunker.cli.JsonObjectOrFile convert(java.lang.String);
}

# 彻底放过 chunker 库下的所有 CLI 相关类，不混淆、不优化、不压缩
-keep class com.hivemc.chunker.cli.** { *; }

-dontwarn org.bouncycastle.**
-dontwarn java.lang.invoke.**