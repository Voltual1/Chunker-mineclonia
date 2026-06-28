-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepnames class com.hivemc.chunker.** { *; }
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


# 1. 顶层特殊或基础平台包
-keepnames class _COROUTINE.** { *; }
-keepnames class android.** { *; }
-keepnames class androidx.** { *; }
-keepnames class j$.** { *; }
-keepnames class java.** { *; }
-keepnames class javax.** { *; }

# 2. Kotlin 核心及扩展
-keepnames class kotlin.** { *; }
-keepnames class kotlinx.** { *; }

# 3. com.* 依赖细分
-keepnames class com.anggrayudi.** { *; }
-keepnames class com.google.** { *; }
-keepnames class com.hivemc.** { *; }
-keepnames class com.termux.** { *; }

# 4. io.* 依赖细分
-keepnames class io.github.** { *; }
-keepnames class io.ktor.** { *; }

# 5. org.* 依赖细分
-keepnames class org.apache.** { *; }
-keepnames class org.intellij.** { *; }
-keepnames class org.iq80.** { *; }
-keepnames class org.koin.** { *; }
-keepnames class org.slf4j.** { *; }

# 6. 其他独立三方库及业务包
-keepnames class it.unimi.** { *; }
-keepnames class me.voltual.** { *; }
-keepnames class net.jpountz.** { *; }
-keepnames class okhttp3.** { *; }
-keepnames class okio.** { *; }
-keepnames class picocli.** { *; }
-keepnames class ro.andob.** { *; }