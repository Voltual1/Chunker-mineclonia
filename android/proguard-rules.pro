-assumenosideeffects class **$$Lambda$* { *; }
 -assumenosideeffects class android.util.Log { *; }
-assumenosideeffects class kotlinx.coroutines.DebugStrings {
    public static *** toString(...);
}

# 替换为更全面的规则:
-keep class com.hivemc.chunker.cli.** {
    @picocli.CommandLine$Command *;
    @picocli.CommandLine$Option *;
    @picocli.CommandLine$Parameters *;
    @picocli.CommandLine$ParentCommand *;
    public <init>(...);
    public *;
}

# 保留类型转换器
-keep class * implements picocli.CommandLine$ITypeConverter {
    public <init>();
    public *;
}

# 保留 VersionProvider
-keep class * implements picocli.CommandLine$IVersionProvider {
    public <init>();
    public java.lang.String[] getVersion();
}

-keepnames class org.iq80.leveldb.** { *; }

# ===== 基础属性保留 =====
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepattributes RuntimeVisible*Annotations, RuntimeInvisible*Annotations

# ===== Guava TypeToken/TypeCapture
-keep class com.google.common.reflect.TypeToken {
    public protected *;
}
-keep class com.google.common.reflect.TypeCapture {
    public protected *;
}
# 保留所有匿名子类
-keep class * extends com.google.common.reflect.TypeToken {
    public protected *;
}
-keep class * extends com.google.common.reflect.TypeCapture {
    public protected *;
}

# ===== ChunkerItemProperty 及其内部类
-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty {
    <fields>;
    <methods>;
    public protected <init>(...);
}
-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty$* {
    <fields>;
    <methods>;
    public protected <init>(...);
}

# ===== 保留被 TypeToken 直接引用的类 =====
-keep class androidx.core.os.HandlerCompat { *; }
-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor { *; }
-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemDisplay { *; }