package com.fzm.chat.widget;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.fzm.chat.R;
import com.fzm.chat.core.data.ChatConst;
import java.lang.reflect.Field;

/**
 * @author zhengjy
 * @since 2021/06/07
 * Description:
 */
public class MutePopupWindow extends PopupWindow implements OnClickListener {

    protected Context context;
    protected View mRootView;

    private TextView cancel;
    private TextView confirm;
    private boolean isMuted;
    private TextView tvTitle;
    private TextView tv_count_down;
    private TextView tv_cancel_disable_speak;
    private TextView tv_select_time;
    private TextView tv_time_1;
    private TextView tv_time_2;
    private TextView tv_time_3;
    private TextView tv_time_4;
    private TextView tv_time_5;
    private TextView tv_time_6;

    private long selectTime;
    private String title;
    private int color;

    private OnTimeSelectListener onTimeSelectListener;

    public MutePopupWindow(Context context, View popupView) {
        super(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        this.context = context;
        mRootView = popupView;
        setOutsideTouchable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Field mLayoutInScreen = PopupWindow.class.getDeclaredField("mLayoutInScreen");
                mLayoutInScreen.setAccessible(true);
                mLayoutInScreen.set(this, true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        setBackgroundDrawable(new BitmapDrawable());
        setFocusable(true);
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        findView();
        initData();
    }

    @Override
    public void showAsDropDown(View anchor) {
        if (Build.VERSION.SDK_INT >= 24) {
            Rect rect = new Rect();
            anchor.getGlobalVisibleRect(rect);
            setHeight(anchor.getResources().getDisplayMetrics().heightPixels - rect.bottom);
        }
        super.showAsDropDown(anchor);
    }

    private void findView() {
        cancel = mRootView.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);
        confirm = mRootView.findViewById(R.id.confirm);
        confirm.setOnClickListener(this);
        tvTitle = mRootView.findViewById(R.id.tv_title);
        tv_count_down = mRootView.findViewById(R.id.tv_count_down);
        tv_time_1 = mRootView.findViewById(R.id.tv_time_1);
        tv_time_1.setOnClickListener(this);
        tv_time_2 = mRootView.findViewById(R.id.tv_time_2);
        tv_time_2.setOnClickListener(this);
        tv_time_3 = mRootView.findViewById(R.id.tv_time_3);
        tv_time_3.setOnClickListener(this);
        tv_time_4 = mRootView.findViewById(R.id.tv_time_4);
        tv_time_4.setOnClickListener(this);
        tv_time_5 = mRootView.findViewById(R.id.tv_time_5);
        tv_time_5.setOnClickListener(this);
        tv_time_6 = mRootView.findViewById(R.id.tv_time_6);
        tv_time_6.setOnClickListener(this);
        tv_cancel_disable_speak = mRootView.findViewById(R.id.tv_cancel_disable_speak);
        tv_cancel_disable_speak.setOnClickListener(this);
        tv_select_time = mRootView.findViewById(R.id.tv_select_time);
        tv_cancel_disable_speak.setText(R.string.chat_mute_cancel);
        title = context.getString(R.string.chat_confirm_mute);
        color = ContextCompat.getColor(context, R.color.biz_text_grey_dark);
    }

    public void setTitle(String title, int color) {
        this.title = title;
        this.color = color;
        tvTitle.setText(title);
        tvTitle.setTextColor(color);
    }

    public void setCountDownText(String text, boolean visible) {
        tv_count_down.setText(text);
        if (visible && tv_count_down.getVisibility() == View.GONE) {
            tv_count_down.setVisibility(View.VISIBLE);
        } else if (!visible && tv_count_down.getVisibility() == View.VISIBLE) {
            tv_count_down.setVisibility(View.GONE);
        }
    }

    public interface OnTimeSelectListener {
        void onTimeSelect(long time);
    }

    public void setOnTimeSelectListener(OnTimeSelectListener listener) {
        this.onTimeSelectListener = listener;
    }

    private void initData() {
        tvTitle.setText(title);
        tvTitle.setTextColor(color);
        tv_time_1.performClick();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.confirm) {
            dismiss();
            if (onTimeSelectListener != null) {
                onTimeSelectListener.onTimeSelect(selectTime);
            }
        } else if (v.getId() == R.id.cancel) {
            dismiss();
        } else if (v.getId() == R.id.tv_cancel_disable_speak) {
            dismiss();
            selectTime = 0;
            onTimeSelectListener.onTimeSelect(selectTime);
        } else if (v.getId() == R.id.tv_time_1) {
            selectTime = ChatConst.MUTE_FOREVER;
            switchSelect(1);
        } else if (v.getId() == R.id.tv_time_2) {
            selectTime = 24 * 60 * 60 * 1000L;
            switchSelect(2);
        } else if (v.getId() == R.id.tv_time_3) {
            selectTime = 10 * 60 * 60 * 1000L;
            switchSelect(3);
        } else if (v.getId() == R.id.tv_time_4) {
            selectTime = 2 * 60 * 60 * 1000L;
            switchSelect(4);
        } else if (v.getId() == R.id.tv_time_5) {
            selectTime = 30 * 60 * 1000L;
            switchSelect(5);
        } else if (v.getId() == R.id.tv_time_6) {
            selectTime = 10 * 60 * 1000L;
            switchSelect(6);
        }
    }

    public void showCancelButton(boolean show) {
        tv_cancel_disable_speak.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            tv_select_time.setText(R.string.chat_change_mute_time);
            tvTitle.setTextColor(ContextCompat.getColor(context, R.color.biz_color_accent));
        } else {
            tv_select_time.setText(R.string.chat_select_mute_time);
            tvTitle.setText(R.string.chat_confirm_mute);
            tvTitle.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
        }
    }

    private void switchSelect(int index){
        switch (index){
            case 1:
                tv_time_1.setBackgroundResource(R.drawable.bg_press_line_rect);
                tv_time_1.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                tv_time_2.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_2.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_3.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_3.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_4.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_4.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_5.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_5.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_6.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_6.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                break;
            case 2:
                tv_time_1.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_1.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_2.setBackgroundResource(R.drawable.bg_press_line_rect);
                tv_time_2.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                tv_time_3.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_3.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_4.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_4.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_5.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_5.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_6.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_6.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                break;
            case 3:
                tv_time_1.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_1.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_2.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_2.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_3.setBackgroundResource(R.drawable.bg_press_line_rect);
                tv_time_3.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                tv_time_4.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_4.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_5.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_5.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_6.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_6.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                break;
            case 4:
                tv_time_1.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_1.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_2.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_2.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_3.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_3.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_4.setBackgroundResource(R.drawable.bg_press_line_rect);
                tv_time_4.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                tv_time_5.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_5.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_6.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_6.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                break;
            case 5:
                tv_time_1.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_1.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_2.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_2.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_3.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_3.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_4.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_4.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_5.setBackgroundResource(R.drawable.bg_press_line_rect);
                tv_time_5.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                tv_time_6.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_6.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                break;
            case 6:
                tv_time_1.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_1.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_2.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_2.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_3.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_3.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_4.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_4.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_5.setBackgroundResource(R.drawable.bg_dark_line_rect);
                tv_time_5.setTextColor(ContextCompat.getColor(context, R.color.biz_text_grey_dark));
                tv_time_6.setBackgroundResource(R.drawable.bg_press_line_rect);
                tv_time_6.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                break;
            default:
                break;
        }
    }
}
