package com.warehouse.pda.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.warehouse.pda.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
import java.io.IOException
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
  private val _sessionExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  val sessionExpiredEvents: SharedFlow<Unit> = _sessionExpiredEvents.asSharedFlow()

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

  fun getPendingSubmission(): PendingSubmission? {
    val json = preferences.getString(KEY_PENDING_SUBMISSION, null) ?: return null
    return runCatching { gson.fromJson(json, PendingSubmission::class.java) }
      .getOrElse {
        clearPendingSubmission()
        null
      }
  }

  suspend fun savePendingSubmission(
    kind: String,
    requestId: String,
    request: Any,
    summary: String
  ): PendingSubmission = withContext(Dispatchers.IO) {
    val pending = PendingSubmission(
      kind = kind,
      requestId = requestId,
      requestJson = gson.toJson(request),
      summary = summary,
      createdAt = System.currentTimeMillis()
    )
    check(preferences.edit().putString(KEY_PENDING_SUBMISSION, gson.toJson(pending)).commit()) {
      "无法保存待确认业务，请释放设备存储空间后重试"
    }
    pending
  }

  fun clearPendingSubmission() {
    preferences.edit().remove(KEY_PENDING_SUBMISSION).apply()
  }

  suspend fun retryPendingSubmission(serverUrl: String, pending: PendingSubmission): SubmissionReceipt {
    return when (pending.kind) {
      PENDING_INBOUND -> submitInbound(serverUrl, gson.fromJson(pending.requestJson, InboundSubmitRequest::class.java)).toReceipt()
      PENDING_OUTBOUND -> submitOutbound(serverUrl, gson.fromJson(pending.requestJson, OutboundSubmitRequest::class.java)).toReceipt()
      PENDING_SALES_RETURN -> submitSalesReturn(serverUrl, gson.fromJson(pending.requestJson, SalesReturnSubmitRequest::class.java)).toReceipt()
      PENDING_TRACKING_OUTBOUND -> submitTrackingOutbound(
        serverUrl,
        gson.fromJson(pending.requestJson, TrackingOutboundSubmitRequest::class.java)
      ).toReceipt()
      PENDING_TRACKING_RETURN -> submitTrackingReturn(
        serverUrl,
        gson.fromJson(pending.requestJson, TrackingReturnSubmitRequest::class.java)
      ).toReceipt()
      else -> throw IllegalArgumentException("无法识别待确认业务，请升级应用后重试")
    }
  }

  suspend fun checkHealth(serverUrl: String): HealthStatus = withContext(Dispatchers.IO) {
    val normalized = normalizeBaseUrl(serverUrl)
    val response = try {
      api(normalized).health()
    } catch (error: IOException) {
      throw NetworkRequestException("无法连接服务器，请检查网络后重试", error)
    } catch (error: RuntimeException) {
      throw ResponseReadException("服务器响应格式不正确", error)
    }
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
    execute(serverUrl, notifySessionExpiration = false) {
      login(LoginRequest(username = username, password = password))
    }

  suspend fun logout(serverUrl: String): LogoutResult =
    execute(serverUrl, notifySessionExpiration = false) { logout() }

  suspend fun getMasterData(serverUrl: String): WarehouseState = execute(serverUrl) { masterData() }

  suspend fun validateBarcodes(
    serverUrl: String,
    request: BarcodeValidationRequest
  ): List<BarcodeValidationResult> = execute(serverUrl) { validateBarcodes(request) }

  suspend fun validateTrackingBarcodes(
    serverUrl: String,
    request: TrackingValidationRequest
  ): List<BarcodeValidationResult> = execute<List<TrackingValidationResult>>(serverUrl) {
    validateTrackingBarcodes(request)
  }.map { result ->
    BarcodeValidationResult(
      barcode = result.barcode,
      ok = result.ok,
      label = result.label,
      detail = result.detail
    )
  }

  suspend fun getInventoryDetail(serverUrl: String, barcode: String): InventoryDetailResult =
    execute(serverUrl) { inventoryDetail(barcode) }

  suspend fun getTrackingDetail(serverUrl: String, barcode: String): TrackingDetailResult =
    execute(serverUrl) { trackingDetail(barcode) }

  suspend fun getPdaReleaseInfo(serverUrl: String): PdaReleaseInfo =
    execute(serverUrl) { pdaReleaseInfo() }

  suspend fun submitInbound(serverUrl: String, request: InboundSubmitRequest): SubmitResult =
    execute(serverUrl) { submitInbound(request) }

  suspend fun submitOutbound(serverUrl: String, request: OutboundSubmitRequest): SubmitResult =
    execute(serverUrl) { submitOutbound(request) }

  suspend fun submitSalesReturn(serverUrl: String, request: SalesReturnSubmitRequest): SubmitResult =
    execute(serverUrl) { submitSalesReturn(request) }

  suspend fun submitTrackingOutbound(
    serverUrl: String,
    request: TrackingOutboundSubmitRequest
  ): TrackingSubmitResult = execute(serverUrl) { submitTrackingOutbound(request) }

  suspend fun submitTrackingReturn(
    serverUrl: String,
    request: TrackingReturnSubmitRequest
  ): TrackingSubmitResult = execute(serverUrl) { submitTrackingReturn(request) }

  private suspend fun <T> execute(
    serverUrl: String,
    notifySessionExpiration: Boolean = true,
    block: suspend WarehouseApi.() -> Response<ApiEnvelope<T>>
  ): T = withContext(Dispatchers.IO) {
    val response = try {
      api(normalizeBaseUrl(serverUrl)).block()
    } catch (error: IOException) {
      throw NetworkRequestException("网络请求未完成，请检查网络后重试", error)
    } catch (error: IllegalArgumentException) {
      throw error
    } catch (error: RuntimeException) {
      throw ResponseReadException("服务器已响应，但处理结果无法确认", error)
    }
    val body = response.body()
    if (response.isSuccessful && body?.data != null) {
      return@withContext body.data
    }

    val errorText = body?.error ?: parseError(response) ?: "请求失败"
    if (response.code() == 401 && notifySessionExpiration) {
      _sessionExpiredEvents.tryEmit(Unit)
    }
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

  companion object {
    private const val SUPPORTED_API_CONTRACT = "1"
    private const val KEY_PENDING_SUBMISSION = "pending_submission"
    const val PENDING_INBOUND = "inbound"
    const val PENDING_OUTBOUND = "outbound"
    const val PENDING_SALES_RETURN = "sales_return"
    const val PENDING_TRACKING_OUTBOUND = "tracking_outbound"
    const val PENDING_TRACKING_RETURN = "tracking_return"
  }
}

private fun SubmitResult.toReceipt() = SubmissionReceipt(
  orderId = orderId,
  quantity = quantity ?: items.size
)

private fun TrackingSubmitResult.toReceipt() = SubmissionReceipt(
  orderId = orderId,
  quantity = quantity
)

class ApiRequestException(
  val statusCode: Int,
  override val message: String
) : IllegalStateException(message)

class NetworkRequestException(
  override val message: String,
  cause: Throwable
) : IOException(message, cause)

class ResponseReadException(
  override val message: String,
  cause: Throwable
) : IllegalStateException(message, cause)

fun shouldRetainPendingSubmission(error: Throwable): Boolean {
  if (error is NetworkRequestException || error is ResponseReadException) return true
  if (error is ApiRequestException) {
    if (error.statusCode == 401 || error.statusCode >= 500) return true
    if (error.statusCode == 409 && error.message.contains("处理中")) return true
  }
  return false
}

fun isSessionExpired(error: Throwable): Boolean {
  return error is ApiRequestException && error.statusCode == 401
}

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

  @POST("api/tracking/validate")
  suspend fun validateTrackingBarcodes(
    @Body body: TrackingValidationRequest
  ): Response<ApiEnvelope<List<TrackingValidationResult>>>

  @GET("api/inventory/{barcode}")
  suspend fun inventoryDetail(
    @retrofit2.http.Path("barcode") barcode: String
  ): Response<ApiEnvelope<InventoryDetailResult>>

  @GET("api/tracking/{barcode}")
  suspend fun trackingDetail(
    @retrofit2.http.Path("barcode") barcode: String
  ): Response<ApiEnvelope<TrackingDetailResult>>

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

  @POST("api/tracking/outbound")
  suspend fun submitTrackingOutbound(
    @Body body: TrackingOutboundSubmitRequest
  ): Response<ApiEnvelope<TrackingSubmitResult>>

  @POST("api/tracking/return")
  suspend fun submitTrackingReturn(
    @Body body: TrackingReturnSubmitRequest
  ): Response<ApiEnvelope<TrackingSubmitResult>>
}
