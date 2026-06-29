package com.ch.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 全局输入框组件
 *
 * 基于 Material 3 OutlinedTextField 封装的统一输入框组件，
 * 支持标签、占位符、错误状态、前后图标等常见功能。
 *
 * 功能特性：
 * - 支持标签和占位符
 * - 支持错误状态和错误提示文字
 * - 支持前导和尾随意图
 * - 统一的设计风格
 *
 * 用法示例：
 * ```kotlin
 * GlobalInputField(
 *     value = text,
 *     onValueChange = { text = it },
 *     label = "用户名",
 *     placeholder = "请输入用户名",
 *     isError = hasError,
 *     errorMessage = "用户名不能为空",
 *     modifier = Modifier.fillMaxWidth()
 * )
 * ```
 *
 * @param value 输入框当前值
 * @param onValueChange 值变化回调
 * @param label 输入框标签
 * @param placeholder 占位符文字
 * @param isError 是否显示错误状态
 * @param errorMessage 错误提示文字
 * @param leadingIcon 前导图标
 * @param trailingIcon 尾随意图
 * @param modifier 修饰符
 * @param visualTransformation 视觉转换，用于密码输入等场景
 * @param keyboardOptions 键盘选项
 * @param singleLine 是否单行显示
 * @param maxLines 最大行数
 */
@Composable
fun GlobalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = if (label != null) {
                { Text(text = label) }
            } else null,
            placeholder = if (placeholder != null) {
                { Text(text = placeholder) }
            } else null,
            isError = isError,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            maxLines = maxLines,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.primary,
                errorCursorColor = MaterialTheme.colorScheme.error,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLeadingIconColor = MaterialTheme.colorScheme.error,
                focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorTrailingIconColor = MaterialTheme.colorScheme.error
            ),
            textStyle = TextStyle(fontSize = 16.sp)
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
