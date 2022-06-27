package com.fzm.chat.media

import android.Manifest
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.utils.onExplain
import com.fzm.chat.biz.widget.FadePageTransformer
import com.fzm.chat.core.logic.MessageSubscription
import com.fzm.chat.databinding.ActivityFileManagementBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.ChatFileViewModel
import com.fzm.widget.ScrollPagerAdapter
import com.fzm.widget.dialog.EasyDialog
import com.permissionx.guolindev.PermissionX
import com.zjy.architecture.ext.setVisible
import com.zjy.architecture.ext.toast
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/03/26
 * Description:
 */
@Route(path = MainModule.FILE_MANAGEMENT)
class FileManagementActivity : BizActivity() {

    @JvmField
    @Autowired
    var target: String? = null

    @JvmField
    @Autowired
    var channelType: Int = 0

    @JvmField
    @Autowired
    var index = 0

    private val viewModel by viewModel<ChatFileViewModel>()
    private val binding by init { ActivityFileManagementBinding.inflate(layoutInflater) }

    private lateinit var fileFragment: ChatFileFragment
    private lateinit var mediaFragment: ChatMediaFragment

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        target?.also {
            binding.vpFile.apply {
                fileFragment = ChatFileFragment.create(it, channelType)
                mediaFragment = ChatMediaFragment.create(it, channelType)
                adapter = ScrollPagerAdapter(
                    supportFragmentManager,
                    listOf(
                        getString(R.string.chat_title_chat_file),
                        getString(R.string.chat_title_chat_media)
                    ),
                    listOf(fileFragment, mediaFragment)
                )
                setPageTransformer(false, FadePageTransformer())
                offscreenPageLimit = 2
            }
        }
        binding.tabLayout.setViewPager(binding.vpFile)
        binding.tabLayout.onPageSelected(binding.vpFile.currentItem)
        binding.vpFile.currentItem = index
    }

    override fun initData() {
        viewModel.chooseMode.observe(this) {
            if (it) {
                binding.ctbTitle.setRightText(getString(R.string.biz_cancel))
            } else {
                binding.ctbTitle.setRightText(getString(R.string.chat_action_file_select))
            }
            viewModel.clearSelect()
            binding.llSelectOptions.setVisible(it)
        }
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.downloadResult.observe(this) {
            viewModel.switchChooseMode()
            toast(getString(R.string.chat_tips_file_download_to, it))
        }
        viewModel.deleteResult.observe(this) { messages ->
            viewModel.switchChooseMode()
            messages.forEach { MessageSubscription.onDeleteMessage(it) }
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressed() }
        binding.ctbTitle.setOnRightClickListener {
            viewModel.switchChooseMode()
        }
        binding.llDownload.setOnClickListener {
            PermissionX.init(this)
                .permissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .onExplain(this)
                .request { allGranted, _, _ ->
                    if (allGranted) {
                        viewModel.downloadSelected()
                    } else {
                        toast(R.string.permission_not_granted)
                    }
                }
        }
        binding.llDelete.setOnClickListener {
            EasyDialog.Builder()
                .setHeaderTitle(getString(R.string.biz_tips))
                .setContent(getString(R.string.chat_dialog_tips_delete_chat_files))
                .setBottomLeftText(getString(R.string.biz_cancel))
                .setBottomRightText(getString(R.string.biz_confirm))
                .setBottomRightClickListener {
                    it.dismiss()
                    viewModel.deleteSelected()
                }
                .create(this)
                .show()
        }
    }

    override fun onBackPressedSupport() {
        if (viewModel.selectable) {
            viewModel.switchChooseMode()
            return
        }
        super.onBackPressedSupport()
    }
}