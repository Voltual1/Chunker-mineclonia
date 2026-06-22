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

# 关键:保留注解和参数名
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, MethodParameters, Signature

-keepnames class org.iq80.leveldb.** { *; }