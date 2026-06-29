package com.ch.sample

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ch.core.common.logger.Logger
import com.ch.sample.databinding.ActivityDashboardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Dashboard Activity
 *
 * 框架能力展示页，直观展示当前框架所有核心组件的运行状态。
 */
class MainActivity : AppCompatActivity() {

    private var _binding: ActivityDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var logCount = 0

    companion object {
        private const val TAG = "Dashboard"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = DashboardViewModel()

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        initViews()
        observeState()
        observeEvents()

        viewModel.loadDashboardData(this)
    }

    override fun onResume() {
        super.onResume()
        registerNetworkCallback()
    }

    override fun onPause() {
        super.onPause()
        unregisterNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                viewModel.updateNetworkStatus(true)
                runOnUiThread {
                    updateNetworkUI(true)
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                viewModel.updateNetworkStatus(false)
                runOnUiThread {
                    updateNetworkUI(false)
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val isConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                viewModel.updateNetworkStatus(isConnected)
                runOnUiThread {
                    updateNetworkUI(isConnected)
                }
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }

    private fun initViews() {
        binding.btnSimulateCrash.setOnClickListener {
            viewModel.simulateCrash()
        }

        binding.btnClearCache.setOnClickListener {
            viewModel.clearCache()
        }

        binding.btnPrintRoutes.setOnClickListener {
            viewModel.printRoutes()
        }

        binding.btnTestNetwork.setOnClickListener {
            viewModel.testNetwork(this)
        }

        binding.btnClearLogs.setOnClickListener {
            binding.tvLogs.text = ""
            logCount = 0
            binding.tvLogCount.text = "0 条"
            viewModel.addLog(TAG, "日志已清空")
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    state?.let { updateUI(it) }
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collectLatest { event ->
                    when (event) {
                        is DashboardEvent.AppendLog -> appendLog(event.message)
                        is DashboardEvent.ShowMessage -> showToast(event.message)
                    }
                }
            }
        }
    }

    private fun updateUI(state: DashboardState) {
        binding.tvVersion.text = "v${state.appVersion}"
        binding.tvBuildType.text = state.buildType.uppercase()

        binding.tvDeviceId.text = state.deviceId
        binding.tvScreenSize.text = state.screenSize
        binding.tvSystemVersion.text = state.systemVersion

        binding.tvProcessCreateTime.text = "${state.processCreateTime}ms"
        binding.tvAppCreateTime.text = "${state.appCreateTime}ms"
        binding.tvTaskExecutionTime.text = "${state.taskExecutionTime}ms"
        binding.tvTotalTime.text = "${state.totalTime}ms"
        binding.tvMemoryUsage.text = state.memoryUsage

        updateNetworkUI(state.isNetworkConnected)

        updateStatusCard(
            tvStatus = binding.tvStorageStatus,
            ivIndicator = binding.ivStorageIndicator,
            status = state.isStorageReady,
            onlineText = "MMKV 就绪",
            offlineText = "未初始化"
        )

        updateStatusCard(
            tvStatus = binding.tvRouterStatus,
            ivIndicator = binding.ivRouterIndicator,
            status = state.isRouterReady,
            onlineText = "已注册 ${state.routeCount} 条路由",
            offlineText = "路由未初始化"
        )

        updateStatusCard(
            tvStatus = binding.tvLoggerStatus,
            ivIndicator = binding.ivLoggerIndicator,
            status = state.isLoggerReady,
            onlineText = "日志系统运行中",
            offlineText = "日志未初始化"
        )

        updateStatusCard(
            tvStatus = binding.tvCrashStatus,
            ivIndicator = binding.ivCrashIndicator,
            status = state.isCrashHandlerReady,
            onlineText = "全局崩溃捕获已启用",
            offlineText = "崩溃捕获未启用"
        )
    }

    private fun updateNetworkUI(isConnected: Boolean) {
        val networkType = if (isConnected) {
            val capabilities = connectivityManager.activeNetwork?.let {
                connectivityManager.getNetworkCapabilities(it)
            }
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                "WiFi"
            } else if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
                "移动数据"
            } else {
                "网络"
            }
        } else {
            ""
        }

        val onlineText = if (isConnected && networkType.isNotEmpty()) {
            "已连接 ($networkType)"
        } else if (isConnected) {
            "已连接"
        } else {
            "未连接"
        }

        updateStatusCard(
            tvStatus = binding.tvNetworkStatus,
            ivIndicator = binding.ivNetworkIndicator,
            status = isConnected,
            onlineText = onlineText,
            offlineText = "未连接"
        )
    }

    private fun updateStatusCard(
        tvStatus: android.widget.TextView,
        ivIndicator: android.widget.ImageView,
        status: Boolean,
        onlineText: String,
        offlineText: String
    ) {
        if (status) {
            tvStatus.text = onlineText
            tvStatus.setTextColor(getColor(com.ch.sample.R.color.colorOnSurfaceVariant))
            ivIndicator.setImageResource(com.ch.sample.R.drawable.ic_status_online)
        } else {
            tvStatus.text = offlineText
            tvStatus.setTextColor(getColor(com.ch.sample.R.color.colorError))
            ivIndicator.setImageResource(com.ch.sample.R.drawable.ic_status_offline)
        }
    }

    private fun appendLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logLine = "[$timestamp] $message\n"
        binding.tvLogs.append(logLine)

        logCount++
        binding.tvLogCount.text = "$logCount 条"

        binding.svLogs.post {
            binding.svLogs.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterNetworkCallback()
        _binding = null
    }
}
