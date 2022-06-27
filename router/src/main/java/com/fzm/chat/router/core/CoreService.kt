package com.fzm.chat.router.core

import com.alibaba.android.arouter.facade.template.IProvider

/**
 * @author zhengjy
 * @since 2020/12/08
 * Description:
 */
interface CoreService : IProvider {

    /**
     * 对请求参数进行签名
     */
    fun sign(map: Map<String, String>): String

    /**
     * 用用户私钥加密字符串
     */
    fun sign(data: String): ByteArray

    /**
     * 生成服务端需要认证的签名格式
     */
    fun signAuth(): String

    /**
     * 鉴权额外参数
     */
    fun authPayload(firstConnect: Boolean): ByteArray

    /**
     * 鉴权返回数据解析
     */
    fun parseAuthReply(payload: ByteArray)

    /**
     * 检查消息service是否启动
     */
    fun checkService()
}