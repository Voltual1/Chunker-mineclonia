package me.voltual.vb.data.model

import android.os.Parcelable
import android.util.Base64
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ConversionManifest(
    val worldId: String,
    val inputPath: String,
    val outputPath: String,
    val format: String,
    val progressIndex: Int,
    val lastBedrockKeyBase64: String? = null,
    val isActive: Boolean = false
) : Parcelable {
    fun getLastBedrockKey(): ByteArray? {
        return if (lastBedrockKeyBase64 != null) {
            Base64.decode(lastBedrockKeyBase64, Base64.NO_WRAP)
        } else null
    }

    fun withLastBedrockKey(key: ByteArray?): ConversionManifest {
        val base64Key = if (key != null) Base64.encodeToString(key, Base64.NO_WRAP) else null
        return this.copy(lastBedrockKeyBase64 = base64Key)
    }
}