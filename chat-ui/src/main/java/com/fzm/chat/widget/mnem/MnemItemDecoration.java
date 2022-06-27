package com.fzm.chat.widget.mnem;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by 42062 on 2018/7/25.
 */

public class MnemItemDecoration extends RecyclerView.ItemDecoration {
    int offset;

    @Override
    public void getItemOffsets(final Rect outRect, final View view, final RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int pos = parent.getChildAdapterPosition(view);
        if ((pos + 1) % 3 == 0 && (pos + 1) % 6 != 0) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    view.getRight();
                    offset = parent.getRight() - view.getRight() * 2 - dip2px(view.getContext(), 34);
                }
            });
            outRect.right = offset;

        }
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
