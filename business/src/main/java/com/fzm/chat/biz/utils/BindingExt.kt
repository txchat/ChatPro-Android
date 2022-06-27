package com.fzm.chat.biz.utils

import android.app.Activity
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * @author zhengjy
 * @since 2022/02/10
 * Description:
 */
inline fun <reified T : ViewBinding> Fragment.init(): FragmentBindingDelegate<T> {
    return FragmentBindingDelegate(T::class.java, this)
}

class FragmentBindingDelegate<T : ViewBinding>(
    private val clazz: Class<T>,
    private val fragment: Fragment
) : ReadOnlyProperty<Fragment, T> {

    private var binding: T? = null

    private val inflater by lazy(LazyThreadSafetyMode.NONE) {
        clazz.getMethod("inflate", LayoutInflater::class.java)
    }

    private val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            destroy()
        }
    }

    private var lifecycleOwner: LifecycleOwner? = null

    init {
        fragment.viewLifecycleOwnerLiveData.observe(fragment) { owner ->
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            owner?.also {
                lifecycleOwner = it
                it.lifecycle.addObserver(observer)
            }
        }
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

inline fun <reified T : ViewBinding> Activity.init(): ActivityBindingDelegate<T> {
    return ActivityBindingDelegate(T::class.java, this)
}

class ActivityBindingDelegate<T : ViewBinding>(
    private val clazz: Class<T>,
    private val activity: Activity
) : ReadOnlyProperty<Activity, T> {

    private var binding: T? = null

    private val inflater by lazy(LazyThreadSafetyMode.NONE) {
        clazz.getMethod("inflate", LayoutInflater::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Activity, property: KProperty<*>): T {
        return binding
            ?: (inflater.invoke(null, activity.layoutInflater) as T).also { binding = it }
    }
}