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

# 1. 保持 ftpserver 和 mina 的所有包、类、方法及构造函数不被混淆和裁剪
-keep class org.apache.ftpserver.** { *; }
-keep class org.apache.mina.** { *; }

# 2. 允许对这些包产生的部分警告进行忽略（防止编译期因某些 Java 环境类缺失而报错）
-dontwarn org.apache.ftpserver.**
-dontwarn org.apache.mina.**

# 3. 核心：必须保持类中的泛型签名、注解、异常以及内部类属性
# R8 在 Full Mode 下会极其激进地剥离泛型和属性，这会导致 MINA 的某些过滤器转换失败
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses,Exceptions

# 4. 保持依赖的日志框架 SLF4J 不被混淆（FtpServer 强依赖它，找不到会导致启动直接白屏/闪退）
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# 5. 如果你使用了默认的基于 Properties 的用户管理器，必须保持反射相关的构造函数
-keepclassmembers class * extends org.apache.ftpserver.usermanager.impl.AbstractUserManager {
    public <init>(...);
}

# 6. 保持所有 FTP 命令的实现类（Apache 内部使用反射根据字符串实例化命令，如 "USER", "PASS"）
-keep class org.apache.ftpserver.command.impl.** { *; }
