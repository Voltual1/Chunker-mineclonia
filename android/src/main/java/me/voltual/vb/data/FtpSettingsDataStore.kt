package me.voltual.vb.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.tink.AeadSerializer
import androidx.datastore.core.DataStoreFactory
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@Serializable
data class FtpSettings(
    val port: Int = (20000..30000).random(),
    val username: String = "admin",
    val password: String = UUID.randomUUID().toString().substring(0, 4),
    val isRunning: Boolean = false
)

object FtpSettingsSerializer : Serializer<FtpSettings> {
    override val defaultValue: FtpSettings
        get() = FtpSettings()

    override suspend fun readFrom(input: InputStream): FtpSettings {
        return try {
            val bytes = input.readBytes()
            Json.decodeFromString(FtpSettings.serializer(), bytes.decodeToString())
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: FtpSettings, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(Json.encodeToString(FtpSettings.serializer(), t).encodeToByteArray())
        }
    }
}

class FtpSettingsDataStore(private val context: Context) {
    private val dataStore: DataStore<FtpSettings>

    init {
        // 注册 Tink 算法支持
        AeadConfig.register()

        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "ftp_keyset", "ftp_keyset_prefs")
            .withKeyTemplate(KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM))
            .withMasterKeyUri("android-keystore://ftp_master_key")
            .build()
            .keysetHandle

        val aead = keysetHandle.getPrimitive(Aead::class.java)

        val aeadSerializer = AeadSerializer(
            aead = aead,
            wrappedSerializer = FtpSettingsSerializer,
            associatedData = "ftp_settings.json".encodeToByteArray()
        )

        dataStore = DataStoreFactory.create(
            serializer = aeadSerializer,
            produceFile = { File(context.filesDir, "datastore/ftp_settings.json") }
        )
    }

    val ftpSettingsFlow: Flow<FtpSettings> = dataStore.data

    suspend fun updateSettings(transform: (FtpSettings) -> FtpSettings) {
        dataStore.updateData { current ->
            transform(current)
        }
    }
}