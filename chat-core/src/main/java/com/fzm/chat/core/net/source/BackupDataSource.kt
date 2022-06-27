package com.fzm.chat.core.net.source

import com.fzm.chat.core.data.bean.BackupMnemonic
import com.fzm.chat.core.data.enum.CodeType
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2021/01/13
 * Description:
 */
interface BackupDataSource {

    /**
     * 发送手机验证码
     */
    @Deprecated("使用v2接口")
    suspend fun sendPhoneCode(phone: String): Result<Any>

    /**
     * 发送邮箱验证码
     */
    @Deprecated("使用v2接口")
    suspend fun sendEmailCode(email: String): Result<Any>

    /**
     * 发送手机验证码
     */
    suspend fun sendPhoneCodeV2(phone: String, codeType: CodeType): Result<Any>

    /**
     * 发送邮箱验证码
     */
    suspend fun sendEmailCodeV2(email: String, codeType: CodeType): Result<Any>

    /**
     * 获取备份的助记词
     */
    suspend fun fetchBackupByPhone(phone: String, code: String): Result<BackupMnemonic>

    /**
     * 获取备份的助记词
     */
    suspend fun fetchBackupByEmail(email: String, code: String): Result<BackupMnemonic>

    /**
     * 获取备份的助记词
     */
    suspend fun fetchBackupByAddress(): Result<BackupMnemonic>

    /**
     * 绑定手机号，并备份助记词
     */
    suspend fun bindPhone(phone: String, code: String, mnemonic: String): Result<Any>

    /**
     * 绑定邮箱，并备份助记词
     */
    suspend fun bindEmail(email: String, code: String, mnemonic: String): Result<Any>

    /**
     * 绑定手机号，并备份助记词
     */
    suspend fun bindPhoneV2(phone: String, code: String, mnemonic: String): Result<Any>

    /**
     * 绑定邮箱，并备份助记词
     */
    suspend fun bindEmailV2(email: String, code: String, mnemonic: String): Result<Any>

    /**
     * 使用手机号验证，导出本地账户
     */
    suspend fun phoneExport(phone: String, code: String, address: String): Result<Any>

    /**
     * 使用邮箱验证，导出本地账户
     */
    suspend fun emailExport(email: String, code: String, address: String): Result<Any>

    /**
     * 手机绑定查询
     */
    suspend fun phoneQuery(phone: String): Result<Map<String, *>>

    /**
     * 邮箱绑定查询
     */
    suspend fun emailQuery(email: String): Result<Map<String, *>>

    /**
     * 修改加密助记词
     */
    suspend fun updateMnemonic(mnemonic: String): Result<Any>

    /**
     * 通过邮箱或者手机号获取地址
     */
    suspend fun getAddress(query: String): Result<BackupMnemonic>
}