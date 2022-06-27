package com.fzm.chat.router.main

/**
 * @author zhengjy
 * @since 2020/12/07
 * Description:
 */
object MainModule {

    private const val GROUP = "/main"

    const val INJECTOR = "$GROUP/injector"

    const val SERVICE = "$GROUP/service"

    const val SESSION = "$GROUP/session"
    const val CONTACT = "$GROUP/contact"
    const val ACCOUNT = "$GROUP/account"

    // 登录模块
    const val CHOOSE_LOGIN = "$GROUP/choose_login"
    const val CHOOSE_SERVER = "$GROUP/choose_server"
    const val EDIT_SERVER = "$GROUP/add_server"
    const val CREATE_MNEM = "$GROUP/create_mnem"
    const val BACKUP_MNEM = "$GROUP/backup_mnem"
    const val IMPORT_ACCOUNT = "$GROUP/import_account"
    const val LOGIN = "$GROUP/login"
    const val END_POINT_LOGIN = "$GROUP/end_point_login"

    const val QR_SCAN = "$GROUP/qr_scan"
    const val QR_CODE = "$GROUP/qr_code"
    const val SEARCH_USER = "$GROUP/search_user"
    const val SEARCH_ONLINE = "$GROUP/search_online"
    const val EDIT_INFO = "$GROUP/edit_info"
    const val EDIT_AVATAR = "$GROUP/edit_avatar"

    const val IMPORT_LOCAL_ACCOUNT = "$GROUP/import_local_account"

    // 服务器管理
    const val SERVER_MANAGE = "$GROUP/server_manage"

    // 添加服务器分组
    const val EDIT_SERVER_GROUP = "$GROUP/add_server_group"

    // 选择服务器分组
    const val CHOOSE_SERVER_GROUP = "$GROUP/choose_server_group"

    // 发消息联系人选择页面
    const val CONTACT_SELECT = "$GROUP/contact_select"

    // 安全管理
    const val SECURITY_SET = "$GROUP/security_set"
    const val SET_ENC_PWD = "$GROUP/set_encrypt_password"
    const val ENCRYPT_PWD = "$GROUP/encrypt_password"
    const val BIND_ACCOUNT = "$GROUP/bind_account"

    // 本地搜索
    const val SEARCH_LOCAL = "$GROUP/search_local"
    const val SEARCH_LOCAL_SCOPED = "$GROUP/search_local_scoped"
    const val SEARCH_FILE = "$GROUP/search_file"

    // 设置中心
    const val SETTING_CENTER = "$GROUP/setting_center"

    // 系統分享
    const val SYSTEM_SHARE = "$GROUP/system_share"

    // 聊天联系人
    const val BLOCK_LIST_ATY = "$GROUP/block_list_aty"
    const val BLOCK_LIST = "$GROUP/block_list"
    const val CONTACT_LIST = "$GROUP/contact_list"
    const val CONTACT_INFO = "$GROUP/contact_info"
    const val CONTACT_REMARK = "$GROUP/contact_remark"
    const val CHAT = "$GROUP/chat"
    const val FORWARD_LIST = "$GROUP/forward_list"
    const val ADD_FRIEND = "$GROUP/add_friend"

    // 群聊
    const val GROUP_INFO = "$GROUP/group_info"
    const val GROUP_SERVER = "$GROUP/group_server"
    const val CREATE_GROUP = "$GROUP/create_group"
    const val GROUP_USER = "$GROUP/group_user"
    const val GROUP_QRCODE = "$GROUP/group_qrcode"
    const val JOIN_GROUP_INFO = "$GROUP/join_group_info"
    const val CREATE_INVITE_GROUP = "$GROUP/create_invite_group"
    const val REMOVE_GROUP_USER = "$GROUP/remove_group_user"
    const val EDIT_GROUP_NAME = "$GROUP/edit_group_name"
    const val EDIT_GROUP_AVATAR = "$GROUP/edit_group_avatar"
    const val MUTE_LIST = "$GROUP/mute_list"
    const val ADMIN_LIST = "$GROUP/admin_list"
    const val SET_ADMIN = "$GROUP/set_admin"

    const val LARGE_TEXT = "$GROUP/large_text"

    const val FILE_MANAGEMENT = "$GROUP/file_management"
    const val FILE_DETAIL = "$GROUP/file_detail"
    @Deprecated("暂时没有用到")
    const val VIDEO_PLAYER = "$GROUP/video_player"
    const val MEDIA_GALLERY = "$GROUP/image_gallery"
    const val CAPTURE = "$GROUP/capture"
    const val UPDATE_DIALOG = "$GROUP/update_dialog"

    // DialogFragment
    const val VERIFY_ENC_PWD = "$GROUP/verify_encrypt_password"
    const val DECRYPT_MNEMONIC = "$GROUP/decrypt_mnemonic"
}