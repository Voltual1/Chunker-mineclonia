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

-keepclassmembers class org.iq80.leveldb.table.TableBuilder {
    static <clinit>();
    public <methods>;
    protected <methods>;
}

-keep class com.google.common.reflect.TypeToken { public protected *; }
-keep class com.google.common.reflect.TypeCapture { public protected *; }
-keep class * extends com.google.common.reflect.TypeToken { public protected *; }
-keep class * extends com.google.common.reflect.TypeCapture { public protected *; }

#-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty { *; }
#-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty$* { *; }
#-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor { *; }
#-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemDisplay { *; }

-keep class com.hivemc.chunker.conversion.intermediate.** { *; }

-keepclassmembers class org.apache.mina.transport.socket.nio.NioProcessor {
    protected <methods>;
    public <methods>;
}

-keepnames class com.google.common.cache.** { *; }

-dontwarn com.google.typography.font.sfntly.**

-dontwarn java.awt.font.sfntly.**

-dontwarn lombok.**

-keepclassmembers class org.geysermc.pack.converter.util.VanillaPackProvider$* {
    <init>(...);
}

-keepclassmembers class org.geysermc.pack.converter.util.VanillaPackProvider$* {
    <fields>;
}

-keep class com.hivemc.chunker.conversion.intermediate.level.ChunkerLevelSettings {
    <fields>;
    <methods>;
}



















-repackageclasses 'androidx'
-flattenpackagehierarchy 'androidx'
#这不是为了好玩，是为了镇压 java.awt 内部残存的 SPI 恶灵！
#由于 Java 跨平台材质包转换机制的底层缺陷，如果把这些包名平铺到其他地方，
#反射机制会直接抓瞎，在触发“材质包转换”时会原地爆炸，抛出不可逆的 java.lang.NullPointerException！
#而androidx这个地方是受官方保护的圣地！ 
#如果你觉得这两行“很神经”而顺手删掉，编译虽然能过，但只要运行到材质包转换功能，
#程序就会炸了。此时请不要来提 Issue，因为神仙也救不了你。
#留着它，它保你材质包转换一路顺风；删掉它，你没有好果子吃了
# 也千万不要试图把 'androidx' 改成自定义包名
# 因为 R8 在执行高级混淆平铺时，为了极致压缩体积，机制上不允许“无中生有”创建全新的未定义根包。
# 强行自定义新包名会导致 R8 字典映射错乱，编译直接Error`
# 必须借用 Android 官方已经注册并在白名单内的 'androidx' 圣地作为宿主。