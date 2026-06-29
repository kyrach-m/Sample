package com.ch.service.crash

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.ch.core.common.logger.Logger
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * 崩溃恢复页面 Activity
 *
 * 当应用发生崩溃后，由 [CrashHandler] 启动。
 * 显示"应用正在恢复，请稍候..."的提示界面，
 * 同时展示脱敏后的错误摘要，为用户提供"反馈错误"的入口。
 *
 * 设计说明：
 * - 运行在独立进程（:recovery），确保主进程崩溃后仍可显示
 * - 使用纯代码构建 UI（不依赖布局文件），避免资源加载失败
 * - 界面包含：ProgressBar + 提示文字 + 错误摘要 + 反馈按钮
 * - 不可返回（禁用返回键）
 * - 2 秒后自动杀死进程，系统会自动回收资源
 *
 * 接收参数：
 * - [EXTRA_ERROR_SUMMARY]：脱敏后的错误摘要字符串
 *
 * 用户体验：
 * - 崩溃后不会显示系统强制关闭对话框
 * - 用户看到的是友好的恢复提示 + 错误摘要
 * - 可点击"反馈错误"按钮（当前为 Toast 提示，业务层可对接反馈系统）
 * - 2 秒后进程被杀死，用户回到桌面或上一次使用的页面
 */
@SuppressLint("CustomSplashScreen")
class RecoveryActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RecoveryActivity"

        /**
         * 恢复页面显示时间（毫秒）
         */
        private const val RECOVERY_DISPLAY_MS = 2000L

        /**
         * Intent 参数：错误摘要
         */
        const val EXTRA_ERROR_SUMMARY = "extra_error_summary"
    }

    /**
     * 定时处理器
     */
    private val handler = Handler(Looper.getMainLooper())

    /**
     * 错误摘要文本
     */
    private var errorSummary: String = ""

    /**
     * 延迟杀死进程的任务
     */
    private val killProcessRunnable = Runnable {
        Logger.d(TAG, "恢复时间结束，杀死进程")
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Logger.d(TAG, "RecoveryActivity 创建（进程: ${android.os.Process.myPid()}）")

        // 接收错误摘要
        errorSummary = intent?.getStringExtra(EXTRA_ERROR_SUMMARY) ?: ""

        // 纯代码构建 UI
        buildRecoveryUI()

        // 延迟 2 秒后杀死进程
        handler.postDelayed(killProcessRunnable, RECOVERY_DISPLAY_MS)
    }

    /**
     * 构建恢复页面 UI
     *
     * 使用纯代码创建布局：
     * - LinearLayout（垂直）作为根布局
     * - 居中显示 ProgressBar + 提示文字 + 错误摘要 + 反馈按钮
     */
    private fun buildRecoveryUI() {
        // 根布局（垂直线性布局）
        val rootLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFFFAFAFA.toInt())
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        // 进度条
        val progressBar = ProgressBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }

        // 主提示文字
        val mainText = TextView(this).apply {
            text = "应用正在恢复，请稍候..."
            textSize = 18f
            setTextColor(0xFF333333.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 32
            }
        }

        // 副提示文字
        val subText = TextView(this).apply {
            text = "应用即将自动重启"
            textSize = 14f
            setTextColor(0xFF999999.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 12
            }
        }

        // 错误摘要区域（如果有）
        val errorLayout = if (errorSummary.isNotEmpty()) {
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 48
                }
                setBackgroundColor(0xFFF5F5F5.toInt())
                setPadding(24, 16, 24, 16)

                // 标题
                val title = TextView(this@RecoveryActivity).apply {
                    text = "错误信息："
                    textSize = 13f
                    setTextColor(0xFF666666.toInt())
                }
                addView(title)

                // 错误摘要（可滚动）
                val errorText = TextView(this@RecoveryActivity).apply {
                    text = errorSummary
                    textSize = 12f
                    setTextColor(0xFFCC3333.toInt())
                    maxLines = 5
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 8
                    }
                }
                addView(errorText)
            }
        } else null

        // 反馈按钮
        val feedbackButton = Button(this).apply {
            text = "反馈错误"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF4CAF50.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 32
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setOnClickListener {
                // 业务层可在此对接反馈系统
                // 当前仅做 Toast 提示
                android.widget.Toast.makeText(
                    this@RecoveryActivity,
                    "感谢您的反馈，我们会尽快修复此问题",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        // 组装布局
        rootLayout.addView(progressBar)
        rootLayout.addView(mainText)
        rootLayout.addView(subText)
        errorLayout?.let { rootLayout.addView(it) }
        rootLayout.addView(feedbackButton)

        setContentView(rootLayout)
    }

    /**
     * 禁用返回键
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 不执行任何操作，禁用返回键
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(killProcessRunnable)
    }
}
