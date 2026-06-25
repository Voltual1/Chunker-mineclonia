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

# =====================================================================
# Apache FtpServer & Apache MINA 核心保持规则
# =====================================================================

# === handler ===
-keep class org.apache.mina.handler.chain.** { *; }
-keep class org.apache.mina.handler.demux.** { *; }
-keep class org.apache.mina.handler.multiton.** { *; }
-keep class org.apache.mina.handler.stream.** { *; }

# === proxy ===
-keep class org.apache.mina.proxy.AbstractProxyIoHandler { *; }
-keep class org.apache.mina.proxy.AbstractProxyLogicHandler { *; }
-keep class org.apache.mina.proxy.AbstractProxyLogicHandler$Event { *; }
-keep class org.apache.mina.proxy.ProxyAuthException { *; }
-keep class org.apache.mina.proxy.ProxyConnector { *; }
-keep class org.apache.mina.proxy.ProxyLogicHandler { *; }
-keep class org.apache.mina.proxy.event.** { *; }
-keep class org.apache.mina.proxy.filter.** { *; }
-keep class org.apache.mina.proxy.handlers.** { *; }
-keep class org.apache.mina.proxy.handlers.http.** { *; }
-keep class org.apache.mina.proxy.handlers.http.basic.** { *; }
-keep class org.apache.mina.proxy.handlers.http.digest.** { *; }
-keep class org.apache.mina.proxy.handlers.http.ntlm.** { *; }
-keep class org.apache.mina.proxy.handlers.socks.** { *; }
-keep class org.apache.mina.proxy.session.** { *; }
-keep class org.apache.mina.proxy.utils.** { *; }


# 防止编译期因某些 Java 环境类缺失而报错
-dontwarn org.apache.ftpserver.**
-dontwarn org.apache.mina.**

# 保持反射相关的构造函数
-keepclassmembers class * extends org.apache.ftpserver.usermanager.impl.AbstractUserManager {
    public <init>(...);
}