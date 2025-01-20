package psycho.euphoria.v;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.IOException;

import psycho.euphoria.v.TimeBar.OnScrubListener;

public class SimpleVideoView extends FrameLayout implements SurfaceTextureListener, OnScrubListener, GestureDetector2.OnGestureListener, GestureDetector2.OnDoubleTapListener {

    private final static float DOUBLE_TAP_SCALE_FACTOR = 1.5f;
    TextureView mTextureView;
    MediaPlayer mMediaPlayer;
    Surface mSurface;
    private int mVideoWidth;
    private int mVideoHeight;
    private SimpleTimeBar mTimeBar;

    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                if (mTextureView.getSurfaceTexture() != null)
                    mTextureView.getSurfaceTexture().setDefaultBufferSize(mVideoWidth, mVideoHeight);
                requestLayout();
            }
        }
    };
    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            mTimeBar.setDuration(mMediaPlayer.getDuration());
            mTimeBar.setPosition(0);
            mMediaPlayer.start();
            Log.e("B5aOx2", String.format("onPrepared, %s", mMediaPlayer.getVideoWidth()));
        }
    };
    String mVideoPath;
    GestureDetector2 gestureDetector;
    private int mWidth;
    private int mHeight;
    private int mLeft;
    private int mTop;
    private int mRight;
    private int mBottom;
    private int mOriginLeft;
    private int mOriginTop;
    private int mOriginRight;
    private int mOriginBottom;
    float mScale = 1.0f;
    private float mLastDownX;
    private float mLastDownY;
    boolean mInitialized;

    public SimpleVideoView(Context context) {
        super(context);
        gestureDetector = new GestureDetector2(context, this);
        gestureDetector.setIsLongpressEnabled(false);
        mTextureView = new TextureView(context);
        addView(mTextureView);
        mTextureView.setSurfaceTextureListener(this);
        mTimeBar = new SimpleTimeBar(context, null);
        mTimeBar.addListener(this);
        addView(mTimeBar);
    }

    public void play(String s) {
        mVideoPath = s;
    }

    public void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;

        }
    }

    private void openVideo() {
        release(false);
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
        mMediaPlayer.setOnPreparedListener(mPreparedListener);
        mMediaPlayer.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(getContext(), what + "", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        try {
            mMediaPlayer.setDataSource(mVideoPath);
        } catch (IOException e) {
        }
        mMediaPlayer.setSurface(mSurface);
        mMediaPlayer.prepareAsync();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mVideoWidth > 0) {
            float ratio = mVideoWidth / (mVideoHeight * 1.0f);
            int width = right - left;
            int height = bottom - top;
            int vh = (int) (width / ratio);
            mLeft = left;
            mTop = (height - vh) / 2;
            mBottom = (height - vh) / 2 + vh;
            mRight = right;
            mTextureView.layout(mLeft, mTop, mRight, mBottom);
            mTimeBar.layout(left + 20, height - 150, right - 20, height - 90);
            mOriginLeft = mLeft;
            mOriginTop = mTop;
            mOriginRight = mRight;
            mOriginBottom = mBottom;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        float xRatio = e.getX() / (getMeasuredWidth() * 1.0f);
        float yRatio = e.getY() / (getMeasuredHeight() * 1.0f);
        mWidth = mRight - mLeft;
        mHeight = mTop - mBottom;
        float difX = mWidth * mScale;
        float difY = mHeight * mScale;
        mLeft = (int) (mLeft - difX * xRatio);
        mRight = (int) (mRight + difX * (1 - xRatio));
        mTop = (int) (mTop + difY * yRatio);
        mBottom = (int) (mBottom - difY * (1 - yRatio));
        mTextureView.layout(mLeft, mTop, mRight, mBottom);
        mScale += .5f;
        if (mScale > 3.0) {
            mScale = 1.0f;
            mTextureView.layout(mOriginLeft,
                    mOriginTop,
                    mOriginRight,
                    mOriginBottom);
            mLeft = mOriginLeft;
            mTop = mOriginTop;
            mRight = mOriginRight;
            mBottom = mOriginBottom;
            mTextureView.layout(mLeft, mTop, mRight, mBottom);
        }
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onScrubMove(TimeBar timeBar, long position) {
        mMediaPlayer.seekTo((int) position);
    }

    @Override
    public void onScrubStart(TimeBar timeBar, long position) {
    }

    @Override
    public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
        mMediaPlayer.seekTo((int) position);
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurface = new Surface(surface);
        openVideo();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        release(true);
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mMediaPlayer.start();
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLastDownX = event.getX();
                mLastDownY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float difX = event.getX() - mLastDownX;
                float difY = event.getY() - mLastDownY;
                mLeft = (int) (mLeft + difX);
                mRight = (int) (mRight + difX);
                mTop = (int) (mTop + difY);
                mBottom = (int) (mBottom + difY);
                mTextureView.layout(mLeft, mTop, mRight, mBottom);
                mLastDownX = event.getX();
                mLastDownY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                mLastDownX = 0;
                mLastDownY = 0;
                break;
        }
        return true;
    }

    @Override
    public void onUp(MotionEvent e) {
    }

}
