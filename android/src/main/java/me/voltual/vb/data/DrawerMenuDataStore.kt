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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object DrawerMenuDataStore {

  private val MENU_ORDER_KEY = stringPreferencesKey("drawer_menu_order")

  /**
   * 加载菜单顺序
   *
   * @param context Context
   * @return Flow<List<String>> 菜单项ID的列表流
   */
  fun loadMenuOrder(context: Context): Flow<List<String>> {
    return context.dataStore.data.map { preferences ->
      val orderString = preferences[MENU_ORDER_KEY]
      if (orderString.isNullOrBlank()) {
        emptyList()
      } else {
        orderString.split(",")
      }
    }
  }

  /**
   * 保存菜单顺序
   *
   * @param context Context
   * @param order 菜单项ID的列表
   */
  suspend fun saveMenuOrder(context: Context, order: List<String>) {
    context.dataStore.edit { settings -> settings[MENU_ORDER_KEY] = order.joinToString(",") }
  }
}
