package me.voltual.vb.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
/*
fun createDataStore(
    serializer: UserCredentialsSerializer,
    context: Any?
): DataStore<UserCredentials> {
    val appContext = context as? Context ?: throw IllegalArgumentException("Android 平台需要传入 Context 参数")
    return DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = serializer,
            producePath = {
                appContext.filesDir.resolve("user_credentials.preferences_pb").absolutePath.toPath()
            }
        )
    )
}
*/
fun createPreferenceDataStore(
    fileName: String,
    context: Any?
): DataStore<Preferences> {
    val appContext = context as? Context ?: throw IllegalArgumentException("Android 平台需要传入 Context 参数")
    return PreferenceDataStoreFactory.create(
        produceFile = { 
            // 手动实现 Context.dataStoreFile(fileName) 的底层路径逻辑，确保完全一致
            File(appContext.filesDir, "datastore/$fileName")
        }
    )
}