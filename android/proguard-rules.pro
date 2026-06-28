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

-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty { *; }
-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty$* { *; }
-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor { *; }
-keep class com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemDisplay { *; }

-keepclassmembers class org.apache.mina.transport.socket.nio.NioProcessor {
    protected <methods>;
    public <methods>;
}

-keepnames class com.google.common.cache.CacheLoader { *; }

# 动画相关
-assumenosideeffects class com.google.android.material.animation.** {
    <methods>;
}

# AppBar（旧式ActionBar/Toolbar）
-assumenosideeffects class com.google.android.material.appbar.** {
    <methods>;
}

# Badge（角标）
-assumenosideeffects class com.google.android.material.badge.** {
    <methods>;
}

# Behavior（协调者布局行为）
-assumenosideeffects class com.google.android.material.behavior.** {
    <methods>;
}

# BottomAppBar（底部应用栏）
-assumenosideeffects class com.google.android.material.bottomappbar.** {
    <methods>;
}

# BottomNavigation（底部导航）
-assumenosideeffects class com.google.android.material.bottomnavigation.** {
    <methods>;
}

# BottomSheet（底部弹窗）
-assumenosideeffects class com.google.android.material.bottomsheet.** {
    <methods>;
}

# Button（各种按钮）
-assumenosideeffects class com.google.android.material.button.** {
    <methods>;
}

# Canvas（画布工具）
-assumenosideeffects class com.google.android.material.canvas.** {
    <methods>;
}

# Card（卡片视图）
-assumenosideeffects class com.google.android.material.card.** {
    <methods>;
}

# CheckBox（复选框）
-assumenosideeffects class com.google.android.material.checkbox.** {
    <methods>;
}

# Chip（芯片组）
-assumenosideeffects class com.google.android.material.chip.** {
    <methods>;
}

# CircularReveal（圆形揭示动画）
-assumenosideeffects class com.google.android.material.circularreveal.** {
    <methods>;
}

# Color（颜色工具）
-assumenosideeffects class com.google.android.material.color.** {
    <methods>;
}

# DatePicker（日期选择器）
-assumenosideeffects class com.google.android.material.datepicker.** {
    <methods>;
}

# Dialog（对话框）
-assumenosideeffects class com.google.android.material.dialog.** {
    <methods>;
}

# Divider（分割线）
-assumenosideeffects class com.google.android.material.divider.** {
    <methods>;
}

# Drawable（可绘制资源）
-assumenosideeffects class com.google.android.material.drawable.** {
    <methods>;
}

# Elevation（高程阴影）
-assumenosideeffects class com.google.android.material.elevation.** {
    <methods>;
}

# Expandable（可展开布局）
-assumenosideeffects class com.google.android.material.expandable.** {
    <methods>;
}

# FloatingActionButton（悬浮按钮）
-assumenosideeffects class com.google.android.material.floatingactionbutton.** {
    <methods>;
}

# ImageView（图片视图）
-assumenosideeffects class com.google.android.material.imageview.** {
    <methods>;
}

# Internal（内部实现）
-assumenosideeffects class com.google.android.material.internal.** {
    <methods>;
}

# Lists（列表项）
-assumenosideeffects class com.google.android.material.lists.** {
    <methods>;
}

# Math（数学工具）
-assumenosideeffects class com.google.android.material.math.** {
    <methods>;
}

# Menu（菜单）
-assumenosideeffects class com.google.android.material.menu.** {
    <methods>;
}

# Motion（运动动画）
-assumenosideeffects class com.google.android.material.motion.** {
    <methods>;
}

# Navigation（导航栏）
-assumenosideeffects class com.google.android.material.navigation.** {
    <methods>;
}

# NavigationRail（侧边导航轨）
-assumenosideeffects class com.google.android.material.navigationrail.** {
    <methods>;
}

# ProgressIndicator（进度指示器）
-assumenosideeffects class com.google.android.material.progressindicator.** {
    <methods>;
}

# RadioButton（单选按钮）
-assumenosideeffects class com.google.android.material.radiobutton.** {
    <methods>;
}

# Resources（资源工具）
-assumenosideeffects class com.google.android.material.resources.** {
    <methods>;
}

# Ripple（涟漪效果）
-assumenosideeffects class com.google.android.material.ripple.** {
    <methods>;
}

# Shadow（阴影）
-assumenosideeffects class com.google.android.material.shadow.** {
    <methods>;
}

# Shape（形状）
-assumenosideeffects class com.google.android.material.shape.** {
    <methods>;
}

# Slider（滑动条）
-assumenosideeffects class com.google.android.material.slider.** {
    <methods>;
}

# Snackbar（底部提示条）
-assumenosideeffects class com.google.android.material.snackbar.** {
    <methods>;
}

# Stateful（状态管理）
-assumenosideeffects class com.google.android.material.stateful.** {
    <methods>;
}

# SwitchMaterial（开关）
-assumenosideeffects class com.google.android.material.switchmaterial.** {
    <methods>;
}

# Tabs（标签页）
-assumenosideeffects class com.google.android.material.tabs.** {
    <methods>;
}

# TextField（文本输入框）
-assumenosideeffects class com.google.android.material.textfield.** {
    <methods>;
}

# TextView（文本视图）
-assumenosideeffects class com.google.android.material.textview.** {
    <methods>;
}

# TimePicker（时间选择器）
-assumenosideeffects class com.google.android.material.timepicker.** {
    <methods>;
}

# Tooltip（提示框）
-assumenosideeffects class com.google.android.material.tooltip.** {
    <methods>;
}

# Transformation（变换动画）
-assumenosideeffects class com.google.android.material.transformation.** {
    <methods>;
}

# Transition（过渡动画）
-assumenosideeffects class com.google.android.material.transition.** {
    <methods>;
}

# Typography（字体排版）
-assumenosideeffects class com.google.android.material.typography.** {
    <methods>;
}