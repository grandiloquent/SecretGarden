package psycho.euphoria.v;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class VideoView extends FrameLayout implements OnPreparedListener {
    private static final int DEFAULT_SCALING = 3;
    private static final int IDLE = 0;
    private static final int SCALE = 1;
    private static final int SCROLL = 2;
    private static final int TOLERANCE = 10;
    private final GestureDetector mGesturesDetector;
    private final ScaleGestureDetector mScaleDetector;
    private final TextureVideoView mTextureVideoView;
    private int mGestureState = IDLE;
    Matrix m = new Matrix();
    private float mLastScalePivotX;
    private float mLastScalePivotY;
    private int mWidth;
    private RectF mVideoRect;
    private RectF mViewportRectangle;
    private Listener mListener;

    public VideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTextureVideoView = new TextureVideoView(getContext());
        FrameLayout.LayoutParams layoutParams = new LayoutParams(getResources().getDisplayMetrics().widthPixels, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        addView(mTextureVideoView, layoutParams);
        //showPartiallyDecodedImage(true);
        GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                zoomAt(e.getX(), e.getY());
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (mGestureState == SCALE) {
                    return false;
                }
                mGestureState = SCROLL;
                m.postTranslate(-distanceX, -distanceY);
                mTextureVideoView.setAnimationMatrix(m);
                RectF endRect = new RectF();
                m.mapRect(endRect, mVideoRect);
                //Log.e("B5aOx2", String.format("snapBack, %s %s", mScaleDetector.getFocusX(), endRect));
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent ev) {
                if (mListener != null)
                    mListener.onVideoClick();
                return true;
            }
        };
        mGesturesDetector = new GestureDetector(getContext(), gestureListener);
        ScaleGestureDetector.OnScaleGestureListener scaleListener = new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                mLastScalePivotX = detector.getFocusX();
                mLastScalePivotY = detector.getFocusY();
                Matrix temporaryMatrix = new Matrix();
                temporaryMatrix.set(m);
                temporaryMatrix.postScale(scaleFactor, scaleFactor, mLastScalePivotX, mLastScalePivotY);
                float[] values = new float[9];
                temporaryMatrix.getValues(values);
                if (values[Matrix.MSCALE_X] < 1 || values[Matrix.MSCALE_Y] < 1) {
                    return true;
                }
                m = temporaryMatrix;
                mTextureVideoView.setAnimationMatrix(m);
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                mGestureState = SCALE;
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                mGestureState = IDLE;
                snapBack();
            }


        };
        mScaleDetector = new ScaleGestureDetector(getContext(), scaleListener);
        mTextureVideoView.setOnPreparedListener(this);
        mTextureVideoView.setOnTouchListener(this::onTouch);

    }

    public int getDuration() {
        return mTextureVideoView.getDuration();
    }

    public boolean onTouch(View view, MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            //endAnimation();
            mGestureState = IDLE;
        } else if (ev.getActionMasked() == MotionEvent.ACTION_UP && mGestureState == SCROLL) {
            snapBack();
            mGestureState = IDLE;
        } else if (ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            mGestureState = SCALE;
        }
        boolean ret = mGesturesDetector.onTouchEvent(ev);
        return mScaleDetector.onTouchEvent(ev) || ret;
    }

    public void playVideo(Uri uri) {
        mTextureVideoView.setVideoURI(uri);
        //mTextureVideoView.setVideoURI(Uri.parse("https://jvod.300hu.com/vod/product/9a7f27c0-e537-490a-9e24-ea2e3eaf35a6/999d6c2bf49c44d59013589923fe1be0.mp4?source=2&h265=h265/18799/04e80c7dfbc44aa78060737caa2f7fc2.mp4"));
        mTextureVideoView.start();
    }

    public void playVideo(String path) {
        mTextureVideoView.setVideoPath(path);
        //mTextureVideoView.setVideoURI(Uri.parse("https://jvod.300hu.com/vod/product/9a7f27c0-e537-490a-9e24-ea2e3eaf35a6/999d6c2bf49c44d59013589923fe1be0.mp4?source=2&h265=h265/18799/04e80c7dfbc44aa78060737caa2f7fc2.mp4"));
        mTextureVideoView.start();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void suspend() {
        mTextureVideoView.suspend();
    }

    private void snapBack() {
        RectF endRect = new RectF();
        // https://developer.android.com/reference/android/graphics/Matrix#mapRect(android.graphics.RectF,%20android.graphics.RectF)
        m.mapRect(endRect, mVideoRect);
        if (endRect.width() < getMeasuredWidth() && endRect.height() < getMeasuredHeight()) {
            m = new Matrix();
            mTextureVideoView.setAnimationMatrix(m);
            return;
        }
        float offsetX = 0;
        float offsetY = 0;
        if (endRect.left > 0) offsetX = -endRect.left;
        else {
            float[] values = new float[9];
            m.getValues(values);
            offsetX = Math.abs(values[Matrix.MTRANS_X]) - (values[Matrix.MSCALE_X] - 1) * getMeasuredWidth();
            if (offsetX < 1) return;
        }
        m.postTranslate(offsetX, offsetY);
        mTextureVideoView.setAnimationMatrix(m);
        endRect = new RectF();
        m.mapRect(endRect, mVideoRect);
    }

    public void seekTo(int msec) {
        mTextureVideoView.seekTo(msec);
    }

    private void zoomAt(float x, float y) {
        float[] values = new float[9];
        m.getValues(values);
        if(values[Matrix.MSCALE_X]>=DEFAULT_SCALING){
            m=new Matrix();
        }else {
            m.postScale(DEFAULT_SCALING, DEFAULT_SCALING, x, y);
        }
        mTextureVideoView.setAnimationMatrix(m);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mViewportRectangle = new RectF(left, top, right, bottom);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        int renderWidth = mTextureVideoView.getMeasuredWidth();
        int renderHeight = mTextureVideoView.getMeasuredHeight();
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();
        mVideoRect = new RectF(0, 0, videoWidth, videoHeight);
        if (mListener != null) {
            mListener.onDurationChange(mTextureVideoView.getDuration());
        }
    }

    //    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
//            Log.e("B5aOx2", String.format("[]: event.getX() = %s;\nevent.getY() = %s;\n", event.getX(), event.getY()));
//        }
//        return false;
//    }
    public interface Listener {
        void onDurationChange(int duration);

        void onVideoClick();

    }
}