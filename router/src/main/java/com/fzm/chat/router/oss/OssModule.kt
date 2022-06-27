package com.fzm.chat.router.oss

/**
 * @author zhengjy
 * @since 2021/01/28
 * Description:
 */
object OssModule {

    private const val GROUP = "/oss"

    const val INJECTOR = "$GROUP/injector"

    const val ALIYUN_OSS = "$GROUP/aliyun_oss"

    const val HUAWEI_OSS = "$GROUP/huawei_oss"

    const val FZM_OSS = "$GROUP/fzm_oss"

    //const val APP_OSS = ALIYUN_OSS
//    const val APP_OSS = HUAWEI_OSS
    const val APP_OSS = FZM_OSS
}