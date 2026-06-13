// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package me.voltual.vb

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.voltual.vb.core.database.*
import me.voltual.vb.core.ui.theme.ThemeColorStore
import me.voltual.vb.core.ui.theme.ThemeManager
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinApplication
import org.koin.dsl.koinConfiguration

@KoinApplication
class BBQApplication : Application(), KoinStartup {
  val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  lateinit var database: AppDatabase
    private set

  override fun onCreate() {
    super.onCreate()
    instance = this

    // 初始化
    database = AppDatabase.getDatabase(this)
    ThemeManager.initialize(this)
    ThemeManager.customColorSet = ThemeColorStore.loadColors(this)
  }

  override fun onKoinStartup() = koinConfiguration {
    androidContext(this@BBQApplication)
    modules(appModule)
  }

  companion object {
    lateinit var instance: BBQApplication
      private set

    // 暴露给全局类作用域
    val context: Context
      get() = instance
  }
}
