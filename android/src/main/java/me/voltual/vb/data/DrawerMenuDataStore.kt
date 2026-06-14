//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class DrawerMenuDataStore(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val MENU_ORDER_KEY = stringPreferencesKey("drawer_menu_order")
    }

    fun loadMenuOrder(): Flow<List<String>> {
        return dataStore.data.map { preferences ->
            val orderString = preferences[MENU_ORDER_KEY]
            if (orderString.isNullOrBlank()) {
                emptyList()
            } else {
                orderString.split(",")
            }
        }
    }

    suspend fun saveMenuOrder(order: List<String>) {
        dataStore.edit { settings ->
            settings[MENU_ORDER_KEY] = order.joinToString(",")
        }
    }
}