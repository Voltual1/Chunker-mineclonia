// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb.core.database

import android.content.Context
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import me.voltual.vb.core.database.dao.*
import me.voltual.vb.core.database.entity.*
import me.voltual.vb.core.database.repository.*

@Database(entities = [LogEntry::class], version = 1, exportSchema = false)
// 1. 使用 @ConstructedBy 关联 Room 3 的构造器
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun logDao(): LogDao

  companion object {
    @Volatile private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE
        ?: synchronized(this) {
          // 2. Room 3 的 databaseBuilder 变更为了单参数泛型形式，不再需要传入 AppDatabase::class.java
          val instance =
            Room.databaseBuilder<AppDatabase>(
                context = context.applicationContext,
                name = "app_database",
              )
              .build()
          INSTANCE = instance
          instance
        }
    }
  }
}

// 3. 声明 Room 3 必需的数据库构造器接口
// 既然你明确表示只支持 Android，这里无需写 expect/actual，直接用普通的 object 实现接口即可
// 这样既符合 Room 3 编译器的要求，又免去了多平台配置的繁琐
@Suppress("KotlinNoActualForExpect")
object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}