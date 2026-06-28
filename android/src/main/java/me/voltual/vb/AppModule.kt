package me.voltual.vb

import me.voltual.vb.core.database.*
import me.voltual.vb.core.database.dao.*
import me.voltual.vb.data.*
import me.voltual.vb.ui.settings.update.*
import me.voltual.vb.ui.settings.cache.*
import me.voltual.vb.ui.settings.conversion.*
import me.voltual.vb.ui.log.LogViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import me.voltual.vb.core.database.repository.*
import org.koin.dsl.module
import me.voltual.vb.core.ui.theme.*
import org.koin.core.qualifier.named
import me.voltual.vb.ui.home.HomeViewModel
import androidx.datastore.core.DataStore
import me.voltual.vb.ui.terminal.TerminalViewModel
import me.voltual.vb.ui.export.ExportViewModel
import androidx.datastore.preferences.core.Preferences
import me.voltual.vb.core.ftp.FtpServerManager

val USER_AGREEMENT_STORE_QUALIFIER = named("user_agreement_store")
val UPDATE_SETTINGS_STORE_QUALIFIER = named("update_settings_store")
val DRAWER_MENU_STORE_QUALIFIER = named("drawer_menu_store")
val THEME_SETTINGS_STORE_QUALIFIER = named("theme_settings_store")
val CONVERSION_SETTINGS_STORE_QUALIFIER = named("conversion_settings_store")

val appModule = module {
    viewModel { UpdateSettingsViewModel(get()) }
    viewModel { HomeViewModel() }
    viewModel { TerminalViewModel(androidContext(), get()) }
    viewModel { LogViewModel(get()) }
    viewModel { CacheSettingsViewModel(androidContext()) }
    viewModel { ExportViewModel(androidContext()) }
    viewModel { ConversionSettingsViewModel(get()) }
   
    single { BBQApplication.instance.database }
    single { get<AppDatabase>().logDao() }
    single { LogRepository(get()) }
      
    single { UserAgreementDataStore(get(USER_AGREEMENT_STORE_QUALIFIER)) }
    single { UpdateSettingsDataStore(get(UPDATE_SETTINGS_STORE_QUALIFIER)) }
    single { ThemeColorDataStore(get(THEME_SETTINGS_STORE_QUALIFIER)) }
    single { DrawerMenuDataStore(get(DRAWER_MENU_STORE_QUALIFIER)) }        
    single { FtpSettingsDataStore(androidContext()) }
    single { FtpServerManager(androidContext(), get()) }
    single { ConversionSettingsDataStore(get(CONVERSION_SETTINGS_STORE_QUALIFIER)) }
    
    val storeFiles = mapOf(
        USER_AGREEMENT_STORE_QUALIFIER to "user_agreement_prefs.preferences_pb",
        UPDATE_SETTINGS_STORE_QUALIFIER to "update_settings.preferences_pb",
        DRAWER_MENU_STORE_QUALIFIER to "settings.preferences_pb",
        THEME_SETTINGS_STORE_QUALIFIER to "theme_settings.preferences_pb",
        CONVERSION_SETTINGS_STORE_QUALIFIER to "conversion_settings_prefs.preferences_pb"
    )

    storeFiles.forEach { (qualifier, fileName) ->
        single<DataStore<Preferences>>(qualifier) {
            createPreferenceDataStore(fileName, androidContext()) 
        }
    }
}