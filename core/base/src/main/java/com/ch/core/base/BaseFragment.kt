package com.ch.core.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.ch.core.base.event.ViewEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment 基类（商业级增强版）
 *
 * 统一 ViewBinding 初始化、ViewModel 状态观察和事件订阅。
 * 自动处理 ViewBinding 生命周期，避免内存泄漏。
 *
 * 用法示例：
 * ```kotlin
 * class HomeFragment : BaseFragment<FragmentHomeBinding, HomeState, HomeEvent, HomeViewModel>() {
 *
 *     override val viewModel: HomeViewModel by viewModel()
 *
 *     override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
 *         return FragmentHomeBinding.inflate(inflater, container, false)
 *     }
 *
 *     override fun initView(savedInstanceState: Bundle?) {
 *         binding.tvTitle.text = "Home"
 *     }
 *
 *     override fun renderState(state: HomeState) {
 *         binding.tvContent.text = state.content
 *     }
 *
 *     override fun handleEvent(event: HomeEvent) {
 *         when (event) {
 *             is HomeEvent.ShowToast -> Toast.makeText(requireContext(), event.msg, Toast.LENGTH_SHORT).show()
 *             else -> {}
 *         }
 *     }
 * }
 * ```
 *
 * @param VB ViewBinding 类型
 * @param State 页面状态类型
 * @param Event 事件类型
 * @param VM ViewModel 类型
 */
abstract class BaseFragment<
    VB : ViewBinding,
    State,
    Event : ViewEvent,
    VM : BaseViewModel<State, Event>
> : Fragment() {

    /**
     * 内部 ViewBinding 引用
     *
     * 在 onCreateView 和 onDestroyView 之间有效
     */
    private var _binding: VB? = null

    /**
     * ViewBinding 实例
     *
     * 仅在 onCreateView 和 onDestroyView 之间可访问
     *
     * @throws IllegalStateException 如果在生命周期外访问
     */
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException(
            "ViewBinding is only valid between onCreateView and onDestroyView"
        )

    /**
     * ViewModel 实例
     *
     * 子类通过 by viewModel() 委托初始化
     */
    protected abstract val viewModel: VM

    /**
     * 提供 ViewBinding 实例
     *
     * @param inflater LayoutInflater
     * @param container ViewGroup?
     * @return ViewBinding 实例
     */
    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /**
     * 初始化 View
     *
     * 在 onViewCreated 中调用，用于设置点击事件、初始化列表等
     *
     * @param savedInstanceState 保存的实例状态
     */
    protected abstract fun initView(savedInstanceState: Bundle?)

    /**
     * 渲染页面状态
     *
     * 当 State 变化时回调，用于更新 UI
     *
     * @param state 当前页面状态
     */
    protected abstract fun renderState(state: State)

    /**
     * 处理一次性事件
     *
     * 默认空实现，子类按需覆写处理 Toast、导航等事件
     *
     * @param event 事件
     */
    protected open fun handleEvent(event: Event) {
        // 默认空实现，子类按需覆写
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(savedInstanceState)
        observeViewModel()
    }

    /**
     * 观察 ViewModel 状态和事件变化
     *
     * 在 viewLifecycleOwner 的 STARTED 状态下收集状态
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 收集状态
                launch {
                    viewModel.state.collectLatest { state ->
                        state?.let { renderState(it) }
                    }
                }
                // 收集事件
                launch {
                    viewModel.event.collectLatest { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
