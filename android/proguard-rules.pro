-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-assumenosideeffects class **$$Lambda$* { *; }
-assumenosideeffects class android.util.Log { *; }
-assumenosideeffects class kotlinx.coroutines.DebugStrings {
    public static *** toString(...);
}
-keep class picocli.** { *; }
-keepclasseswithmembers class * {
    @picocli.CommandLine$Option <fields>;
}
-keepclasseswithmembers class * {
    @picocli.CommandLine$Parameters <fields>;
}
-keepclasseswithmembers class * {
    @picocli.CommandLine$Mixin <fields>;
}
-keepclasseswithmembers class * {
    @picocli.CommandLine$Unmatched <fields>;
}
-keepclasseswithmembers class * {
    @picocli.CommandLine$Spec <fields>;
}

-keep class com.hivemc.chunker.cli.** {
    @picocli.CommandLine$Command *;
    @picocli.CommandLine$Option *;
    @picocli.CommandLine$Parameters *;
    @picocli.CommandLine$ParentCommand *;
    public <init>(...);
    public *;
}

-keep class * implements picocli.CommandLine$ITypeConverter {
    public <init>();
}

-keep class * implements picocli.CommandLine$IVersionProvider {
    public <init>();
    public java.lang.String[] getVersion();
}

-keep class org.iq80.leveldb.table.TableBuilder { *; }

-keep class com.google.common.reflect.TypeToken { public protected *; }
-keep class com.google.common.reflect.TypeCapture { public protected *; }
-keep class * extends com.google.common.reflect.TypeToken { public protected *; }
-keep class * extends com.google.common.reflect.TypeCapture { public protected *; }

-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty { *; }
-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty$* { *; }
-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor { *; }
-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemDisplay { *; }

# =====================================================================
# Apache FtpServer & Apache MINA 核心保持规则
# =====================================================================

# === core ===
-keep class org.apache.mina.core.IoUtil { *; }
-keep class org.apache.mina.core.RuntimeIoException { *; }
-keep class org.apache.mina.core.buffer.** { *; }
-keep class org.apache.mina.core.file.** { *; }
-keep class org.apache.mina.core.filterchain.** { *; }
-keep class org.apache.mina.core.future.** { *; }
-keep class org.apache.mina.core.polling.** { *; }
-keep class org.apache.mina.core.service.** { *; }
-keep class org.apache.mina.core.session.** { *; }
-keep class org.apache.mina.core.write.** { *; }

# === filter (注释掉) ===
# -keep class org.apache.mina.filter.FilterEvent { *; }
# -keep class org.apache.mina.filter.buffer.** { *; }
# -keep class org.apache.mina.filter.codec.** { *; }
# -keep class org.apache.mina.filter.codec.demux.** { *; }
# -keep class org.apache.mina.filter.codec.prefixedstring.** { *; }
# -keep class org.apache.mina.filter.codec.serialization.** { *; }
# -keep class org.apache.mina.filter.codec.statemachine.** { *; }
# -keep class org.apache.mina.filter.codec.textline.** { *; }
# -keep class org.apache.mina.filter.errorgenerating.** { *; }
# -keep class org.apache.mina.filter.executor.** { *; }
# -keep class org.apache.mina.filter.firewall.** { *; }
# -keep class org.apache.mina.filter.keepalive.** { *; }
# -keep class org.apache.mina.filter.logging.** { *; }
# -keep class org.apache.mina.filter.ssl.** { *; }
# -keep class org.apache.mina.filter.statistic.** { *; }
# -keep class org.apache.mina.filter.stream.** { *; }
# -keep class org.apache.mina.filter.util.** { *; }

# === handler / proxy / transport / util (全注释) ===
# ... 不变

# 防止编译期因某些 Java 环境类缺失而报错
-dontwarn org.apache.ftpserver.**
-dontwarn org.apache.mina.**

# 保持反射相关的构造函数
-keepclassmembers class * extends org.apache.ftpserver.usermanager.impl.AbstractUserManager {
    public <init>(...);
}