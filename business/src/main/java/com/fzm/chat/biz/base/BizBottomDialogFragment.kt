package com.fzm.chat.biz.base

import android.os.Bundle
import android.view.*
import com.fzm.chat.biz.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * @author zhengjy
 * @since 2021/03/19
 * Description:
 */
abstract class BizBottomDialogFragment : BottomSheetDialogFragment() {

    abstract val root: View

    /**
     * 设置Dialog，View，以及监听事件
     */
    abstract fun setupView(dialog: BottomSheetDialog)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(requireDialog() as BottomSheetDialog)
    }
}