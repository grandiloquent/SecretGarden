package psycho.euphoria.v;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;


public class VideoForwardDrawable extends Drawable {

    public static final RectF rectTmp = new RectF();
    private final static int[] playPath = new int[]{10, 7, 26, 16, 10, 25};
    private final float density;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private Path path1 = new Path();
    private boolean leftSide;
    private boolean isRound;
    private Path clippingPath;
    private int lastClippingPath;
    private float animationProgress;
    private float enterAnimationProgress;
    private boolean animating;
    private boolean isOneShootAnimation;
    private boolean showing;
    private long lastAnimationTime;
    private VideoForwardDrawableDelegate delegate;
    private long time;
    private String timeStr;
    private float playScaleFactor = 1f;

    public VideoForwardDrawable(Context context, boolean isRound) {
        density = context.getResources().getDisplayMetrics().density;
        this.isRound = isRound;
        paint.setColor(0xffffffff);
        textPaint.setColor(0xffffffff);
        textPaint.setTextSize(dp(12));
        textPaint.setTextAlign(Paint.Align.CENTER);
        path1.reset();
        for (int a = 0; a < playPath.length / 2; a++) {
            if (a == 0) {
                path1.moveTo(dp(playPath[a * 2]), dp(playPath[a * 2 + 1]));
            } else {
                path1.lineTo(dp(playPath[a * 2]), dp(playPath[a * 2 + 1]));
            }
        }
        path1.close();
    }

    public void addTime(long time) {
        this.time += time;
        timeStr = (int) (this.time / 1000) + " 秒";
    }

    public boolean isAnimating() {
        return animating;
    }

    public void setColor(int value) {
        paint.setColor(value);
    }

    public void setDelegate(VideoForwardDrawableDelegate videoForwardDrawableDelegate) {
        delegate = videoForwardDrawableDelegate;
    }

    public void setLeftSide(boolean value) {
        if (leftSide == value && animationProgress >= 1.0f && isOneShootAnimation) {
            return;
        }
        if (leftSide != value) {
            time = 0;
            timeStr = null;
        }
        leftSide = value;
        startAnimation();
    }

    public void setOneShootAnimation(boolean isOneShootAnimation) {
        if (this.isOneShootAnimation != isOneShootAnimation) {
            this.isOneShootAnimation = isOneShootAnimation;
            timeStr = null;
            time = 0;
            animationProgress = 0f;
        }
    }

    public void setPlayScaleFactor(float playScaleFactor) {
        this.playScaleFactor = playScaleFactor;
        invalidate();
    }

    public void setShowing(boolean showing) {
        this.showing = showing;
        invalidate();
    }

    public void setTime(long dt) {
        time = dt;
        if (time >= 1000) {
            timeStr = (int) (time / 1000) + " 秒";
            ;
        } else {
            timeStr = null;
        }
    }

    public void startAnimation() {
        animating = true;
        animationProgress = 0.0f;
        invalidateSelf();
    }

