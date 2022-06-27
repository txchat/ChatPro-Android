package com.fzm.chat.core.processor

import com.fzm.arch.connection.processor.Processor
import com.fzm.chat.core.rtc.data.RTCProto
import com.fzm.chat.core.rtc.RTCSignalingManager
import dtalk.biz.signal.Signaling

/**
 * @author zhengjy
 * @since 2021/04/26
 * Description:
 */
class RTCSignalProcessor : Processor<Signaling.Msg> {

    override suspend fun process(server: String, message: Signaling.Msg): Signaling.Msg? {
        when (Signaling.ActionType.forNumber(message.action)) {
            Signaling.ActionType.startCall -> {
                val payload = parseProtocol(Signaling.SignalStartCall::class.java, message.body)
                RTCSignalingManager.signalingChannel.send(
                    RTCProto(payload.traceId, 0, message.action)
                )
            }
            Signaling.ActionType.acceptCall -> {
                val payload = parseProtocol(Signaling.SignalAcceptCall::class.java, message.body)
                RTCSignalingManager.signalingChannel.send(
                    RTCProto(
                        payload.traceId, payload.roomId, message.action, payload.signature,
                        payload.privateMapKey, payload.sdkAppId
                    )
                )

            }
            Signaling.ActionType.stopCall -> {
                val payload = parseProtocol(Signaling.SignalStopCall::class.java, message.body)
                RTCSignalingManager.signalingChannel.send(
                    RTCProto(payload.traceId, 0, message.action, reason = payload.reasonValue)
                )
            }
        }
        return message
    }
}