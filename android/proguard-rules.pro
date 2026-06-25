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

# === transport ===
-keep class org.apache.mina.transport.socket.nio.NioDatagramAcceptor { *; }
-keep class org.apache.mina.transport.socket.nio.NioDatagramAcceptor$* { *; }
-keep class org.apache.mina.transport.socket.nio.NioDatagramConnector { *; }
-keep class org.apache.mina.transport.socket.nio.NioDatagramSession { *; }
-keep class org.apache.mina.transport.socket.nio.NioDatagramSessionConfig { *; }
-keep class org.apache.mina.transport.socket.nio.NioProcessor { *; }
-keep class org.apache.mina.transport.socket.nio.NioProcessor$* { *; }
-keep class org.apache.mina.transport.socket.nio.NioSession { *; }

# -keep class org.apache.mina.transport.socket.nio.NioSocketAcceptor { *; }
# -keep class org.apache.mina.transport.socket.nio.NioSocketAcceptor$* { *; }
# -keep class org.apache.mina.transport.socket.nio.NioSocketConnector { *; }
# -keep class org.apache.mina.transport.socket.nio.NioSocketConnector$* { *; }
# -keep class org.apache.mina.transport.socket.nio.NioSocketSession { *; }
# -keep class org.apache.mina.transport.socket.nio.NioSocketSession$* { *; }