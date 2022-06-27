package com.fzm.chat.wallet.ui.coins

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.listener.OnItemDragListener
import com.fzm.wallet.sdk.BWallet

/**
 * @author zhengjy
 * @since 2022/03/01
 * Description:钱包页币种列表编辑
 */
class HomeCoinFragment : AbstractCoinFragment() {

    companion object {
        fun create(): HomeCoinFragment {
            return HomeCoinFragment()
        }
    }

    private var startPos = 0

    override fun setupCoinDrag() {
        adapter.draggableModule.isDragEnabled = true
        adapter.draggableModule.isDragOnLongPressEnabled = true
        adapter.draggableModule.itemTouchHelperCallback.setDragMoveFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN)
        adapter.draggableModule.setOnItemDragListener(object : OnItemDragListener {
            override fun onItemDragStart(viewHolder: RecyclerView.ViewHolder?, pos: Int) {
                startPos = pos
            }

            override fun onItemDragMoving(
                source: RecyclerView.ViewHolder?,
                from: Int,
                target: RecyclerView.ViewHolder?,
                to: Int
            ) {
            }

            override fun onItemDragEnd(viewHolder: RecyclerView.ViewHolder?, pos: Int) {
                if (startPos < pos) {
                    for (i in startPos..pos) {
                        adapter.data[i].apply {
                            sort = i
                            BWallet.get().changeCoinOrder(this, i)
                        }
                    }
                } else if (startPos > pos) {
                    for (i in pos..startPos) {
                        adapter.data[i].apply {
                            sort = i
                            BWallet.get().changeCoinOrder(this, i)
                        }
                    }
                }
            }
        })
    }

    override fun loadCoins() {
        viewModel.homeCoins.observe(viewLifecycleOwner) { coins ->
            onCoinsUpdate(coins.sorted())
        }
    }
}