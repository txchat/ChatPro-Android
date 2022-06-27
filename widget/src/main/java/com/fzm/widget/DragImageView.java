package com.fzm.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * author: NYB
 * created on: 2020-04-14
 * description:
 */
public class DragImageView extends AppCompatImageView {

    private int parentHeight;
    private int parentWidth;
    private int slop;

    private int leftMargin;
    private int rightMargin;

    public DragImageView(Context context) {
        this(context, null);
    }

    public DragImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public DragImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        slop = ViewConfiguration.get(context).getScaledPagingTouchSlop();
    }


    private int lastX;
    private int lastY;

    private boolean isDrag;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int rawX = (int) event.getRawX();
        int rawY = (int) event.getRawY();
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                isDrag = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                lastX = rawX;
                lastY = rawY;
                ViewGroup parent;
                if (getParent() != null) {
                    parent = (ViewGroup) getParent();
                    parentHeight = parent.getHeight();
                    parentWidth = parent.getWidth();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (parentHeight <= 0 || parentWidth <= 0) {
                    //如果不存在父类的宽高则无法拖动，默认直接返回false
                    isDrag = false;
                    break;
                }
                int dx = rawX - lastX;
                int dy = rawY - lastY;
                //这里修复一些华为手机无法触发点击事件
                int distance = (int) Math.sqrt(dx * dx + dy * dy);
                //此处稍微增加一些移动的偏移量，防止手指抖动，误判为移动无法触发点击时间
                if (!isDrag && (distance == 0 || distance <= slop)) {
                    isDrag = false;
                    break;
                }
                //程序到达此处一定是正在拖动了
                isDrag = true;
                float x = getX() + dx;
                float y = getY() + dy;
                //检测是否到达边缘 左上右下
                x = x < 0 ? 0 : x > parentWidth - getWidth() ? parentWidth - getWidth() : x;
                y = getY() < 0 ? 0 : getY() + getHeight() > parentHeight ? parentHeight - getHeight() : y;
                setX(x);
                setY(y);
                lastX = rawX;
                lastY = rawY;
                break;
            case MotionEvent.ACTION_UP:
                if (isDrag()) {
                    //恢复按压效果
                    setPressed(false);
                }
                welt(rawX);
                break;
        }
        //如果是拖拽则消耗事件，否则正常传递即可。
        return isDrag() || super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) getLayoutParams();
        rightMargin = params.rightMargin;
        leftMargin = params.leftMargin;
    }

    private boolean isDrag() {
        return isDrag;
    }

    private boolean isLeftSide() {
        return getX() == 0;
    }

    private boolean isRightSide() {
        return getX() == parentWidth - getWidth();
    }

    private void welt(int currentX) {
        if (!isLeftSide() || !isRightSide()) {
            if (currentX >= parentWidth / 2) {
                //靠右吸附
                animate().setInterpolator(new DecelerateInterpolator())
                        .setDuration(300)
                        .xBy(parentWidth - getWidth() - getX() - rightMargin)
                        .start();
            } else {
                //靠左吸附
                ObjectAnimator oa = ObjectAnimator.ofFloat(this, "x", getX(), leftMargin);
                oa.setInterpolator(new DecelerateInterpolator());
                oa.setDuration(300);
                oa.start();
            }
        }

    }
}
