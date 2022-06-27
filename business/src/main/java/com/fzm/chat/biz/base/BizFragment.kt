package com.fzm.chat.biz.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import com.zjy.architecture.base.BaseFragment
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * @author zhengjy
 * @since 2020/12/09
 * Description:
 */
abstract class BizFragment : BaseFragment() {

    abstract val root: View

    override val layoutId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return root
    }

    @Deprecated("使用init()扩展函数")
    protected inline fun <reified T : ViewBinding> init(): FragmentBindingDelegate<T> {
        return FragmentBindingDelegate(T::class.java, this)
    }

    protected class FragmentBindingDelegate<T : ViewBinding>(
        private val clazz: Class<T>,
        private val fragment: Fragment
    ) : ReadOnlyProperty<Fragment, T> {

        private var binding: T? = null

        private val inflater by lazy(LazyThreadSafetyMode.NONE) { clazz.getMethod("inflate", LayoutInflater::class.java) }

        init {
            fragment.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        destroy()
                        source.lifecycle.removeObserver(this)
                    }
                }
            })
        }

        @Suppress("UNCHECKED_CAST")
        override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
            return binding
                ?: (inflater.invoke(null, fragment.layoutInflater) as T).also { binding = it }
        }

        private fun destroy() {
            binding = null
        }
    }
}