package com.warehouse.pda.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.warehouse.pda.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.net.CookieManager
import java.net.CookiePolicy

class WarehouseRepository(context: Context) {
  private val preferences = context.getSharedPreferences("warehouse_pda_config", Context.MODE_PRIVATE)
  private val gson = Gson()
  private val credentialStore = SecureCredentialStore(preferences, gson)
  private val cookieManager = CookieManager().apply {
    setCookiePolicy(CookiePolicy.ACCEPT_ALL)
  }
  private val okHttpClient = OkHttpClient.Builder()
    .cookieJar(JavaNetCookieJar(cookieManager))
    .addInterceptor(
      HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
      }
    )
    .build()

  @Volatile
  private var currentBaseUrl = ""

  @Volatile
  private var api: WarehouseApi? = null

  fun getSavedServerUrl(): String {
    return preferences.getString("server_url", BuildConfig.DEFAULT_SERVER_URL) ?: BuildConfig.DEFAULT_SERVER_URL
  }

  fun saveServerUrl(serverUrl: String) {
    preferences.edit().putString("server_url", normalizeBaseUrl(serverUrl)).apply()
  }

  fun getRememberCredentials(): Boolean {
    return preferences.getBoolean("remember_credentials", true)
  }

  fun saveRememberCredentials(enabled: Boolean) {
    preferences.edit().putBoolean("remember_credentials", enabled).apply()
  }

  fun getSavedUsername(): String {
    return credentialStore.load()?.username.orEmpty()
  }

  fun getSavedPassword(): String {
    return credentialStore.load()?.password.orEmpty()
  }

  fun saveCredentials(username: String, password: String): Boolean {
    return credentialStore.save(SavedCredentials(username = username, password = password))
  }

  fun clearCredentials() {
    credentialStore.clear()
  }

  suspend fun checkHealth(serverUrl: String): HealthStatus = withContext(Dispatchers.IO) {
    val normalized = normalizeBaseUrl(serverUrl)
    val response = api(normalized).health()
    val health = response.body()?.data
    if (health == null) {
      throw ApiRequestException(response.code(), parseError(response) ?: "无法读取服务器健康状态")
    }
    if (health.status != "ok" || health.database != "ok") {
      throw ApiRequestException(response.code(), "服务器数据库暂不可用")
    }
    if (health.apiContractVersion != SUPPORTED_API_CONTRACT) {
      throw ApiRequestException(
        response.code(),
        "服务器接口版本 ${health.apiContractVersion} 与当前 PDA 不兼容"
      )
    }
    health
  }

  suspend fun getCurrentUser(serverUrl: String): CurrentUser = execute(serverUrl) { me() }

  suspend fun login(serverUrl: String, username: String, password: String): CurrentUser =
    execute(serverUrl) { login(LoginRequest(username = username, password = password)) }

  suspend fun logout(serverUrl: String): LogoutResult = execute(serverUrl) { logout() }

  suspend fun getMasterData(serverUrl: String): WarehouseState = execute(serverUrl) { masterData() }

  suspend fun validateBarcodes(
    serverUrl: String,
    request: BarcodeValidationRequest
  ): List<BarcodeValidationResult> = execute(serverUrl) { validateBarcodes(request) }

  suspend fun getInventoryDetail(serverUrl: String, barcode: String): InventoryDetailResult =
    execute(serverUrl) { inventoryDetail(barcode) }

  suspend fun getPdaReleaseInfo(serverUrl: String): PdaReleaseInfo =
    execute(serverUrl) { pdaReleaseInfo() }

  suspend fun submitInbound(serverUrl: String, request: InboundSubmitRequest): SubmitResult =
    execute(serverUrl) { submitInbound(request) }

  suspend fun submitOutbound(serverUrl: String, request: OutboundSubmitRequest): SubmitResult =
    execute(serverUrl) { submitOutbound(request) }

  suspend fun submitSalesReturn(serverUrl: String, request: SalesReturnSubmitRequest): SubmitResult =
    execute(serverUrl) { submitSalesReturn(request) }

  private suspend fun <T> execute(
    serverUrl: String,
    block: suspend WarehouseApi.() -> Response<ApiEnvelope<T>>
  ): T = withContext(Dispatchers.IO) {
    val response = api(normalizeBaseUrl(serverUrl)).block()
    val body = response.body()
    if (response.isSuccessful && body?.data != null) {
      return@withContext body.data
    }

    val errorText = body?.error ?: parseError(response) ?: "请求失败"
    throw ApiRequestException(response.code(), errorText)
  }

  private fun api(serverUrl: String): WarehouseApi {
    if (serverUrl != currentBaseUrl || api == null) {
      currentBaseUrl = serverUrl
      api = Retrofit.Builder()
        .baseUrl(serverUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(WarehouseApi::class.java)
    }

    return api!!
  }

  private fun parseError(response: Response<*>): String? {
    val body = response.errorBody()?.string() ?: return null
    return try {
      val type = object : TypeToken<ApiEnvelope<Any>>() {}.type
      gson.fromJson<ApiEnvelope<Any>>(body, type).error
    } catch (_: Exception) {
      null
    }
  }

  private fun normalizeBaseUrl(serverUrl: String): String {
    val trimmed = serverUrl.trim()
    val candidate = if (trimmed.isEmpty()) BuildConfig.DEFAULT_SERVER_URL else trimmed
    val normalized = if (candidate.endsWith("/")) candidate else "$candidate/"
    val parsed = normalized.toHttpUrlOrNull()
      ?: throw IllegalArgumentException("服务器地址格式不正确")
    if (parsed.scheme != "http" && parsed.scheme != "https") {
      throw IllegalArgumentException("服务器地址仅支持 http 或 https")
    }
    return parsed.toString()
  }

  private companion object {
    const val SUPPORTED_API_CONTRACT = "1"
  }
}

class ApiRequestException(
  val statusCode: Int,
  override val message: String
) : IllegalStateException(message)

private interface WarehouseApi {
  @GET("api/health")
  suspend fun health(): Response<ApiEnvelope<HealthStatus>>

  @POST("api/auth/login")
  suspend fun login(@Body body: LoginRequest): Response<ApiEnvelope<CurrentUser>>

  @POST("api/auth/logout")
  suspend fun logout(): Response<ApiEnvelope<LogoutResult>>

  @GET("api/auth/me")
  suspend fun me(): Response<ApiEnvelope<CurrentUser>>

  @GET("api/master-data")
  suspend fun masterData(): Response<ApiEnvelope<WarehouseState>>

  @POST("api/barcodes/validate")
  suspend fun validateBarcodes(
    @Body body: BarcodeValidationRequest
  ): Response<ApiEnvelope<List<BarcodeValidationResult>>>

  @GET("api/inventory/{barcode}")
  suspend fun inventoryDetail(
    @retrofit2.http.Path("barcode") barcode: String
  ): Response<ApiEnvelope<InventoryDetailResult>>

  @GET("api/pda-release")
  suspend fun pdaReleaseInfo(): Response<ApiEnvelope<PdaReleaseInfo>>

  @POST("api/inbound")
  suspend fun submitInbound(
    @Body body: InboundSubmitRequest
  ): Response<ApiEnvelope<SubmitResult>>

  @POST("api/outbound")
  suspend fun submitOutbound(
    @Body body: OutboundSubmitRequest
  ): Response<ApiEnvelope<SubmitResult>>

  @POST("api/sales-return")
  suspend fun submitSalesReturn(
    @Body body: SalesReturnSubmitRequest
  ): Response<ApiEnvelope<SubmitResult>>
}
