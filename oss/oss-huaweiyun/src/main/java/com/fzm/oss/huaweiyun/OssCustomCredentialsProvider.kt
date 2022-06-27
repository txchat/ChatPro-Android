package com.fzm.oss.huaweiyun

import android.annotation.SuppressLint
import com.obs.services.IObsCredentialsProvider
import com.obs.services.exception.ObsException
import com.obs.services.internal.security.LimitedTimeSecurityKey
import com.obs.services.model.ISecurityKey
import com.zjy.architecture.di.rootScope
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat

class OssCustomCredentialsProvider(private val stsServer: String) : IObsCredentialsProvider {

    private val client by rootScope.inject<OkHttpClient>()

//    {
//        "result": 0,
//        "message": "",
//        "data": {
//        "RequestId": "",
//        "Credentials": {
//        "AccessKeySecret": "ChbyFLkHvGNWuFGJFDfBrf4J3m8FDckUZEEGek5i",
//        "Expiration": "2021-04-09T04:21:25.372000Z",
//        "AccessKeyId": "K83LF8Y6FXZG0Y46QFVP",
//        "SecurityToken": "ggljbi1lYXN0LTNJJnsiYWNjZXNzIjoiSzgzTEY4WTZGWFpHMFk0NlFGVlAiLCJtZXRob2RzIjpbInRva2VuIl0sInBvbGljeSI6eyJWZXJzaW9uIjoiMS4xIiwiU3RhdGVtZW50IjpbeyJBY3Rpb24iOlsib2JzOm9iamVjdDoqIl0sIkVmZmVjdCI6IkFsbG93In1dfSwicm9sZSI6W10sInJvbGV0YWdlcyI6W10sInRpbWVvdXRfYXQiOjE2MTc5NDIwODUzNzIsInVzZXIiOnsiZG9tYWluIjp7ImlkIjoiMDU2YjIwZDllZTAwMjYwNDBmNWJjMDAyM2JjYmMzYzAiLCJuYW1lIjoiVGhlQ2h5In0sImlkIjoiMDU2YjIwZGFiNTAwMjZmZDFmOTdjMDAyNzU2NzQ1ZDMiLCJuYW1lIjoiVGhlQ2h5IiwicGFzc3dvcmRfZXhwaXJlc19hdCI6IiJ9fcBj_-6oDgqAgJJY2oEhnMNTyG0tBFFD8ba0pg-wtKlX_spitaUkx8TmRH9JMKVWJec19_rAbYZrkbnFwHo0YSYb-HvkyaCZsRw3UsRELUEl1SLCnMGSJAZYQ1mYcn0vHvxhawjtegMv7ZbBImmFsWRUySi1BpoC66_WmTy_CEhdfeaib8So5d6s3PCgS7Zv7Ch_X8xQHoe160271kXDfJ8RZlQnOIuU2tOjTRgo5oDJaBNAAul7knpuoi5IQkYrCrkW4y-4iSbejZrMgae-Ot1dy4vAlgiHAsv6okR7cYV5GqcQZLxdrEY5bYoJD6tbGPCw7M_n6FW9AvOSkkTi6rU="
//    },
//        "AssumedRoleUser": {
//        "AssumedRoleId": "",
//        "Arn": ""
//    }
//    }
//    }

    override fun setSecurityKey(p0: ISecurityKey?) {
        throw Exception("you cannot set security key")
    }

    @SuppressLint("SimpleDateFormat")
    override fun getSecurityKey(): ISecurityKey {

        val authToken: LimitedTimeSecurityKey
        return try {
            val request = Request.Builder().url(stsServer).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val authData = response.body()?.string()
                if (authData.isNullOrEmpty()) {
                    throw ObsException("obs auth empty response")
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
                    LimitedTimeSecurityKey(
                        ak,
                        sk,
                        token,
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(expiration)
                    )
                } else {
                    val errorMessage = jsonObj.getString("message")
                    throw ObsException("ErrorCode: $statusCode| ErrorMessage: $errorMessage")
                }
            } else {
                throw ObsException("request fail")
            }
            authToken
        } catch (e: Exception) {
            throw ObsException("obs exception", e)
        }
    }
}