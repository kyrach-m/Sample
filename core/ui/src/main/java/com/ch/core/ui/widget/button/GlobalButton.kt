package com.ch.core.ui.widget.button

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.ch.core.ui.R
import com.google.android.material.button.MaterialButton

class GlobalButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialButtonStyle
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class Variant(val value: Int) {
        PRIMARY(0),
        SECONDARY(1),
        TEXT(2),
        DANGER(3)
    }

    enum class ButtonSize(val value: Int) {
        LARGE(0),
        MEDIUM(1),
        SMALL(2)
    }

    private var variant: Variant = Variant.PRIMARY
    private var buttonSize: ButtonSize = ButtonSize.MEDIUM
    private var isLoading: Boolean = false
    private var cornerRadius: Float = 0f

    private val button: MaterialButton
    private val progressBar: ProgressBar

    private var originalText: CharSequence? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        button = MaterialButton(context, null, com.google.android.material.R.attr.materialButtonStyle)
        progressBar = ProgressBar(context).apply {
            isIndeterminate = true
            visibility = GONE
        }

        // 安全读取属性值，避免数组越界
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GlobalButton, defStyleAttr, 0)
        val variantIndex = typedArray.getInt(R.styleable.GlobalButton_variant, 0).coerceIn(0, Variant.entries.size - 1)
        val buttonSizeIndex = typedArray.getInt(R.styleable.GlobalButton_buttonSize, 1).coerceIn(0, ButtonSize.entries.size - 1)
        variant = Variant.entries[variantIndex]
        buttonSize = ButtonSize.entries[buttonSizeIndex]
        isLoading = typedArray.getBoolean(R.styleable.GlobalButton_loading, false)
        cornerRadius = typedArray.getDimension(R.styleable.GlobalButton_cornerRadius, resources.getDimension(R.dimen.radius_medium))
        val text = typedArray.getString(android.R.attr.text)
        typedArray.recycle()

        setupButton()
        setupProgressBar()

        if (!text.isNullOrEmpty()) {
            button.text = text
        }

        if (isLoading) {
            showLoading()
        }
    }

    private fun setupButton() {
        val height = when (buttonSize) {
            ButtonSize.LARGE -> resources.getDimensionPixelSize(R.dimen.button_height_large)
            ButtonSize.MEDIUM -> resources.getDimensionPixelSize(R.dimen.button_height_medium)
            ButtonSize.SMALL -> resources.getDimensionPixelSize(R.dimen.button_height_small)
        }

        val btnTextSize = when (buttonSize) {
            ButtonSize.LARGE -> resources.getDimension(R.dimen.button_text_size_large) / resources.displayMetrics.density
            ButtonSize.MEDIUM -> resources.getDimension(R.dimen.button_text_size_medium) / resources.displayMetrics.density
            ButtonSize.SMALL -> resources.getDimension(R.dimen.button_text_size_small) / resources.displayMetrics.density
        }

        val hPadding = when (buttonSize) {
            ButtonSize.LARGE -> resources.getDimensionPixelSize(R.dimen.button_horizontal_padding_large)
            ButtonSize.MEDIUM -> resources.getDimensionPixelSize(R.dimen.button_horizontal_padding_medium)
            ButtonSize.SMALL -> resources.getDimensionPixelSize(R.dimen.button_horizontal_padding_small)
        }

        button.apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, height)
            textSize = btnTextSize
            setPadding(hPadding, 0, hPadding, 0)
            cornerRadius = this@GlobalButton.cornerRadius.toInt()
            isClickable = true
            isFocusable = true
        }

        applyVariant()

        addView(button, LayoutParams(LayoutParams.MATCH_PARENT, height))
    }

    private fun setupProgressBar() {
        val size = when (buttonSize) {
            ButtonSize.LARGE -> 24
            ButtonSize.MEDIUM -> 20
            ButtonSize.SMALL -> 16
        }
        val pxSize = (size * resources.displayMetrics.density).toInt()

        progressBar.layoutParams = LayoutParams(pxSize, pxSize).apply {
            gravity = Gravity.CENTER
        }
    }

    private fun applyVariant() {
        when (variant) {
            Variant.PRIMARY -> {
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.primary))
                button.setTextColor(context.getColor(R.color.on_primary))
                button.strokeWidth = 0
            }
            Variant.SECONDARY -> {
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.surface))
                button.setTextColor(context.getColor(R.color.primary))
                button.strokeWidth = (2 * resources.displayMetrics.density).toInt()
                button.strokeColor = android.content.res.ColorStateList.valueOf(context.getColor(R.color.primary))
            }
            Variant.TEXT -> {
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
                button.setTextColor(context.getColor(R.color.primary))
                button.strokeWidth = 0
            }
            Variant.DANGER -> {
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.error))
                button.setTextColor(context.getColor(R.color.on_error))
                button.strokeWidth = 0
            }
        }
    }

    fun showLoading() {
        isLoading = true
        originalText = button.text
        button.text = ""
        button.isEnabled = false
        addView(progressBar, 0)
        progressBar.visibility = VISIBLE
    }

    fun hideLoading() {
        isLoading = false
        button.text = originalText
        button.isEnabled = true
        progressBar.visibility = GONE
        removeView(progressBar)
    }

    fun setText(text: CharSequence?) {
        if (isLoading) {
            originalText = text
        } else {
            button.text = text
        }
    }

    fun getText(): CharSequence? {
        return if (isLoading) originalText else button.text
    }

    fun setOnButtonClickListener(listener: OnClickListener?) {
        button.setOnClickListener(listener)
    }

    fun setVariant(variant: Variant) {
        this.variant = variant
        applyVariant()
    }

    fun setButtonSize(size: ButtonSize) {
        this.buttonSize = size
        setupButton()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        button.isEnabled = enabled
    }

    override fun isEnabled(): Boolean {
        return button.isEnabled
    }
}
