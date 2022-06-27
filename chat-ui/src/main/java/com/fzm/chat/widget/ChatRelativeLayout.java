package com.fzm.chat.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

/**
 * @author zhengjy
 * @since 2021/03/25
 * Description:
 */
public class ChatRelativeLayout extends RelativeLayout {

    private boolean selectable = false;

    public ChatRelativeLayout(Context context) {
        super(context);
    }

    public ChatRelativeLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ChatRelativeLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return selectable || super.onInterceptTouchEvent(ev);
    }
}
