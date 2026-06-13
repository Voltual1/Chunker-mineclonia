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
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

// 定义当前的协议版本号
object AgreementVersions {
  const val USER_AGREEMENT = 1
}

private val Context.userAgreementDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "user_agreement_prefs")

@Single
class UserAgreementDataStore(context: Context) {

  private val dataStore = context.userAgreementDataStore

  private object Keys {
    val USER_AGREEMENT_VER = intPreferencesKey("user_agreement_ver")
  }

  // --- 通用判断逻辑 ---
  private fun isAccepted(key: Preferences.Key<Int>, currentVersion: Int): Flow<Boolean> =
    dataStore.data.map { prefs -> (prefs[key] ?: 0) >= currentVersion }

  // --- 对外暴露的 Flow ---
  val isUserAgreementAccepted =
    isAccepted(Keys.USER_AGREEMENT_VER, AgreementVersions.USER_AGREEMENT)

  // --- 写入方法 ---
  suspend fun acceptUserAgreement() =
    saveVersion(Keys.USER_AGREEMENT_VER, AgreementVersions.USER_AGREEMENT)

  private suspend fun saveVersion(key: Preferences.Key<Int>, version: Int) {
    dataStore.edit { it[key] = version }
  }
}
