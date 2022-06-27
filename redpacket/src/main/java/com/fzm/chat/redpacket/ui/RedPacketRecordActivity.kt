package com.fzm.chat.redpacket.ui

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.alibaba.android.arouter.facade.annotation.Route
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.flyco.tablayout.listener.OnTabSelectListener
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.redpacket.R
import com.fzm.chat.redpacket.data.bean.ReceiveInfo
import com.fzm.chat.redpacket.data.bean.RedPacketInfo
import com.fzm.chat.redpacket.data.model.ChooseTimeBean
import com.fzm.chat.redpacket.databinding.ActivityRedPacketRecordBinding
import com.fzm.chat.router.redpacket.ModuleAsset
import com.fzm.chat.router.redpacket.RedPacketModule
import com.noober.background.drawable.DrawableCreator
import com.pl.wheelview.WheelView
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.notchSupport
import com.zjy.architecture.ext.visible
import com.zjy.architecture.util.other.BarUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author zhengjy
 * @since 2021/08/30
 * Description:
 */
@SuppressLint("SimpleDateFormat")
@Route(path = RedPacketModule.RED_PACKET_RECORD)
class RedPacketRecordActivity : BizActivity(), View.OnClickListener {

    companion object {
        private const val allYear: String = "全年"
        private const val allCoin: String = "全部"

        const val TYPE_RECEIVE_PACKET = 2
        const val TYPE_SEND_PACKET = 1
    }


    private lateinit var hashMap: HashMap<Int, MutableList<ChooseTimeBean>>

    private val yearList: ArrayList<Int> = ArrayList()
    private val yearStrList: ArrayList<String> = ArrayList()
    private var monthList: ArrayList<String> = ArrayList()

    private var currentDate: String = ""
    private var tempDate: String = ""
    private val sdf = SimpleDateFormat("yyyy/MM")

    private var currentYear = 2017
    private var currentMonth = allYear

    private var currentAsset: ModuleAsset? = null
    private var tempAsset = currentAsset
    private lateinit var mAdapter: BaseQuickAdapter<ModuleAsset?, BaseViewHolder>

    /**
     * 确定保存过滤器
     */
    private var confirmFilter = false

    private lateinit var receiveFragment: PacketRecordFragment<ReceiveInfo>
    private lateinit var sendFragment: PacketRecordFragment<RedPacketInfo>

    private val binding by init<ActivityRedPacketRecordBinding>()

