// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb

import me.voltual.vb.core.database.*
import me.voltual.vb.core.database.dao.*
import me.voltual.vb.data.*
import me.voltual.vb.ui.settings.update.*
import me.voltual.vb.ui.log.LogViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import me.voltual.vb.core.database.repository.*
import org.koin.dsl.module
import me.voltual.vb.core.ui.theme.*
import org.koin.core.qualifier.named
import androidx.datastore.core.DataStore
import me.voltual.vb.ui.terminal.TerminalViewModel
import androidx.datastore.preferences.core.Preferences

val USER_AGREEMENT_STORE_QUALIFIER = named("user_agreement_store")
val UPDATE_SETTINGS_STORE_QUALIFIER = named("update_settings_store")
val DRAWER_MENU_STORE_QUALIFIER = named("drawer_menu_store")
val THEME_SETTINGS_STORE_QUALIFIER = named("theme_settings_store")

val appModule = module {
      viewModel { UpdateSettingsViewModel(get()) }
      viewModel { TerminalViewModel(androidContext()) }
          viewModel { LogViewModel(get()) }
   
  single { BBQApplication.instance.database }
  single { get<AppDatabase>().logDao() }
      single { LogRepository(get()) }
      
          single { UserAgreementDataStore(get(USER_AGREEMENT_STORE_QUALIFIER)) }
              single { UpdateSettingsDataStore(get(UPDATE_SETTINGS_STORE_QUALIFIER)) }
                  single { ThemeColorDataStore(get(THEME_SETTINGS_STORE_QUALIFIER)) }
        single { DrawerMenuDataStore(get(DRAWER_MENU_STORE_QUALIFIER)) }        
    
    // 1. 先把基础的 DataStore 实例批量生产并贴上标签（Qualifier）
    val storeFiles = mapOf(
        USER_AGREEMENT_STORE_QUALIFIER to "user_agreement_prefs.preferences_pb",
        UPDATE_SETTINGS_STORE_QUALIFIER to "update_settings.preferences_pb",
        DRAWER_MENU_STORE_QUALIFIER to "settings.preferences_pb",
        THEME_SETTINGS_STORE_QUALIFIER to "theme_settings.preferences_pb"
    )

    storeFiles.forEach { (qualifier, fileName) ->
        single<DataStore<Preferences>>(qualifier) {
            createPreferenceDataStore(fileName, androidContext()) 
        }
    }
}