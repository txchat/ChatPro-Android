package com.fzm.chat.group

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.view.View
import androidx.core.content.ContextCompat
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.core.data.bean.PreSendParams
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.base.shareGroupCode
import com.fzm.chat.biz.utils.ImageUtils.blur
import com.fzm.chat.biz.utils.onExplain
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.data.po.typeDesc
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.databinding.ActivityGroupQrCodeBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.QRCodeViewModel
import com.fzm.widget.span.RoundCornerBackgroundSpan
import com.king.zxing.util.CodeUtils
import com.permissionx.guolindev.PermissionX
import com.zjy.architecture.ext.*
import com.zjy.architecture.util.isAndroidQ
import com.zjy.architecture.util.other.BarUtils
import dtalk.biz.Biz
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.net.URLEncoder

@Route(path = MainModule.GROUP_QRCODE)
class GroupQRCodeActivity : BizActivity(), View.OnClickListener {

    @JvmField
    @Autowired
    var groupInfo: GroupInfo? = null
    private val delegate by inject<LoginDelegate>()
    private val viewModel by viewModel<QRCodeViewModel>()
    private val binding by init { ActivityGroupQrCodeBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        BarUtils.setStatusBarColor(this, resources.getColor(R.color.biz_color_accent), 0)
        BarUtils.setStatusBarLightMode(this, false)
        window?.navigationBarColor = resources.getColor(R.color.biz_color_primary)
    }


    @SuppressLint("SetTextI18n")
    override fun initView() {
        ARouter.getInstance().inject(this)
        viewModel.saveResult.observe(this) {
            if (it != null) {
                toast(R.string.chat_tips_img_saved)
            } else {
                toast(R.string.chat_tips_img_not_exist)
            }
        }

        groupInfo?.let {
            binding.ivAvatar.load(it.avatar, R.mipmap.default_avatar_room)
            val ssb = SpannableStringBuilder(it.getRawName())
            if (it.groupType == GroupInfo.TYPE_TEAM || it.groupType == GroupInfo.TYPE_DEPART) {
                val backgroundSpan = RoundCornerBackgroundSpan(
                    resources.getColor(R.color.biz_color_accent),
                    resources.getColor(R.color.biz_blue_tips_light),
                    4.dp.toFloat()
                )
                val sizeSpan = AbsoluteSizeSpan(12, true)
                if (it.groupType == GroupInfo.TYPE_TEAM) {
                    ssb.append(" 全员")
                } else if (it.groupType == GroupInfo.TYPE_DEPART) {
                    ssb.append(" 部门")
                }
                ssb.setSpan(backgroundSpan, ssb.length - 2, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                ssb.setSpan(sizeSpan, ssb.length - 2, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            binding.tvGroupName.text = ssb
            binding.tvGroupUid.text = getString(R.string.chat_tips_group_mark_id, it.markId)
            val url = AppConfig.shareGroupCode(
                gid = it.gid,
                server = URLEncoder.encode(it.server.address, Charsets.UTF_8.name()),
                inviterId = delegate.getAddress(),
                createTime = System.currentTimeMillis()
            )
            val bitmap = (ContextCompat.getDrawable(
                this,
                R.mipmap.ic_bitmap_chat33
            ) as BitmapDrawable?)?.bitmap
            binding.tipsTeamGroup.text = "此群属于团队${groupInfo?.typeDesc}群，仅组织内部成员可加入，如果组织外部人员要加入群，需要先申请加入该组织"

            if (it.joinType == GroupInfo.ONLY_INVITE_GROUP) {
                binding.tvForbidJoin.visible()
                binding.layoutAction.gone()
                binding.tvJoinTips.gone()
                binding.copyGroupNum.gone()
                binding.ivGroupQr.setImageBitmap(CodeUtils.createQRCode(url, 350, bitmap).blur())
            } else {
                binding.tvForbidJoin.gone()
                binding.layoutAction.visible()
                binding.tvJoinTips.visible()
                binding.copyGroupNum.visible()
                binding.ivGroupQr.setImageBitmap(CodeUtils.createQRCode(url, 350, bitmap))
            }
            binding.tipsTeamGroup.setVisible(it.groupType != GroupInfo.TYPE_NORMAL)
        }
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressed() }
        binding.copyGroupNum.setOnClickListener(this)
        binding.ivChatMessage.setOnClickListener(this)
        binding.ivSaveGroupQrcode.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.copy_group_num -> {
                groupInfo?.markId?.also {
                    val data = ClipData.newPlainText("GroupNum", it)
                    clipboardManager?.setPrimaryClip(data)
                    toast(R.string.chat_tips_copy_group_num)
                }
            }
            R.id.iv_chat_message -> {
                val message = MessageContent.contactCard(
                    2,
                    groupInfo!!.getId(),
                    groupInfo!!.getRawName(),
                    groupInfo!!.getDisplayImage(),
                    groupInfo!!.getServerList().firstOrNull()?.address,
                    viewModel.getAddress(),
                )
                val params = PreSendParams(Biz.MsgType.ContactCard_VALUE , message, null)
                ARouter.getInstance().build(MainModule.CONTACT_SELECT)
                    .withSerializable("preSend", params)
                    .navigation()
                finish()
            }
            R.id.iv_save_group_qrcode -> {
                if (isAndroidQ) {
                    viewModel.saveQRCode(this, getShareBitmap())
                } else {
                    PermissionX.init(this)
                        .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .onExplain(this)
                        .request { allGranted, _, _ ->
                            if (allGranted) {
                                viewModel.saveQRCode(this, getShareBitmap())
                            } else {
                                this.toast(com.fzm.chat.biz.R.string.biz_permission_not_granted)
                            }
                        }
                }
            }
        }
    }

    private fun getShareBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(
            binding.rlShareView.width,
            binding.rlShareView.height,
            Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bitmap)
        binding.rlShareView.draw(canvas)
        return bitmap
    }

}