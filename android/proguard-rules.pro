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

# 保留 picocli 框架
-keep class picocli.** { *; }

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

-keepnames class org.iq80.leveldb.table.Block
-keepnames class org.iq80.leveldb.table.BlockBuilder
-keepnames class org.iq80.leveldb.table.BlockHandle
-keepnames class org.iq80.leveldb.table.BlockHandleSliceWeigher
-keepnames class org.iq80.leveldb.table.BlockIterator
-keepnames class org.iq80.leveldb.table.BlockTrailer
-keepnames class org.iq80.leveldb.table.BloomFilterPolicy
-keepnames class org.iq80.leveldb.table.BytewiseComparator
-keepnames class org.iq80.leveldb.table.CacheKey
-keepnames class org.iq80.leveldb.table.FilterBlockBuilder
-keepnames class org.iq80.leveldb.table.FilterBlockReader
-keepnames class org.iq80.leveldb.table.FilterPolicy
-keepnames class org.iq80.leveldb.table.Footer
-keepnames class org.iq80.leveldb.table.KeyValueFunction
-keepnames class org.iq80.leveldb.table.RestartPositions
-keepnames class org.iq80.leveldb.table.Table
-keepnames class org.iq80.leveldb.table.TableBuilder
-keepnames class org.iq80.leveldb.table.UserComparator

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