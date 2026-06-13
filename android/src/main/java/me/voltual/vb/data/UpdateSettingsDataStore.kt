// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.voltual.vb.BBQApplication

private val Context.updateSettingsDataStore by preferencesDataStore(name = "update_settings")

object UpdateSettingsDataStore {

  private val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")

  val autoCheckUpdates: Flow<Boolean>
    get() =
      BBQApplication.instance.applicationContext.updateSettingsDataStore.data.map { preferences ->
        preferences[AUTO_CHECK_UPDATES] ?: true
      }

  suspend fun setAutoCheckUpdates(value: Boolean) {
    BBQApplication.instance.applicationContext.updateSettingsDataStore.edit {
      it[AUTO_CHECK_UPDATES] = value
    }
  }
}
