package com.fzm.chat.biz.base

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.fzm.chat.biz.R
import com.zjy.architecture.base.BaseActivity
import com.zjy.architecture.ext.notchSupport
import com.zjy.architecture.util.other.BarUtils
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * @author zhengjy
 * @since 2020/12/07
 * Description:
 */
abstract class BizActivity : BaseActivity() {

    override val layoutId: Int = 0

    abstract val root: View

    protected open val darkStatusColor = false

    override fun setContentView() {
        setContentView(root)
    }

    override fun setSystemBar() {
        notchSupport(window)
        val color = if (darkStatusColor) R.color.biz_color_primary_dark else R.color.biz_color_primary
        BarUtils.setStatusBarColor(this, ContextCompat.getColor(this, color), 0)
        BarUtils.setStatusBarLightMode(this, true)
        window?.navigationBarColor = ContextCompat.getColor(this, color)
    }

    @Deprecated("不需要传入初始化方法", ReplaceWith("init()"))
    protected fun <T> init(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)

    @Deprecated("使用init()扩展函数")
    protected inline fun <reified T : ViewBinding> init(): ActivityBindingDelegate<T> {
        return ActivityBindingDelegate(T::class.java, this)
    }

    protected class ActivityBindingDelegate<T : ViewBinding>(
        private val clazz: Class<T>,
        private val activity: AppCompatActivity
    ) : ReadOnlyProperty<AppCompatActivity, T> {

        private var binding: T? = null

        private val inflater by lazy(LazyThreadSafetyMode.NONE) { clazz.getMethod("inflate", LayoutInflater::class.java) }

        @Suppress("UNCHECKED_CAST")
        override fun getValue(thisRef: AppCompatActivity, property: KProperty<*>): T {
            return binding
                ?: (inflater.invoke(null, activity.layoutInflater) as T).also { binding = it }
        }
    }
}