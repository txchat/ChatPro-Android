package com.fzm.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.fzm.widget.databinding.CustomTopTitleBarBinding

/**
 * Created by ljn on 2019/12/31.
 * Explain 自定义的顶部标题栏
 */
class CustomTitleBar : FrameLayout {

    private lateinit var binding: CustomTopTitleBarBinding

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        initView(context)
        initData(context, attrs)
    }

    private fun initView(context: Context) {
        binding = CustomTopTitleBarBinding.inflate(LayoutInflater.from(context), this, true)
    }

    @SuppressLint("CustomViewStyleable")
    private fun initData(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.CustomTitleBar
        ).apply {
            binding.ivLeft.apply {
                val leftIconWidth = getDimension(R.styleable.CustomTitleBar_left_icon_width, -1f)
                val leftIconHeight =
                    getDimension(R.styleable.CustomTitleBar_left_icon_height, -1f)
                if (leftIconWidth != -1f || leftIconHeight != -1f) {
                    layoutParams = layoutParams.apply {
                        width = if (leftIconWidth == -1f) width else leftIconWidth.toInt()
                        height = if (leftIconHeight == -1f) height else leftIconHeight.toInt()
                    }
                }
                val leftIcon = getResourceId(R.styleable.CustomTitleBar_left_icon, -1)
                if (leftIcon != -1) {
                    setImageResource(leftIcon)
                }
            }

            binding.tvLeft.apply {
                text = getString(R.styleable.CustomTitleBar_left_text)
                setTextColor(
                    getColor(
                        R.styleable.CustomTitleBar_left_text_color,
                        resources.getColor(R.color.widget_title_color_text)
                    )
                )
                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX, getDimension(
                        R.styleable.CustomTitleBar_left_text_size,
                        resources.getDimension(R.dimen.widget_titleBar_text)
                    )
                )
            }

            binding.tvMiddle.apply {
                text = getString(R.styleable.CustomTitleBar_title_text)
                setTextColor(
                    getColor(
                        R.styleable.CustomTitleBar_title_text_color,
                        resources.getColor(R.color.widget_title_color_text)
                    )
                )
                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX, getDimension(
                        R.styleable.CustomTitleBar_title_text_size,
                        resources.getDimension(R.dimen.widget_titleBar_title)
                    )
                )
                paint.isFakeBoldText = true
            }

            binding.ivRight.apply {
                val rightIconWidth =
                    getDimension(R.styleable.CustomTitleBar_right_icon_width, -1f)
                val rightIconHeight =
                    getDimension(R.styleable.CustomTitleBar_right_icon_height, -1f)
                if (rightIconWidth != -1f || rightIconHeight != -1f) {
                    layoutParams = layoutParams.apply {
                        width =
                            if (rightIconWidth == -1f) width else rightIconWidth.toInt()
                        height =
                            if (rightIconHeight == -1f) height else rightIconHeight.toInt()
                    }
                }
                val rightIcon =
                    getResourceId(R.styleable.CustomTitleBar_right_icon, -1)
                if (rightIcon != -1) {
                    setImageResource(rightIcon)
                }
            }

            binding.tvRight.apply {
                text = getString(R.styleable.CustomTitleBar_right_text)
                setTextColor(
                    getColor(
                        R.styleable.CustomTitleBar_right_text_color,
                        resources.getColor(R.color.widget_title_color_text)
                    )
                )
                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX, getDimension(
                        R.styleable.CustomTitleBar_right_text_size,
                        resources.getDimension(R.dimen.widget_titleBar_text)
                    )
                )
            }

            // 销毁
            recycle()
        }
    }

    fun setTitle(title: String?) {
        binding.tvMiddle.text = title
    }

    fun setTitleAlpha(alpha: Float) {
        binding.tvMiddle.alpha = alpha
    }

    fun getTitleView() = binding.tvMiddle

    fun getLeftView() = binding.layoutLeft

    fun getRightView() = binding.layoutRight

    fun setLeftVisible(visible: Boolean) {
        binding.layoutLeft.visibility = if (visible) {
            VISIBLE
        } else {
            GONE
        }
    }

    fun setLeftText(left: String) {
        binding.tvLeft.text = left
    }

    fun setRightVisible(visible: Boolean) {
        binding.layoutRight.visibility = if (visible) {
            VISIBLE
        } else {
            GONE
        }
    }

    fun setRightText(right: String) {
        binding.tvRight.text = right
    }

    fun setOnLeftClickListener(listener: OnClickListener) {
        binding.layoutLeft.setOnClickListener(listener)
    }

    fun setOnLeftClickListener(listener: (View) -> Unit) {
        binding.layoutLeft.setOnClickListener {
            listener(it)
        }
    }

    fun setOnRightClickListener(listener: OnClickListener) {
        binding.layoutRight.setOnClickListener(listener)
    }

    fun setOnRightClickListener(listener: (View) -> Unit) {
        binding.layoutRight.setOnClickListener {
            listener(it)
        }
    }
}
