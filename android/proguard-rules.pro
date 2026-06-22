-assumenosideeffects class **$$Lambda$* { *; }
 -assumenosideeffects class android.util.Log { *; }
-assumenosideeffects class kotlinx.coroutines.DebugStrings {
    public static *** toString(...);
}
-keep class com.hivemc.chunker.cli.** { *; }
-keepnames class org.iq80.leveldb.** { *; }