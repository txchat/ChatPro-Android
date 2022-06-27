package com.fzm.chat.conversation.adapter.msg

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.View
import androidx.core.view.ViewCompat
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.isSendType
import com.fzm.chat.databinding.LayoutMsgTransferBinding
import com.fzm.chat.router.main.SimpleTx
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.visible
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/08/11
 * Description:
 */
class ChatTransfer(listener: ChatMessageClickListener) : ChatBaseItem(listener) {

    override val itemViewType: Int
        get() = Biz.MsgType.Transfer_VALUE

    override fun setupView(holder: BaseViewHolder, item: ChatMessage) {
        val binding = getOrCreateBinding<LayoutMsgTransferBinding>(holder)
        if (item.isSendType) {
            binding.tvTransferTips.text = "发起一笔转账，点击查看"
        } else {
            binding.tvTransferTips.text = "你有一笔收款，点击查看"
        }
        if (item.msg.txStatus == SimpleTx.PENDING) {
            binding.tvTransferInfo.text = "查询中..."
            binding.tvTransferInfo.setTextColor(Color.parseColor("#80FFFFFF"))
        } else if (item.msg.txAmount == null) {
            binding.tvTransferInfo.text = "查询失败"
            binding.tvTransferInfo.setTextColor(Color.parseColor("#80FFFFFF"))
        } else if (item.msg.txStatus == SimpleTx.FAIL) {
            binding.tvTransferInfo.text = "交易失败"
            binding.tvTransferInfo.setTextColor(Color.parseColor("#80FFFFFF"))
        } else {
            if (item.msg.txInvalid) {
                binding.llTransfer.gone()
                binding.tvInvalid.visible()
            } else {
                binding.llTransfer.visible()
                binding.tvInvalid.gone()
                binding.tvTransferInfo.text = "${item.msg.txAmount}${item.msg.txSymbol}"
                binding.tvTransferInfo.setTextColor(Color.parseColor("#FFFFFF"))
            }
        }
    }

    override fun onActionDown(view: View, item: ChatMessage) {
        val colors = ColorStateList.valueOf(context.resources.getColor(R.color.biz_wallet_accent_pressed))
        ViewCompat.setBackgroundTintList(view, colors)
        ViewCompat.setBackgroundTintMode(view, PorterDuff.Mode.SRC_IN)
    }

    override fun onActionUp(view: View, item: ChatMessage) {
        ViewCompat.setBackgroundTintList(view, null)
    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutMsgTransferBinding>(holder).root
    }
}