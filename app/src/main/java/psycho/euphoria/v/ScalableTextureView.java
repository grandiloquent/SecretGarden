package psycho.euphoria.v;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;

public class ScalableTextureView extends TextureView {

    private static final int INVALID_POINTER_ID = -1;

    private float mScaleFactor = 1.f;
    private float mLastTouchX;
    private float mLastTouchY;
    private int mActivePointerId = INVALID_POINTER_ID;
    private ScaleGestureDetector mScaleDetector;
    private Matrix mMatrix;

    public ScalableTextureView(Context context) {
        this(context, null);
    }

    public ScalableTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScalableTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mMatrix = new Matrix();
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleDetector.onTouchEvent(event);

                final int action = event.getAction();
                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN: {
                        final int pointerIndex = event.getActionIndex();
                        final float x = event.getX(pointerIndex);
                        final float y = event.getY(pointerIndex);

                        mLastTouchX = x;
                        mLastTouchY = y;
                        mActivePointerId = event.getPointerId(pointerIndex);
                        break;
                    }

                    case MotionEvent.ACTION_MOVE: {
                        final int pointerIndex = event.findPointerIndex(mActivePointerId);
                        if (pointerIndex == -1) {
                            break;
                        }

                        final float x = event.getX(pointerIndex);
                        final float y = event.getY(pointerIndex);

                        // Only move if the ScaleGestureDetector isn't processing a gesture.
                        if (!mScaleDetector.isInProgress()) {
                            float dx = x - mLastTouchX;
                            float dy = y - mLastTouchY;

                            mMatrix.postTranslate(dx, dy);
                            setTransform(mMatrix);
                            invalidate();

                        }

                        mLastTouchX = x;
                        mLastTouchY = y;
                        break;
                    }

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mActivePointerId = INVALID_POINTER_ID;
                        break;

                    case MotionEvent.ACTION_POINTER_UP: {
                        final int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                        final int pointerId = event.getPointerId(pointerIndex);
                        if (pointerId == mActivePointerId) {
                            // This was our active pointer going up. Choose a new
                            // active pointer and adjust accordingly.
                            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                            mLastTouchX = event.getX(newPointerIndex);
                            mLastTouchY = event.getY(newPointerIndex);
                            mActivePointerId = event.getPointerId(newPointerIndex);
                        }
                        break;
                    }
                }
                return true;
            }
        });
    }

//    @Override
//    public void draw(Canvas canvas) {
//        canvas.setMatrix(mMatrix);
//        super.draw(canvas);
//    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

            mMatrix.setScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
            setTransform(mMatrix);
            invalidate();
            return true;
        }
    }
}