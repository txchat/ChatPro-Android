package com.fzm.chat.core.rtc.calling

import com.fzm.chat.core.rtc.data.RTCTask

/**
 * @author zhengjy
 * @since 2021/07/07
 * Description:
 */
interface RTCCallingDelegate {

    /**
     * sdk内部发生了错误
     * @param code 错误码
     * @param msg 错误消息
     */
    fun onError(code: Int, msg: String?)

    /**
     * 被邀请通话回调
     * @param sponsor 邀请者
     * @param userIdList 同时还被邀请的人
     * @param isFromGroup 是否IM群组邀请
     * @param callType 邀请类型 1-语音通话，2-视频通话
     */
    fun onInvited(sponsor: String, userIdList: List<String>, isFromGroup: Boolean, callType: Int)

    /**
     * 对方接受邀请回调
     * @param calledId 邀请者
     * @param task 通话任务
     * @param targetId 接受邀请的人
     */
    fun onAccepted(calledId: String?, task: RTCTask, targetId: String?)

    /**
     * 正在IM群组通话时，如果其他与会者邀请他人，会收到此回调
     * 例如 A-B-C 正在IM群组中，A邀请[D、E]进入通话，B、C会收到[D、E]的回调
     * 如果此时 A 再邀请 F 进入群聊，那么B、C会收到[D、E、F]的回调
     * @param userIdList 邀请群组
     */
    fun onGroupCallInviteeListUpdate(userIdList: List<String>)

    /**
     * 当用户自己进入房间时的回调
     * @param cost 进房消耗时长
     */
    fun onEnterRoom(cost: Long)

    /**
     * 如果有其他用户同意进入通话，那么会收到此回调
     * @param userId 进入通话的用户
     */
    fun onUserEnter(userId: String?)

    /**
     * 如果有用户同意离开通话，那么会收到此回调
     * @param userId 离开通话的用户
     */
    fun onUserLeave(userId: String?)

    /**
     * 远端用户开启/关闭了摄像头
     * @param userId 远端用户ID
     * @param isVideoAvailable true:远端用户打开摄像头  false:远端用户关闭摄像头
     */
    fun onUserVideoAvailable(userId: String?, isVideoAvailable: Boolean)

    /**
     * 远端用户开启/关闭了麦克风
     * @param userId 远端用户ID
     * @param isVideoAvailable true:远端用户打开麦克风  false:远端用户关闭麦克风
     */
    fun onUserAudioAvailable(userId: String?, isVideoAvailable: Boolean)

    /**
     * 1. 在C2C通话中，只有发起方会收到拒绝回调
     * 例如 A 邀请 B、C 进入通话，B拒绝，A可以收到该回调，但C不行
     *
     * 2. 在IM群组通话中，所有被邀请人均能收到该回调
     * 例如 A 邀请 B、C 进入通话，B拒绝，A、C均能收到该回调
     * @param userId 拒绝通话的用户
     */
    fun onReject(userId: String?)

    /**
     * 用户在接通电话后挂断
     */
    fun onHangup()

    /**
     * 1. 在C2C通话中，只有发起方会收到无人应答的回调
     * 例如 A 邀请 B、C 进入通话，B不应答，A可以收到该回调，但C不行
     *
     * 2. 在IM群组通话中，所有被邀请人均能收到该回调
     * 例如 A 邀请 B、C 进入通话，B不应答，A、C均能收到该回调
     * @param userId
     */
    fun onNoResp(userId: String?)

    /**
     * 邀请方忙线
     * @param userId 忙线用户
     */
    fun onLineBusy(userId: String?)

    /**
     * 作为被邀请方会收到，收到该回调说明本次通话被取消了
     */
    fun onCallingCancel()

    /**
     * 作为被邀请方会收到，收到该回调说明本次通话超时未应答
     */
    fun onCallingTimeout()

    /**
     * 收到该回调说明本次通话结束了
     */
    fun onCallEnd()

    /**
     * 通话时长回调
     */
    fun onCallTimeTick(duration: Long)

    /**
     * 用户说话音量回调
     * @param volumeMap 音量表，根据每个userid可以获取对应的音量大小，音量最小值0，音量最大值100
     */
    fun onUserVoiceVolume(volumeMap: Map<String, Int>)

    /**
     * 视频通话切换为语音通话回调
     * @param success 切换是否成功，true:切换成功  false:切换失败
     * @param message 切换结果信息，如果切换失败，会有切换失败的错误信息
     */
    fun onSwitchToAudio(success: Boolean, message: String?)
}