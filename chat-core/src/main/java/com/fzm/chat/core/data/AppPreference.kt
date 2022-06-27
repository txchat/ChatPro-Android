package com.fzm.chat.core.data

import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.core.utils.uuid
import com.zjy.architecture.Arch
import com.zjy.architecture.util.preference.IStorage
import com.zjy.architecture.util.preference.Preference
import com.zjy.architecture.util.preference.PreferenceDelegate
import com.zjy.architecture.util.preference.PreferenceStorage

/**
 * @author zhengjy
 * @since 2020/08/06
 * Description:
 */
object AppPreference : Preference {

    private const val PREF_ADDRESS = "PREF_ADDRESS"
    private const val PREF_CONTRACT_URL = "PREF_CONTRACT_URL"
    private const val PREF_CHAT_URL = "PREF_CHAT_URL"
    private const val PREF_WALLET_URL = "PREF_WALLET_URL"
    private const val PREF_WALLET_PROXY_URL = "PREF_WALLET_PROXY_URL"
    private const val PREF_OA_URL = "PREF_OA_URL"
    private const val PREF_SHOP_URL = "PREF_SHOP_URL"

    private const val DEVICE_TOKEN = "DEVICE_TOKEN"
    private const val UUID = "UUID"
    private const val KEYBOARD_HEIGHT = "KEYBOARD_HEIGHT"

    private val changeListener = OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            PREF_ADDRESS -> address_.value = ADDRESS
        }
    }

    private val preference = Arch.context.getSharedPreferences(
        "${Arch.context.packageName}.AppPreference",
        Context.MODE_PRIVATE
    ).apply {
        registerOnSharedPreferenceChangeListener(changeListener)
    }

    override val sp: IStorage = PreferenceStorage(preference)

    /**
     * 获取用户地址，登录之后首先要设置Address
     */
    var ADDRESS by PreferenceDelegate(PREF_ADDRESS, "", sp, true)

    /**
     * 获取可观察address
     */
    private val address_: MutableLiveData<String> by lazy {
        MutableLiveData<String>().apply { postValue(ADDRESS) }
    }
    val address: LiveData<String> = address_

    /**
     * 上次登录时的合约服务器地址
     */
    var CONTRACT_URL by PreferenceDelegate(PREF_CONTRACT_URL, "", sp, true)

    /**
     * 上次登录时的聊天服务器地址
     */
    var CHAT_URL by PreferenceDelegate(PREF_CHAT_URL, "", sp)

    /**
     * 上次登录时的钱包合约服务器地址
     */
    var WALLET_URL by PreferenceDelegate(PREF_WALLET_URL, "", sp)

    /**
     * 上次登录时的企业服务器地址
     */
    var OA_URL by PreferenceDelegate(PREF_OA_URL, "", sp)

    /**
     * 上次登录时的商城服务器地址
     */
    var SHOP_URL by PreferenceDelegate(PREF_SHOP_URL, "", sp)

    /**
     * 推送deviceToken
     */
    var deviceToken by PreferenceDelegate(DEVICE_TOKEN, "", sp, true)

    /**
     * uuid
     */
    var uuid by PreferenceDelegate(UUID, "", sp, true)
        private set

    /**
     * 键盘高度
     */
    var keyboardHeight by PreferenceDelegate(KEYBOARD_HEIGHT, 0, sp)


    init {
        if (uuid.isEmpty()) {
            uuid = uuid()
        }
    }
}