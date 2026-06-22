# ==============================================================================
# R8 NPE 排查专用混淆规则 (二分法查找专用)
# 核心原理：保持类名和成员名不被混淆/移除，逐步注释掉半数规则来缩小范围。
# ==============================================================================

# ---- 组 1：Android 官方官方核心库及核心架构 ----
-keepnames class android.** { *; }
-keepnames class androidx.activity.** { *; }
-keepnames class androidx.annotation.** { *; }
-keepnames class androidx.appcompat.** { *; }
-keepnames class androidx.arch.** { *; }
-keepnames class androidx.collection.** { *; }
-keepnames class androidx.compose.** { *; }
-keepnames class androidx.concurrent.** { *; }
-keepnames class androidx.coordinatorlayout.** { *; }
-keepnames class androidx.core.** { *; }
-keepnames class androidx.customview.** { *; }
-keepnames class androidx.datastore.** { *; }
-keepnames class androidx.documentfile.** { *; }
-keepnames class androidx.emoji2.** { *; }
-keepnames class androidx.exifinterface.** { *; }
-keepnames class androidx.fragment.** { *; }
-keepnames class androidx.graphics.** { *; }
-keepnames class androidx.interpolator.** { *; }
-keepnames class androidx.lifecycle.** { *; }
-keepnames class androidx.loader.** { *; }
-keepnames class androidx.navigation3.** { *; }
-keepnames class androidx.navigationevent.** { *; }
-keepnames class androidx.profileinstaller.** { *; }
-keepnames class androidx.recyclerview.** { *; }
-keepnames class androidx.room.** { *; }
-keepnames class androidx.room3.** { *; }
-keepnames class androidx.savedstate.** { *; }
-keepnames class androidx.sqlite.** { *; }
-keepnames class androidx.startup.** { *; }
-keepnames class androidx.tracing.** { *; }
-keepnames class androidx.vectordrawable.** { *; }
-keepnames class androidx.versionedparcelable.** { *; }
-keepnames class androidx.window.** { *; }
-keepnames class androidx.work.** { *; }

# ---- 组 2：Google 扩展、Gson、Caffeine 缓存与基础设施 ----
-keepnames class com.google.accompanist.** { *; }
-keepnames class com.google.android.material.** { *; }
-keepnames class com.google.common.** { *; }
-keepnames class com.google.gson.** { *; }
-keepnames class com.github.benmanes.caffeine.** { *; }

# ---- 组 3：Kotlin 核心库、协程与序列化 ----
-keepnames class _COROUTINE.** { *; }
-keepnames class kotlin.** { *; }
-keepnames class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.datetime.** { *; }
-keepnames class kotlinx.io.** { *; }
-keepnames class kotlinx.serialization.** { *; }

# ---- 组 4：网络与图片加载 (Ktor, OkHttp, Coil, FileKit) ----
-keepnames class coil3.** { *; }
-keepnames class io.github.vinceglb.filekit.** { *; }
-keepnames class io.ktor.** { *; }
-keepnames class okhttp3.** { *; }
-keepnames class okio.** { *; }

# ---- 组 5：项目自研/特定业务与第三方工具 (Minecraft/HiveMC/Termux 等) ----
-keepnames class me.voltual.** { *; }            # 看起来是你的核心业务包
-keepnames class com.hivemc.chunker.** { *; }      # Chunker 地图转换库
-keepnames class com.anggrayudi.storage.** { *; }  # 存储权限/文件工具
-keepnames class com.termux.** { *; }              # Termux 终端相关
-keepnames class picocli.** { *; }                 # 命令行解析器

# ---- 组 6：其他底层和辅助依赖库 (Org, Net, It, Ro, Java 兼容层) ----
-keepnames class org.apache.commons.imaging.** { *; }
-keepnames class org.harmony.** { *; }
-keepnames class org.intellij.markdown.** { *; }
-keepnames class org.iq80.leveldb.** { *; }
-keepnames class org.jetbrains.compose.** { *; }
-keepnames class org.koin.** { *; }
-keepnames class org.slf4j.** { *; }
-keepnames class net.jpountz.** { *; }              # LZ4/XXHash 压缩
-keepnames class it.unimi.dsi.fastutil.** { *; }    # 高性能集合
-keepnames class ro.andob.awtcompat.** { *; }       # AWT 兼容层
-keepnames class j$.** { *; }                       # Java 8+ API 脱糖支持
-keepnames class java.awt.** { *; }
-keepnames class javax.imageio.** { *; }