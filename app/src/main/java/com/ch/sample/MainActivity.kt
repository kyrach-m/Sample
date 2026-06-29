package com.ch.sample

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ch.core.base.BaseComposeActivity
import com.ch.core.base.navigation.AppNavHost
import com.ch.core.base.navigation.AppNavigator
import com.ch.core.base.navigation.Screen
import com.ch.sample.components.ComponentsScreen
import com.ch.sample.dashboard.DashboardEvent
import com.ch.sample.dashboard.DashboardScreen
import com.ch.sample.dashboard.DashboardState
import com.ch.sample.dashboard.DashboardViewModel
import com.ch.sample.login.LoginScreen

/**
 * 主 Activity（Compose 版本）
 *
 * 框架能力展示页，直观展示当前框架所有核心组件的运行状态。
 * 使用 Jetpack Compose 实现 UI，通过 Navigation Compose 管理页面导航。
 *
 * 功能特性：
 * - Compose 声明式 UI
 * - Navigation Compose 页面导航
 * - 沉浸式状态栏
 * - 深色/浅色主题切换
 * - 网络状态监听
 */
class MainActivity : BaseComposeActivity<DashboardState, DashboardEvent, DashboardViewModel>() {

    override val viewModel: DashboardViewModel by viewModels()

    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        private const val TAG = "Dashboard"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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

    @androidx.compose.runtime.Composable
    override fun ScreenContent() {
        val navController = rememberNavController()
        val navigator = remember { AppNavigator(navController) }
        val context = LocalContext.current

        AppNavHost(
            navController = navController,
            startDestination = Screen.DASHBOARD,
            dashboardScreen = {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToComponents = { navigator.navigateToComponents() }
                )
            },
            componentsScreen = {
                ComponentsScreen()
            },
            loginScreen = {
                LoginScreen(
                    onLoginSuccess = { navigator.navigateToDashboard() }
                )
            }
        )
    }

    override fun handleEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.AppendLog -> {
            }
            is DashboardEvent.ShowMessage -> {
                showSnackbar(event.message)
            }
        }
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                viewModel.updateNetworkStatus(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                viewModel.updateNetworkStatus(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val isConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                viewModel.updateNetworkStatus(isConnected)
            }
        }

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        } catch (e: Exception) {
        }
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
            }
            networkCallback = null
        }
    }
}
