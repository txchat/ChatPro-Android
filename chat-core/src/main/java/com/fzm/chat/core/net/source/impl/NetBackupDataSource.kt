package com.fzm.chat.core.net.source.impl

import com.fzm.chat.core.data.bean.BackupMnemonic
import com.fzm.chat.core.data.enum.CodeType
import com.fzm.chat.core.net.api.BackupService
import com.fzm.chat.core.net.source.BackupDataSource
import com.zjy.architecture.data.Result
import com.zjy.architecture.ext.apiCall

/**
 * @author zhengjy
 * @since 2021/01/13
 * Description:
 */
class NetBackupDataSource(
    private val service: BackupService
) : BackupDataSource {

    override suspend fun sendPhoneCode(phone: String): Result<Any> {
        return apiCall { service.sendPhoneCode(mapOf("phone" to phone)) }
    }

    override suspend fun sendEmailCode(email: String): Result<Any> {
        return apiCall { service.sendEmailCode(mapOf("email" to email)) }
    }

    override suspend fun sendPhoneCodeV2(phone: String, codeType: CodeType): Result<Any> {
        return apiCall { service.sendPhoneCodeV2(mapOf("phone" to phone, "codeType" to codeType.value)) }
    }

    override suspend fun sendEmailCodeV2(email: String, codeType: CodeType): Result<Any> {
        return apiCall { service.sendEmailCodeV2(mapOf("email" to email, "codeType" to codeType.value)) }
    }

    override suspend fun fetchBackupByPhone(phone: String, code: String): Result<BackupMnemonic> {
        return apiCall { service.fetchBackupByPhoneV2(mapOf("phone" to phone, "code" to code)) }
    }

    override suspend fun fetchBackupByEmail(email: String, code: String): Result<BackupMnemonic> {
        return apiCall { service.fetchBackupByEmailV2(mapOf("email" to email, "code" to code)) }
    }

    override suspend fun fetchBackupByAddress(): Result<BackupMnemonic> {
        return apiCall { service.fetchBackupByAddress() }
    }

    override suspend fun bindPhone(phone: String, code: String, mnemonic: String): Result<Any> {
        val map = mapOf(
            "phone" to phone,
            "code" to code,
            "mnemonic" to mnemonic
        )
        return apiCall { service.bindPhone(map) }
    }

    override suspend fun bindEmail(email: String, code: String, mnemonic: String): Result<Any> {
        val map = mapOf(
            "email" to email,
            "code" to code,
            "mnemonic" to mnemonic
        )
        return apiCall { service.bindEmail(map) }
    }

    override suspend fun bindPhoneV2(phone: String, code: String, mnemonic: String): Result<Any> {
        val map = mapOf(
            "phone" to phone,
            "code" to code,
            "mnemonic" to mnemonic
        )
        return apiCall { service.bindPhoneV2(map) }
    }

    override suspend fun bindEmailV2(email: String, code: String, mnemonic: String): Result<Any> {
        val map = mapOf(
            "email" to email,
            "code" to code,
            "mnemonic" to mnemonic
        )
        return apiCall { service.bindEmailV2(map) }
    }

    override suspend fun phoneExport(phone: String, code: String, address: String): Result<Any> {
        val map = mapOf(
            "phone" to phone,
            "code" to code,
            "address" to address
        )
        return apiCall { service.phoneExport(map) }
    }

    override suspend fun emailExport(email: String, code: String, address: String): Result<Any> {
        val map = mapOf(
            "email" to email,
            "code" to code,
            "address" to address
        )
        return apiCall { service.emailExport(map) }
    }

    override suspend fun phoneQuery(phone: String): Result<Map<String, *>> {
        return apiCall { service.phoneQuery(mapOf("phone" to phone)) }
    }

    override suspend fun emailQuery(email: String): Result<Map<String, *>> {
        return apiCall { service.emailQuery(mapOf("email" to email)) }
    }

    override suspend fun updateMnemonic(mnemonic: String): Result<Any> {
        return apiCall { service.updateMnemonic(mapOf("mnemonic" to mnemonic)) }
    }

    override suspend fun getAddress(query: String): Result<BackupMnemonic> {
        return apiCall { service.getAddress(mapOf("query" to query)) }
    }
}