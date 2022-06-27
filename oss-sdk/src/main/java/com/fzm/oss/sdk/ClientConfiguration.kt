package com.fzm.oss.sdk

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/18
 * Description:
 */
class ClientConfiguration : Serializable {

    /**
     * 连接超时时间
     */
    var connectTimeout: Long = 2 * 60 * 1000
    /**
     * 读写超时时间
     */
    var socketTimeout: Long = 2 * 60 * 1000

    /**
     * 云服务商, 可不填, 会选择默认服务商, 目前可选huaweiyun,aliyun,minio(不支持临时角色)
     */
    var ossType: String = ""

    /**
     * 应用唯一标识id
     */
    var appId: String = ""
}