    private int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    private void invalidate() {
        if (delegate != null) {
            delegate.invalidate();
        } else {
            invalidateSelf();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        android.graphics.Rect rect = getBounds();
        int x = rect.left + (rect.width() - getIntrinsicWidth()) / 2;
        int y = rect.top + (rect.height() - getIntrinsicHeight()) / 2;
        if (leftSide) {
            x -= rect.width() / 4 - dp(16);
        } else {
            x += rect.width() / 4 + dp(16);
        }
        canvas.save();
        if (isRound) {
            if (clippingPath == null) {
                clippingPath = new Path();
            }
            int clippingPathHash = rect.left + (rect.top << 8) + (rect.bottom << 16) + (rect.right << 24);
            if (lastClippingPath != clippingPathHash) {
                clippingPath.reset();
                rectTmp.set(rect);
                clippingPath.addOval(rectTmp, Path.Direction.CCW);
                lastClippingPath = clippingPathHash;
            }
            canvas.clipPath(clippingPath);
        } else {
            canvas.clipRect(rect.left, rect.top, rect.right, rect.bottom);
        }
        if (!isOneShootAnimation) {
            paint.setAlpha((int) (80 * enterAnimationProgress));
            textPaint.setAlpha((int) (255 * enterAnimationProgress));
        } else {
            if (animationProgress <= 0.7f) {
                paint.setAlpha((int) (80 * Math.min(1.0f, animationProgress / 0.3f)));
                textPaint.setAlpha((int) (255 * Math.min(1.0f, animationProgress / 0.3f)));
            } else {
                paint.setAlpha((int) (80 * (1.0f - (animationProgress - 0.7f) / 0.3f)));
                textPaint.setAlpha((int) (255 * (1.0f - (animationProgress - 0.7f) / 0.3f)));
            }
        }
        canvas.drawCircle(x + Math.max(rect.width(), rect.height()) / 4 * (leftSide ? -1 : 1), y + dp(16), Math.max(rect.width(), rect.height()) / 2, paint);
        canvas.restore();
        if (timeStr != null) {
            canvas.drawText(timeStr, x + getIntrinsicWidth() * (leftSide ? -1 : 1), y + getIntrinsicHeight() + dp(15), textPaint);
        }
        canvas.save();
        canvas.scale(playScaleFactor, playScaleFactor, x, y + getIntrinsicHeight() / 2f);
        if (leftSide) {
            canvas.rotate(180, x, y + getIntrinsicHeight() / 2);
        }
        canvas.translate(x, y);
        if (animationProgress <= 0.6f) {
            int a;
            if (animationProgress < 0.4f) {
                a = Math.min(255, (int) (255 * animationProgress / 0.2f));
            } else {
                a = (int) (255 * (1.0f - (animationProgress - 0.4f) / 0.2f));
            }
            if (!isOneShootAnimation) {
                a = (int) (a * enterAnimationProgress);
            }
            paint.setAlpha(a);
            canvas.drawPath(path1, paint);
        }
        canvas.translate(dp(18), 0);
        if (animationProgress >= 0.2f && animationProgress <= 0.8f) {
            float progress = animationProgress - 0.2f;
            int a;
            if (progress < 0.4f) {
                a = Math.min(255, (int) (255 * progress / 0.2f));
            } else {
                a = (int) (255 * (1.0f - (progress - 0.4f) / 0.2f));
            }
            if (!isOneShootAnimation) {
                a = (int) (a * enterAnimationProgress);
            }
            paint.setAlpha(a);
            canvas.drawPath(path1, paint);
        }
        canvas.translate(dp(18), 0);
        if (animationProgress >= 0.4f && animationProgress <= 1.0f) {
            float progress = animationProgress - 0.4f;
            int a;
            if (progress < 0.4f) {
                a = Math.min(255, (int) (255 * progress / 0.2f));
            } else {
                a = (int) (255 * (1.0f - (progress - 0.4f) / 0.2f));
            }
            if (!isOneShootAnimation) {
                a = (int) (a * enterAnimationProgress);
            }
            paint.setAlpha(a);
            canvas.drawPath(path1, paint);
        }
        canvas.restore();
        if (animating) {
            long newTime = System.currentTimeMillis();
            long dt = newTime - lastAnimationTime;
            if (dt > 17) {
                dt = 17;
            }
            lastAnimationTime = newTime;
            if (animationProgress < 1.0f) {
                animationProgress += dt / 800.0f;
                if (!isOneShootAnimation) {
                    if (animationProgress >= 1.0f) {
                        if (showing) {
                            animationProgress = 0f;
                        } else {
                            animationProgress = 1f;
                        }
                    }
                } else {
                    if (animationProgress >= 1.0f) {
                        animationProgress = 0.0f;
                        animating = false;
                        time = 0;
                        timeStr = null;
                        if (delegate != null) {
                            delegate.onAnimationEnd();
                        }
                    }
                }
                invalidate();
            }
            if (!isOneShootAnimation) {
                if (showing && enterAnimationProgress != 1f) {
                    enterAnimationProgress += 16 / 150f;
                    invalidate();
                } else if (!showing && enterAnimationProgress != 0) {
                    enterAnimationProgress -= 16 / 150f;
                    invalidate();
                }
                if (enterAnimationProgress < 0) {
                    enterAnimationProgress = 0f;
                } else if (enterAnimationProgress > 1f) {
                    enterAnimationProgress = 1f;
                }
            }
        }
    }

    @Override
    public int getIntrinsicHeight() {
        return dp(32);
    }

    @Override
    public int getIntrinsicWidth() {
        return dp(32);
    }

    @Override
    public int getMinimumHeight() {
        return dp(32);
    }

    @Override
    public int getMinimumWidth() {
        return dp(32);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    public interface VideoForwardDrawableDelegate {
        void invalidate();

        void onAnimationEnd();
    }
}
