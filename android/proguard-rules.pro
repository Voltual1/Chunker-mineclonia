-assumenosideeffects class **$$Lambda$* { *; }
 -assumenosideeffects class android.util.Log { *; }
-assumenosideeffects class kotlinx.coroutines.DebugStrings {
    public static *** toString(...);
}
-keepnames class com.hivemc.chunker.cli.** { *; }
-keepnames class org.iq80.leveldb.** { *; }
-keepattributes *Annotation*


-keep class com.hivemc.chunker.cli.VersionProvider {
    public <init>();
}

-keep public class com.hivemc.chunker.cli.JsonObjectOrFile$Converter {
    public <init>();
    public com.hivemc.chunker.cli.JsonObjectOrFile convert(java.lang.String);
}