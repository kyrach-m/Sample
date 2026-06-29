package com.ch.core.network.base

import com.ch.core.network.client.NetworkClient
import com.ch.core.network.error.ConnectivityChecker
import com.ch.core.network.error.DefaultErrorCodeHandler
import com.ch.core.network.error.ErrorCodeHandler
import com.ch.core.network.response.ApiResponse
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * API 基类（商业级增强版）
 *
 * 提供统一的网络请求错误处理、错误码拦截、网络预检和请求取消能力。
 *
 * 用法示例：
 * ```kotlin
 * // Application 中初始化
 * class SampleApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         NetworkClient.init(this)
 *
 *         // 注入网络连通性检查器
 *         BaseApi.setConnectivityChecker { NetworkUtil.isConnected(this) }
 *
 *         // 配置错误处理器
 *         val handler = DefaultErrorCodeHandler()
 *         handler.onTokenExpired = { /* 跳转登录 */ }
 *         handler.onAccountKicked = { /* 弹出提示并跳转登录 */ }
 *         BaseApi.setErrorHandler(handler)
 *     }
 * }
 *
 * // Repository 中使用
 * class UserRepository {
 *     private val api = NetworkClient.create(UserApi::class.java)
 *
 *     suspend fun getUser(): ApiResponse<User> {
 *         return BaseApi.safeRequest { api.getUser() }
 *     }
 * }
 * ```
 */
object BaseApi {

    /**
     * 错误码处理器
     *
     * 默认为 DefaultErrorCodeHandler，处理 401/1001/1002 等常见错误码。
     * 可通过 [setErrorHandler] 注入自定义处理器。
     */
    @Volatile
    private var errorHandler: ErrorCodeHandler = DefaultErrorCodeHandler()

    /**
     * 网络连通性检查器
     *
     * 为 null 时跳过网络预检。
     * 应在 Application 初始化时通过 [setConnectivityChecker] 注入。
     */
    @Volatile
    private var connectivityChecker: ConnectivityChecker? = null

    /**
     * 设置错误码处理器
     *
     * 业务层可通过此方法注入自定义错误处理器，覆盖默认行为。
     *
     * @param handler 错误码处理器
     */
    fun setErrorHandler(handler: ErrorCodeHandler) {
        errorHandler = handler
    }

    /**
     * 设置网络连通性检查器
     *
     * 注入后，safeRequest 执行前会自动检查网络状态。
     * 若网络未连接，直接返回 ApiResponse.Failure(-1, "网络未连接")。
     *
     * 注意：:core:network 不依赖 :core:common，需业务层注入实现。
     * 推荐在 Application 中注入：
     * ```kotlin
     * BaseApi.setConnectivityChecker { NetworkUtil.isConnected(context) }
     * ```
     *
     * @param checker 连通性检查器
     */
    fun setConnectivityChecker(checker: ConnectivityChecker) {
        connectivityChecker = checker
    }

    /**
     * 安全请求封装（商业级增强版）
     *
     * 执行流程：
     * 1. 网络预检：检查网络连通性，未连接直接返回 Failure
     * 2. 执行请求：调用 block 发起网络请求
     * 3. 错误处理：全局捕获异常，转换为 ApiResponse.Failure
     * 4. 错误码拦截：对 Failure 结果调用 errorHandler 处理
     *
     * @param T 数据类型
     * @param block 网络请求挂起函数
     * @return ApiResponse 封装的结果
     */
    suspend fun <T> safeRequest(block: suspend () -> Response<T>): ApiResponse<T> {
        // 第一步：网络预检
        val checker = connectivityChecker
        if (checker != null && !checker.isConnected()) {
            return ApiResponse.Failure(code = -1, msg = "网络未连接")
        }

        // 第二步：执行请求
        val result: ApiResponse<T> = try {
            val response = block()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ApiResponse.Success(body)
                } else {
                    ApiResponse.Failure(
                        code = response.code(),
                        msg = "Response body is null"
                    )
                }
            } else {
                ApiResponse.Failure(
                    code = response.code(),
                    msg = response.errorBody()?.string() ?: response.message()
                )
            }
        } catch (e: HttpException) {
            ApiResponse.Failure(code = e.code(), msg = e.message())
        } catch (e: IOException) {
            ApiResponse.Failure(code = -1, msg = e.message ?: "Network error")
        } catch (e: Exception) {
            ApiResponse.Failure(code = -1, msg = e.message ?: "Unknown error")
        }

        // 第三步：错误码拦截
        if (result is ApiResponse.Failure) {
            val handled = errorHandler.handle(result.code, result.msg)
            if (handled) {
                // 已处理，返回原始结果（上层不再重复处理）
                return result
            }
        }

        return result
    }

    /**
     * 安全请求封装（直接返回数据版本）
     *
     * 与 [safeRequest] 类似，但直接返回数据而非 Response。
     * 包含网络预检和错误码拦截。
     *
     * @param T 数据类型
     * @param block 网络请求挂起函数
     * @return ApiResponse 封装的结果
     */
    suspend fun <T> safeCall(block: suspend () -> T): ApiResponse<T> {
        // 网络预检
        val checker = connectivityChecker
        if (checker != null && !checker.isConnected()) {
            return ApiResponse.Failure(code = -1, msg = "网络未连接")
        }

        val result: ApiResponse<T> = try {
            ApiResponse.Success(block())
        } catch (e: HttpException) {
            ApiResponse.Failure(code = e.code(), msg = e.message())
        } catch (e: IOException) {
            ApiResponse.Failure(code = -1, msg = e.message ?: "Network error")
        } catch (e: Exception) {
            ApiResponse.Failure(code = -1, msg = e.message ?: "Unknown error")
        }

        // 错误码拦截
        if (result is ApiResponse.Failure) {
            errorHandler.handle(result.code, result.msg)
        }

        return result
    }

    /**
     * 取消所有网络请求
     *
     * 取消 OkHttpClient 中所有正在执行的请求。
     * 通常在 ViewModel.onCleared() 中调用，避免内存泄漏。
     *
     * 用法示例：
     * ```kotlin
     * class HomeViewModel : BaseViewModel<HomeState, HomeEvent>(savedStateHandle) {
     *     override fun onCleared() {
     *         super.onCleared()
     *         BaseApi.cancelAllRequests()
     *     }
     * }
     * ```
     */
    fun cancelAllRequests() {
        NetworkClient.getOkHttpClient().dispatcher.cancelAll()
    }

    /**
     * 取消指定 tag 的网络请求
     *
     * 通过 Request.tag 标记的请求可以被精确取消。
     *
     * @param tag 请求标签对象
     */
    fun cancelRequestsByTag(tag: Any) {
        NetworkClient.getOkHttpClient().dispatcher.runningCalls().forEach { call ->
            if (call.request().tag() == tag) {
                call.cancel()
            }
        }
        NetworkClient.getOkHttpClient().dispatcher.queuedCalls().forEach { call ->
            if (call.request().tag() == tag) {
                call.cancel()
            }
        }
    }
}

/**
 * Response 扩展函数：安全获取数据
 *
 * 将 Response 转换为 ApiResponse，处理异常情况。
 *
 * @param T 数据类型
 * @return ApiResponse 封装的结果
 */
suspend fun <T> Response<T>.toApiResponse(): ApiResponse<T> {
    return if (isSuccessful) {
        val body = body()
        if (body != null) {
            ApiResponse.Success(body)
        } else {
            ApiResponse.Failure(code(), "Response body is null")
        }
    } else {
        ApiResponse.Failure(code(), errorBody()?.string() ?: message())
    }
}
