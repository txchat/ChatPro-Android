package com.fzm.chat.router.biz;

/**
 * @author zhengjy
 * @since 2021/10/27
 * Description:
 */
public interface ResultReceiver<T> {

    void setOnResultReceiver(OnResultListener<T> listener);

    interface OnResultListener<T> {

        void onResult(T result);
    }
}
