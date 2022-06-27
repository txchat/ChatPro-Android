package com.fzm.chat.router

import android.os.Bundle
import com.alibaba.android.arouter.launcher.ARouter

/**
 * @author zhengjy
 * @since 2020/05/12
 * Description:
 */
/**
 * 延迟加载路径为[path]的Router
 */
@Suppress("UNCHECKED_CAST")
fun <T> route(path: String?, bundle: Bundle? = null, default: (() -> T)? = null): Lazy<T?> = lazy(LazyThreadSafetyMode.NONE) {
    try {
        val route = ARouter.getInstance().build(path).with(bundle).navigation() as T?
        route ?: default?.invoke()
    } catch (e: Exception) {
        default?.invoke()
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> route(pair: Pair<String, Bundle?>, default: (() -> T)? = null) = route(pair.first, pair.second, default)