package com.fzm.chat.conversation.adapter

import com.chad.library.adapter.base.BaseProviderMultiAdapter
import com.fzm.chat.conversation.adapter.forward.*
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.ForwardMsg
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/06/23
 * Description:
 */
class ForwardAdapter(
    batchMsg: ChatMessage?,
    message: MutableList<ForwardMsg>,
    listener: ForwardBaseItem.ForwardMessageClickListener
) : BaseProviderMultiAdapter<ForwardMsg>(message) {

    init {
        addItemProvider(ForwardText(listener))
        addItemProvider(ForwardAudio(listener))
        addItemProvider(ForwardImage(batchMsg, listener))
        addItemProvider(ForwardVideo(listener))
        addItemProvider(ForwardFile(listener))
        addItemProvider(ForwardBatchMsg(listener))
        addItemProvider(ForwardRTC(listener))
        addItemProvider(ForwardContactCard(listener))
    }

    override fun getItemType(data: List<ForwardMsg>, position: Int): Int {
        val msgType = data[position].msgType
        val type = Biz.MsgType.forNumber(msgType)
        return if (type != null) {
            msgType
        } else {
            // 不支持的消息类型
            ChatConst.UNSUPPORTED_MSG_TYPE
        }
    }

    override fun addData(newData: Collection<ForwardMsg>) {
        this.data.addAll(newData)
        configMessageTime(this.data)
        notifyItemRangeChanged(this.data.size - newData.size + headerLayoutCount, newData.size)
        compatibilityDataSizeChanged(newData.size)
    }

    /**
     * 判断消息时间是否显示
     */
    private fun configMessageTime(list: List<ForwardMsg>?) {
        if (list == null) return
        var last: Long = 0
        list.forEachIndexed { i, msg ->
            if (i == 0) {
                last = msg.datetime
                msg.showTime = true
            } else {
                val cur: Long = msg.datetime
                if (cur - last > 60 * 10 * 1000) {
                    last = cur
                    msg.showTime = true
                } else {
                    msg.showTime = false
                }
            }
        }
    }
}