    private val viewModel by viewModel<RedPacketViewModel>()

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        notchSupport(window)
        BarUtils.setStatusBarColor(this, ContextCompat.getColor(this, android.R.color.transparent), 0)
        BarUtils.addPaddingTopEqualStatusBarHeight(this, binding.rlTitle)
        BarUtils.setStatusBarLightMode(this, false)
        window?.navigationBarColor = ContextCompat.getColor(this, R.color.biz_color_primary)
    }

    override fun initView() {
        val sdf = SimpleDateFormat("yyyy")
        currentDate = sdf.format(System.currentTimeMillis()).also { tempDate = it }
        binding.tvDate.text = currentDate
        val c = Calendar.getInstance()
        val date = currentDate.split("/").toTypedArray()
        val requestMonth: String
        if (date.size == 2) {
            currentYear = date[0].toInt()
            currentMonth = "${date[1]}月"
            requestMonth = "${date[0]}_${date[1]}"
            c[date[0].toInt(), date[1].toInt() - 1] = 1
        } else {
            currentYear = date[0].toInt()
            currentMonth = allYear
            requestMonth = ""
            c[date[0].toInt(), 1] = 1
        }
        receiveFragment = PacketRecordFragment.create(TYPE_RECEIVE_PACKET, currentYear.toString(), requestMonth)
        sendFragment = PacketRecordFragment.create(TYPE_SEND_PACKET, currentYear.toString(), requestMonth)
        binding.stlTitle.setTabData(arrayOf("收到红包", "发出红包"), this, R.id.fcv_container, arrayListOf(receiveFragment, sendFragment))
        binding.stlTitle.setOnTabSelectListener(object : OnTabSelectListener {
            override fun onTabSelect(position: Int) {
                binding.stlTitle.currentTab = position
            }

            override fun onTabReselect(position: Int) {}
        })

        syncStatus(false)
        initDateInfo()
    }

    override fun initData() {
        binding.recordDrawer.addDrawerListener(object : DrawerLayout.DrawerListener{
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                
            }

            override fun onDrawerOpened(drawerView: View) {
                BarUtils.setStatusBarLightMode(instance, true)
            }

            override fun onDrawerClosed(drawerView: View) {
                BarUtils.setStatusBarLightMode(instance, false)
                syncStatus(confirmFilter)
                if (confirmFilter) {
                    val date = currentDate.split("/").toTypedArray()
                    val requestMonth: String
                    if (date.size == 2) {
                        currentYear = date[0].toInt()
                        currentMonth = "${date[1]}月"
                        requestMonth = "${date[0]}_${date[1]}"
                    } else {
                        currentYear = date[0].toInt()
                        currentMonth = allYear
                        requestMonth = ""
                    }
                    binding.tvDate.text = currentDate
                    getStatisticInfo(currentYear, requestMonth)
                    confirmFilter = false
                }
                showYear(false)
            }

            override fun onDrawerStateChanged(newState: Int) {
                
            }
        })

        viewModel.packetAsset.observe(this) {
            if (it.isEmpty()) return@observe
            setRedPacketSymbolData(it)
        }

        viewModel.getRedPacketAssets()
    }

    private fun setRedPacketSymbolData(list: List<ModuleAsset>) {
        binding.drawerContent.tvCoinShow.visible()
        binding.drawerContent.rvCoin.visible()

        val dataList = mutableListOf<ModuleAsset?>().apply {
            add(null)
            addAll(list)
        }

        mAdapter = object : BaseQuickAdapter<ModuleAsset?, BaseViewHolder>(R.layout.item_coin_filter, dataList) {
            override fun convert(holder: BaseViewHolder, item: ModuleAsset?) {
                holder.setText(R.id.coin_type, item?.symbol?: allCoin)
                holder.setTextColor(R.id.coin_type, if (tempAsset == item) resources.getColor(R.color.biz_red_tips) else resources.getColor(R.color.biz_text_grey_dark))
                val drawable = DrawableCreator.Builder().setCornersRadius(5f.dp * 1f)
                    .setSolidColor(if (tempAsset == item) resources.getColor(R.color.biz_wallet_coin_bg5) else resources.getColor(R.color.biz_color_primary_dark))
                    .build()
                holder.getView<TextView>(R.id.coin_type).background = drawable
            }
        }

        mAdapter.setOnItemClickListener { _, _, position ->
            tempAsset = mAdapter.data[position]
            mAdapter.notifyDataSetChanged()
        }
        binding.drawerContent.rvCoin.adapter = mAdapter
    }

    private fun getStatisticInfo(year: Int, month: String) {
        receiveFragment.getStatisticInfo(year = year.toString(), month = month, asset = currentAsset)
        sendFragment.getStatisticInfo(year = year.toString(), month = month, asset = currentAsset)
    }

    private fun syncStatus(confirm: Boolean) {
        if (confirm) {
            currentDate = tempDate
            currentAsset = tempAsset
        } else {
            tempDate = currentDate
            tempAsset = currentAsset
        }
        if (this@RedPacketRecordActivity::mAdapter.isInitialized) {
            mAdapter.notifyDataSetChanged()
        }
    }

    override fun onBackPressedSupport() {
        if (binding.recordDrawer.isDrawerOpen(GravityCompat.END)) {
            binding.recordDrawer.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressedSupport()
        }
    }

    override fun setEvent() {
        binding.ivBack.setOnClickListener(this)
        binding.tvDate.setOnClickListener(this)
        binding.drawerContent.reset.setOnClickListener(this)
        binding.drawerContent.confirm.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_back -> onBackPressed()
            R.id.tv_date -> {
                binding.recordDrawer.openDrawer(GravityCompat.END)
            }
            R.id.reset->{
                currentDate = SimpleDateFormat("yyyy").format(System.currentTimeMillis()).also {
                    tempDate = it
                }
                showYear(true)

                tempAsset = null
                if (this::mAdapter.isInitialized) mAdapter.notifyDataSetChanged()
            }
            R.id.confirm->{
                confirmFilter = true
                binding.recordDrawer.closeDrawer(GravityCompat.END)
            }
        }
    }


    private fun initDateInfo() {
        hashMap = getMonthBetween("2017/01", sdf.format(System.currentTimeMillis()))
        for ((key) in hashMap.entries) {
            yearList.add(key)
            hashMap[key]?.reverse()
        }
        yearList.reverse()
        binding.drawerContent.wvYear.setOnSelectListener(object : WheelView.OnSelectListener {
            override fun endSelect(id: Int, text: String) {
                tempDate = yearList[id].toString()
                showMonth(yearList[id], true)
            }

            override fun selecting(id: Int, text: String) {}
        })
        binding.drawerContent.wvMonth.setOnSelectListener(object : WheelView.OnSelectListener {
            override fun endSelect(id: Int, text: String) {
                if (id == 0) {
                    tempDate = yearList[id].toString()
                } else {
                    val value = id - 1
                    if (binding.drawerContent.wvYear.selected == -1) {
                        return
                    }
                    val list = hashMap[yearList[binding.drawerContent.wvYear.selected]]
                    if (list != null && value < list.size) {
                        tempDate = sdf.format(list[value].data)
                    }
                }
            }

            override fun selecting(id: Int, text: String) {}
        })
        if (yearList.size > 0) {
            for (year in yearList) {
                yearStrList.add(year.toString())
            }
        }
        showYear(true)
    }

    private fun showYear(reset: Boolean) {
        var currentItem: Int = yearList.indexOf(currentYear)
        if (currentItem < 0 || reset) {
            currentItem = 0
        }
        binding.drawerContent.wvYear.setData(yearStrList)
        binding.drawerContent.wvYear.setDefault(currentItem)
        showMonth(yearList[currentItem], reset)
    }

    private fun showMonth(year: Int, reset: Boolean) {
        monthList = getMonth(year)
        var currentItem: Int = monthList.indexOf(currentMonth)
        if (currentItem < 0 || reset) {
            currentMonth = allYear
            currentItem = 0
        }
        binding.drawerContent.wvMonth.setData(monthList)
        binding.drawerContent.wvMonth.setDefault(currentItem)
    }

    private fun getMonth(year: Int): ArrayList<String> {
        val list = hashMap[year]
        val monthL = ArrayList<String>()
        monthL.add(allYear)
        if (list != null) {
            for (i in list.indices) {
                monthL.add(list[i].showTime)
            }
        }
        return monthL
    }

    /**
     * 获取时间段内所有的年月集合
     *
     * @param minDate 最小时间  2017/01
     * @param maxDate 最大时间 2017/10
     * @return 日期集合 格式为 key年,value月
     * @throws Exception
     */
    private fun getMonthBetween(
        minDate: String?,
        maxDate: String?
    ): HashMap<Int, MutableList<ChooseTimeBean>> {
        val result = HashMap<Int, MutableList<ChooseTimeBean>>()
        val sdf = SimpleDateFormat("yyyy/MM")
        val min = Calendar.getInstance()
        val max = Calendar.getInstance()
        try {
            min.time = sdf.parse(minDate)
            min.set(min[Calendar.YEAR], min[Calendar.MONTH], 1)
            max.time = sdf.parse(maxDate)
            max.set(max[Calendar.YEAR], max[Calendar.MONTH], 2)

            val curr = min
            while (curr.before(max)) {
                val list = result[curr.get(Calendar.YEAR)] ?: ArrayList<ChooseTimeBean>()
                val date = curr.timeInMillis
                val showTime = if (curr[Calendar.MONTH] + 1 < 10) {
                    "0${curr[Calendar.MONTH] + 1}月"
                } else {
                    "${curr[Calendar.MONTH] + 1}月"
                }
                list.add(ChooseTimeBean(date, showTime))
                result[curr.get(Calendar.YEAR)] = list
                curr.add(Calendar.MONTH, 1)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return result
    }
}