package com.fzm.chat.core.net

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * @author zhengjy
 * @since 2021/08/12
 * Description:
 */
class TrustAllCertificate : X509TrustManager {

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return emptyArray()
    }

    companion object {
        fun createSSLSocketFactory(): SSLSocketFactory? {
            return try {
                val sc = SSLContext.getInstance("TLS")
                sc.init(null, arrayOf(TrustAllCertificate()), SecureRandom())

                sc.socketFactory
            } catch (e: Exception) {
                null
            }
        }
    }
}

class TrustAllHostnameVerifier : HostnameVerifier {

    override fun verify(hostname: String?, session: SSLSession?): Boolean {
        return true
    }
}