-assumenosideeffects class **$$Lambda$* { *; }
 -assumenosideeffects class android.util.Log { *; }
-assumenosideeffects class kotlinx.coroutines.DebugStrings {
    public static *** toString(...);
}

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

-keepnames class org.iq80.leveldb.** { *; }
-keepattributes *Annotation*