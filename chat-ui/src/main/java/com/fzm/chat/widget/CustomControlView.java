package com.fzm.chat.widget;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import xyz.doikki.videoplayer.controller.ControlWrapper;
import xyz.doikki.videoplayer.controller.IControlComponent;
import xyz.doikki.videoplayer.player.VideoView;
import xyz.doikki.videoplayer.util.PlayerUtils;
import com.fzm.chat.R;

import static xyz.doikki.videoplayer.util.PlayerUtils.stringForTime;

/**
 * 点播底部控制栏
 */
public class CustomControlView extends FrameLayout implements IControlComponent, View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    protected ControlWrapper mControlWrapper;

    private TextView mTotalTime, mCurrTime;
    private ViewGroup mBottomContainer;
    private SeekBar mVideoProgress;
    private ImageView mPlayButton;
    private ViewGroup llAction;
    private ImageView mDownload;
    private ImageView mGallery;
    private ImageView mForward;
    private ImageView mClose;

    private boolean mIsDragging;

    public CustomControlView(@NonNull Context context) {
        super(context);
    }

    public CustomControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        LayoutInflater.from(getContext()).inflate(getLayoutId(), this, true);
        mBottomContainer = findViewById(R.id.bottom_container);
        mVideoProgress = findViewById(R.id.seekBar);
        mVideoProgress.setOnSeekBarChangeListener(this);
        mTotalTime = findViewById(R.id.total_time);
        mCurrTime = findViewById(R.id.curr_time);
        mPlayButton = findViewById(R.id.iv_play);
        mPlayButton.setOnClickListener(this);
        llAction = findViewById(R.id.ll_action);
        mGallery = findViewById(R.id.iv_gallery);
        mGallery.setOnClickListener(this);
        mDownload = findViewById(R.id.iv_download);
        mDownload.setOnClickListener(this);
        mForward = findViewById(R.id.iv_forward);
        mForward.setOnClickListener(this);
        mClose = findViewById(R.id.iv_close);
        mClose.setOnClickListener(this);

        //5.1以下系统SeekBar高度需要设置成WRAP_CONTENT
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mVideoProgress.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
    }

    protected int getLayoutId() {
        return R.layout.dkplayer_custom_control_view;
    }

    @Override
    public void attach(@NonNull ControlWrapper controlWrapper) {
        mControlWrapper = controlWrapper;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onVisibilityChanged(boolean isVisible, Animation anim) {
        if (isVisible) {
            mBottomContainer.setVisibility(VISIBLE);
        } else {
            mBottomContainer.setVisibility(GONE);
        }
        if (anim != null) {
            mBottomContainer.startAnimation(anim);
        }
    }

    @Override
    public void onPlayStateChanged(int playState) {

        switch (playState) {
            case VideoView.STATE_IDLE:
            case VideoView.STATE_PLAYBACK_COMPLETED:
                setVisibility(GONE);
                mVideoProgress.setProgress(0);
                mVideoProgress.setSecondaryProgress(0);
                break;
            case VideoView.STATE_START_ABORT:
            case VideoView.STATE_PREPARING:
            case VideoView.STATE_PREPARED:
            case VideoView.STATE_ERROR:
                setVisibility(GONE);
                break;
            case VideoView.STATE_PLAYING:
                mPlayButton.setSelected(true);
                mControlWrapper.show();
                setVisibility(VISIBLE);
                //开始刷新进度
                mControlWrapper.startProgress();
                break;
            case VideoView.STATE_PAUSED:
                mPlayButton.setSelected(false);
                break;
            case VideoView.STATE_BUFFERING:
            case VideoView.STATE_BUFFERED:
                mPlayButton.setSelected(mControlWrapper.isPlaying());
                break;
        }
    }

    @Override
    public void onPlayerStateChanged(int playerState) {
        Activity activity = PlayerUtils.scanForActivity(getContext());
        if (activity != null && mControlWrapper.hasCutout()) {
            int orientation = activity.getRequestedOrientation();
            int cutoutHeight = mControlWrapper.getCutoutHeight();
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                mBottomContainer.setPadding(0, 0, 0, 0);
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                mBottomContainer.setPadding(cutoutHeight, 0, 0, 0);
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                mBottomContainer.setPadding(0, 0, cutoutHeight, 0);
            }
        }
    }

    @Override
    public void setProgress(int duration, int position) {
        if (mIsDragging) {
            return;
        }

        if (mVideoProgress != null) {
            if (duration > 0) {
                mVideoProgress.setEnabled(true);
                int pos = (int) (position * 1.0 / duration * mVideoProgress.getMax());
                mVideoProgress.setProgress(pos);
            } else {
                mVideoProgress.setEnabled(false);
            }
            int percent = mControlWrapper.getBufferedPercentage();
            if (percent >= 95) { //解决缓冲进度不能100%问题
                mVideoProgress.setSecondaryProgress(mVideoProgress.getMax());
            } else {
                mVideoProgress.setSecondaryProgress(percent * 10);
            }
        }

        if (mTotalTime != null)
            mTotalTime.setText(stringForTime(duration));
        if (mCurrTime != null)
            mCurrTime.setText(stringForTime(position));
    }

    @Override
    public void onLockStateChanged(boolean isLocked) {
        onVisibilityChanged(!isLocked, null);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fullscreen) {
            toggleFullScreen();
        } else if (id == R.id.iv_play) {
            mControlWrapper.togglePlay();
        } else if (id == R.id.iv_gallery) {
            if (mOnControlEventListener != null) {
                mOnControlEventListener.openGallery();
            }
        } else if (id == R.id.iv_download) {
            if (mOnControlEventListener != null) {
                mOnControlEventListener.saveVideo(v);
            }
        } else if (id == R.id.iv_forward) {
            if (mOnControlEventListener != null) {
                mOnControlEventListener.forwardVideo();
            }
        } else if (id == R.id.iv_close) {
            if (mOnControlEventListener != null) {
                mOnControlEventListener.close();
            }
        }
    }

    public void setGalleryVisible(boolean visible) {
        if (visible) {
            mGallery.setVisibility(VISIBLE);
        } else {
            mGallery.setVisibility(GONE);
        }
    }

    public void setOptionsVisible(boolean visible) {
        if (visible) {
            llAction.setVisibility(VISIBLE);
        } else {
            llAction.setVisibility(GONE);
        }
    }

    /**
     * 横竖屏切换
     */
    private void toggleFullScreen() {
        Activity activity = PlayerUtils.scanForActivity(getContext());
        mControlWrapper.toggleFullScreen(activity);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mIsDragging = true;
        mControlWrapper.stopProgress();
        mControlWrapper.stopFadeOut();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        long duration = mControlWrapper.getDuration();
        long newPosition = (duration * seekBar.getProgress()) / mVideoProgress.getMax();
        mControlWrapper.seekTo((int) newPosition);
        mIsDragging = false;
        mControlWrapper.startProgress();
        mControlWrapper.startFadeOut();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }

        long duration = mControlWrapper.getDuration();
        long newPosition = (duration * progress) / mVideoProgress.getMax();
        if (mCurrTime != null)
            mCurrTime.setText(stringForTime((int) newPosition));

    }

    private OnControlEventListener mOnControlEventListener;

    public void setOnControlEventListener(OnControlEventListener listener) {
        this.mOnControlEventListener = listener;
        if (mOnControlEventListener.showDownload()) {
            mDownload.setVisibility(VISIBLE);
        } else {
            mDownload.setVisibility(GONE);
        }
    }

    public ControlWrapper getController() {
        return mControlWrapper;
    }

    public interface OnControlEventListener {
        boolean showDownload();

        void openGallery();

        void saveVideo(View downloadView);

        void forwardVideo();

        void close();
    }
}
