package com.fzm.oss.aliyun

import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.common.auth.OSSFederationCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSFederationToken
import com.zjy.architecture.di.rootScope
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * @author zhengjy
 * @since 2021/02/24
 * Description:
 */
class OSSCustomAuthCredentialsProvider(
    private val stsServer: String
) : OSSFederationCredentialProvider() {

    /*{
        "result": 0,
        "message": "操作成功",
        "data": {
            "RequestId": "",
            "Credentials": {
                "AccessKeySecret": "",
                "Expiration": "2021-02-24T07:48:21Z",
                "AccessKeyId": "",
                "SecurityToken": ""
            },
            "AssumedRoleUser": {
                "AssumedRoleId": "",
                "Arn": ""
            }
        }
    }*/

    private val client by rootScope.inject<OkHttpClient>()

    override fun getFederationToken(): OSSFederationToken {
        val authToken: OSSFederationToken
        return try {
            val request = Request.Builder().url(stsServer).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val authData = response.body()?.string()
                if (authData.isNullOrEmpty()) {
                    throw ClientException("oss auth empty response")
                }
                val jsonObj = JSONObject(authData)
                val statusCode = jsonObj.getInt("result")
                authToken = if (statusCode == 0) {
                    val data = jsonObj.getJSONObject("data")
                    val credential = data.getJSONObject("Credentials")
                    val ak = credential.getString("AccessKeyId")
                    val sk = credential.getString("AccessKeySecret")
                    val token = credential.getString("SecurityToken")
                    val expiration = credential.getString("Expiration")
                    OSSFederationToken(ak, sk, token, expiration)
                } else {
                    val errorMessage = jsonObj.getString("message")
                    throw ClientException("ErrorCode: $statusCode| ErrorMessage: $errorMessage")
                }
            } else {
                throw ClientException("request fail")
            }
            authToken
        } catch (e: Exception) {
            throw ClientException(e)
        }
    }
}