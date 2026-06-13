// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package me.voltual.vb

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.voltual.vb.data.UpdateInfo

object KtorClient {
  private const val MAX_RETRIES = 3
  private const val RETRY_DELAY = 1000L

  val httpClient =
    HttpClient(OkHttp) {
      install(ContentNegotiation) {
        json(
          Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            encodeDefaults = true // 强制序列化默认值
          }
        )
      }

      defaultRequest {
        header(HttpHeaders.Accept, ContentType.Application.Json.toString())
      }

      install(HttpTimeout) {
        requestTimeoutMillis = 30000
        connectTimeoutMillis = 15000
      }

      install(Logging) { level = LogLevel.INFO }

    }

  // ===== 数据模型 =====

  // ===== API 接口定义 =====

  interface ApiService {
    // 兼容 UpdateChecker.kt
    suspend fun getLatestRelease(url: String): Result<UpdateInfo>
    
  }

  object ApiServiceImpl : ApiService {

    override suspend fun getLatestRelease(url: String): Result<UpdateInfo> {
      return safeApiCall { httpClient.get(url) }
    }

  /** 安全地执行 Ktor 请求 */
  private suspend inline fun <reified T> safeApiCall(block: suspend () -> HttpResponse): Result<T> {
    var attempts = 0
    while (attempts < MAX_RETRIES) {
      try {
        val response = block()
        if (response.status.value in 300..399 && T::class == String::class) {
          // 特殊处理重定向返回
          return Result.success(response as T)
        }
        if (!response.status.isSuccess() && response.status != HttpStatusCode.Found) {
          throw IOException("HTTP Error: ${response.status}")
        }
        return Result.success(response.body())
      } catch (e: Exception) {
        attempts++
        if (attempts >= MAX_RETRIES) return Result.failure(e)
        delay(RETRY_DELAY)
      }
    }
    return Result.failure(IOException("Request failed after retries"))
  }

  fun close() {
    httpClient.close()
  }
}
}
