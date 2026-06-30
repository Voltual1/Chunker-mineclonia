-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-assumenosideeffects class **$$Lambda$* { *; }
-assumenosideeffects class android.util.Log { *; }
#-assumenosideeffects class ro.andob.awtcompat.nativec.** { *; }
#现在需要处理材质包转换可能会需要用到so
-assumenosideeffects class kotlinx.coroutines.DebugStrings {
    public static *** toString(...);
}

# -keep class picocli.** { *; }
# -keepclasseswithmembers class * {
#     @picocli.CommandLine$Option <fields>;
# }
# -keepclasseswithmembers class * {
#     @picocli.CommandLine$Parameters <fields>;
# }
# -keepclasseswithmembers class * {
#     @picocli.CommandLine$Mixin <fields>;
# }
# -keepclasseswithmembers class * {
#     @picocli.CommandLine$Unmatched <fields>;
# }
# -keepclasseswithmembers class * {
#     @picocli.CommandLine$Spec <fields>;
# }

# -keep class com.hivemc.chunker.cli.** {
#     @picocli.CommandLine$Command *;
#     @picocli.CommandLine$Option *;
#     @picocli.CommandLine$Parameters *;
#     @picocli.CommandLine$ParentCommand *;
#     public <init>(...);
#     public *;
# }

# -keep class * implements picocli.CommandLine$ITypeConverter {
#     public <init>();
# }

# -keep class * implements picocli.CommandLine$IVersionProvider {
#     public <init>();
#     public java.lang.String[] getVersion();
# }
# --------------------------------------------

-keepclassmembers class org.iq80.leveldb.table.TableBuilder {
    static <clinit>();
    public <methods>;
    protected <methods>;
}

-keep class com.google.common.reflect.TypeToken { public protected *; }
-keep class com.google.common.reflect.TypeCapture { public protected *; }
-keep class * extends com.google.common.reflect.TypeToken { public protected *; }
-keep class * extends com.google.common.reflect.TypeCapture { public protected *; }

-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty { *; }
-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty$* { *; }
-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor { *; }
-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemDisplay { *; }

-keepclassmembers class org.apache.mina.transport.socket.nio.NioProcessor {
    protected <methods>;
    public <methods>;
}

-keepnames class com.google.common.cache.** { *; }

# 保护 NativeImageFormat 及其全部实例字段（C 语言 JNI 依赖名称和签名直接映射）
-keep class org.apache.harmony.awt.gl.color.NativeImageFormat {
    private int cmmFormat;
    private int rows;
    private int cols;
    private int scanlineStride;
    private java.lang.Object imageData;
    private int dataOffset;
    private int alphaOffset;
    public <init>(...);
    public <methods>;
}

# 保护 JNI 绑定类极其子成员
-keep class org.apache.harmony.awt.gl.color.NativeCMM { *; }
-keep class ro.andob.awtcompat.nativec.AwtCompatNativeComponents { *; }
-keep class ro.andob.awtcompat.nativec.AwtCompatNativeComponents$NativePointerContainer { *; }
-keep class org.apache.harmony.awt.gl.image.GifDecoder** { *; }