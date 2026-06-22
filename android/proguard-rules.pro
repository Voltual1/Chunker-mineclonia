-assumenosideeffects class **$$Lambda$* { *; }
 -assumenosideeffects class android.util.Log { *; }
-assumenosideeffects class kotlinx.coroutines.DebugStrings {
    public static *** toString(...);
}
-keep class com.hivemc.chunker.cli.** { *; }
-keepnames class org.iq80.leveldb.** { *; }

# 保持 picocli 的注解不被 R8 优化和移除
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep @picocli.CommandLine.Command class * {*;}
-keep class * implements picocli.CommandLine.IExitCodeGenerator