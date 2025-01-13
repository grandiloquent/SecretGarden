package psycho.euphoria.v;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.FrameLayout;

import java.io.IOException;

public class VideoActivity extends Activity {
    private final static long ZOOM_ANIMATION_DURATION = 300L;
    VideoView mVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVideoView = new VideoView(this);
        setContentView(mVideoView);
        mVideoView.play("/storage/emulated/0/Download/浙江反差眼镜妹 对白精彩身材露脸出镜超级反差.vv");
    }


    /*
     * what mean onMeasure android view
     * what mean onLayout android view
     * */
    public class VideoView extends FrameLayout implements SurfaceTextureListener {

        private final static float DOUBLE_TAP_SCALE_FACTOR = 1.5f;
        ScalableTextureView mTextureView;
        MediaPlayer mMediaPlayer;
        Surface mSurface;
        private int mVideoWidth;
        private int mVideoHeight;
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
                mMediaPlayer.start();
            }
        };
        String mVideoPath;


        public VideoView(Context context) {
            super(context);
            mTextureView = new ScalableTextureView(context);
            addView(mTextureView);
            mTextureView.setSurfaceTextureListener(this);


        }

        public void play(String s) {
            mVideoPath = s;
        }


        private void release(boolean cleartargetstate) {
            if (mMediaPlayer != null) {
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;

            }
        }


        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            if (mVideoWidth > 0) {
                float ratio = mVideoWidth / (mVideoHeight * 1.0f);
                int width = right - left;
                int height = bottom - top;
                int vh = (int) (width / ratio);
                mTextureView.layout(left, (height - vh) / 2, right, (height - vh) / 2 + vh);

            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }


        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurface = new Surface(surface);
            release(false);
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            try {
                mMediaPlayer.setDataSource(mVideoPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            mMediaPlayer.setSurface(mSurface);
            mMediaPlayer.prepareAsync();
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
            return true;
        }
    }
}