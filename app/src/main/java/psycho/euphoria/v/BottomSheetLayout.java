package psycho.euphoria.v;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Property;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import java.util.concurrent.CopyOnWriteArraySet;


public class BottomSheetLayout extends FrameLayout {

    private static final long ANIMATION_DURATION = 300;
    private static final Property<BottomSheetLayout, Float> SHEET_TRANSLATION = new Property<BottomSheetLayout, Float>(Float.class, "sheetTranslation") {
        @Override
        public Float get(BottomSheetLayout object) {
            return object.sheetTranslation;
        }

        @Override
        public void set(BottomSheetLayout object, Float value) {
            object.setSheetTranslation(value);
        }
    };
    private Runnable runAfterDismiss;
    private Rect contentClipRect = new Rect();
    private State state = State.HIDDEN;
    private boolean peekOnDismiss = false;
    private TimeInterpolator animationInterpolator = new DecelerateInterpolator(1.6f);
    public boolean bottomSheetOwnsTouch;
    private boolean sheetViewOwnsTouch;
    private float sheetTranslation;
    private VelocityTracker velocityTracker;
    private float minFlingVelocity;
    private float touchSlop;
    private IdentityViewTransformer defaultViewTransformer = new IdentityViewTransformer();
    private IdentityViewTransformer viewTransformer;
    private boolean shouldDimContentView = true;
    private boolean useHardwareLayerWhileAnimating = true;
    private Animator currentAnimator;
    private CopyOnWriteArraySet<OnSheetDismissedListener> onSheetDismissedListeners = new CopyOnWriteArraySet<>();
    private CopyOnWriteArraySet<OnSheetStateChangeListener> onSheetStateChangeListeners = new CopyOnWriteArraySet<>();
    private OnLayoutChangeListener sheetViewOnLayoutChangeListener;
    private View dimView;
    private boolean interceptContentTouch = true;
    private int currentSheetViewHeight;
    private boolean hasIntercepted;
    private float peekKeyline;
    private float peek;
    /**
     * Some values we need to manage width on tablets
     */
    private int screenWidth = 0;
    private int sheetStartX = 0;
    private int sheetEndX = 0;
    /**
     * Snapshot of the touch's y position on a down event
     */
    private float downY;
    /**
     * Snapshot of the touch's x position on a down event
     */
    private float downX;
    /**
     * Snapshot of the sheet's translation at the time of the last down event
     */
    private float downSheetTranslation;
    /**
     * Snapshot of the sheet's state at the time of the last down event
     */
    private State downState;

    public BottomSheetLayout(Context context) {
        super(context);
        init();
    }

    public BottomSheetLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomSheetLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BottomSheetLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * Adds an {@link OnSheetDismissedListener} which will be notified when the state of the presented sheet changes.
     * The listener will not be automatically removed, so remember to remove it when it's no longer needed
     * (probably when the sheet is HIDDEN)
     *
     * @param onSheetDismissedListener the listener to be notified.
     */
    public void addOnSheetDismissedListener( OnSheetDismissedListener onSheetDismissedListener) {
        checkNotNull(onSheetDismissedListener, "onSheetDismissedListener == null");
        this.onSheetDismissedListeners.add(onSheetDismissedListener);
    }

    /**
     * Adds an {@link OnSheetStateChangeListener} which will be notified when the state of the presented sheet changes.
     * The listener will not be automatically removed, so remember to remove it when it's no longer needed
     * (probably when the sheet is HIDDEN)
     *
     * @param onSheetStateChangeListener the listener to be notified.
     */
    public void addOnSheetStateChangeListener( OnSheetStateChangeListener onSheetStateChangeListener) {
        checkNotNull(onSheetStateChangeListener, "onSheetStateChangeListener == null");
        this.onSheetStateChangeListeners.add(onSheetStateChangeListener);
    }

    /**
     * Dismiss the sheet currently being presented.
     */
    public void dismissSheet() {
        dismissSheet(null);
    }

    /**
     * Set the presented sheet to be in an expanded state.
     */
    public void expandSheet() {
        cancelCurrentAnimation();
        setSheetLayerTypeIfEnabled(LAYER_TYPE_NONE);
        ObjectAnimator anim = ObjectAnimator.ofFloat(this, SHEET_TRANSLATION, getMaxSheetTranslation());
        anim.setDuration(ANIMATION_DURATION);
        anim.setInterpolator(animationInterpolator);
        anim.addListener(new CancelDetectionAnimationListener() {
            @Override
            public void onAnimationEnd( Animator animation) {
                if (!canceled) {
                    currentAnimator = null;
                }
            }
        });
        anim.start();
        currentAnimator = anim;
        setState(State.EXPANDED);
    }

    /**
     * @return The currently presented sheet view. If no sheet is currently presented null will returned.
     */
    public View getContentView() {
        return getChildCount() > 0 ? getChildAt(0) : null;
    }

    /**
     * Set the content view of the bottom sheet. This is the view which is shown under the sheet
     * being presented. This is usually the root view of your application.
     *
     * @param contentView The content view of your application.
     */
    public void setContentView(View contentView) {
        super.addView(contentView, -1, generateDefaultLayoutParams());
        super.addView(dimView, -1, generateDefaultLayoutParams());
    }

    /**
     * @return true if we are intercepting content view touches or false to allow interaction with
     * Bottom Sheet's content view. Default value is true.
     */
    public boolean getInterceptContentTouch() {
        return interceptContentTouch;
    }

    /**
     * Controls whether or not child view interaction is possible when the bottomsheet is open.
     *
     * @param interceptContentTouch true to intercept content view touches or false to allow
     *                              interaction with Bottom Sheet's content view
     */
    public void setInterceptContentTouch(boolean interceptContentTouch) {
        this.interceptContentTouch = interceptContentTouch;
    }

    /**
     * @return The maximum translation for the presented sheet view. Translation is counted from the bottom of the view.
     */
    public float getMaxSheetTranslation() {
        return hasFullHeightSheet() ? getHeight() - getPaddingTop() : getSheetView().getHeight();
    }

    /**
     * Returns the current peekOnDismiss value, which controls the behavior response to back presses
     * when the current state is {@link State#EXPANDED}.
     *
     * @return the current peekOnDismiss value
     */
    public boolean getPeekOnDismiss() {
        return peekOnDismiss;
    }

    /**
     * Controls the behavior on back button press when the state is {@link State#EXPANDED}.
     *
     * @param peekOnDismiss true to show the peeked state on back press or false to completely hide
     *                      the Bottom Sheet. Default is false.
     */
    public void setPeekOnDismiss(boolean peekOnDismiss) {
        this.peekOnDismiss = peekOnDismiss;
    }

    /**
     * @return The peeked state translation for the presented sheet view. Translation is counted from the bottom of the view.
     */
    public float getPeekSheetTranslation() {
        return peek == 0 ? getDefaultPeekTranslation() : peek;
    }

    /**
     * Set custom height for PEEKED state.
     *
     * @param peek Peek height in pixels
     */
    public void setPeekSheetTranslation(float peek) {
        this.peek = peek;
    }

    /**
     * @return The currently presented sheet view. If no sheet is currently presented null will returned.
     */
    public View getSheetView() {
        return getChildCount() > 2 ? getChildAt(2) : null;
    }

    /**
     * @return The current state of the sheet.
     */
    public State getState() {
        return state;
    }

    private void setState(State state) {
        if (state != this.state) {
            this.state = state;
            for (OnSheetStateChangeListener onSheetStateChangeListener : onSheetStateChangeListeners) {
                onSheetStateChangeListener.onSheetStateChanged(state);
            }
        }
    }

    /**
     * @return Whether or not a sheet is currently presented.
     */
    public boolean isSheetShowing() {
        return state != State.HIDDEN;
    }


    /**
     * Set the presented sheet to be in a peeked state.
     */
    public void peekSheet() {
        cancelCurrentAnimation();
        setSheetLayerTypeIfEnabled(LAYER_TYPE_HARDWARE);
        ObjectAnimator anim = ObjectAnimator.ofFloat(this, SHEET_TRANSLATION, getPeekSheetTranslation());
        anim.setDuration(ANIMATION_DURATION);
        anim.setInterpolator(animationInterpolator);
        anim.addListener(new CancelDetectionAnimationListener() {
            @Override
            public void onAnimationEnd( Animator animation) {
                if (!canceled) {
                    currentAnimator = null;
                }
            }
        });
        anim.start();
        currentAnimator = anim;
        setState(State.PEEKED);
    }

    /**
     * Returns the predicted default width of the sheet if it were shown.
     *
     * @param context Context instance to retrieve resources and display metrics
     * @return Predicted width of the sheet if shown
     */
    public static int predictedDefaultWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * Removes a previously added {@link OnSheetDismissedListener}.
     *
     * @param onSheetDismissedListener the listener to be removed.
     */
    public void removeOnSheetDismissedListener( OnSheetDismissedListener onSheetDismissedListener) {
        checkNotNull(onSheetDismissedListener, "onSheetDismissedListener == null");
        this.onSheetDismissedListeners.remove(onSheetDismissedListener);
    }

    /**
     * Removes a previously added {@link OnSheetStateChangeListener}.
     *
     * @param onSheetStateChangeListener the listener to be removed.
     */
    public void removeOnSheetStateChangeListener( OnSheetStateChangeListener onSheetStateChangeListener) {
        checkNotNull(onSheetStateChangeListener, "onSheetStateChangeListener == null");
        this.onSheetStateChangeListeners.remove(onSheetStateChangeListener);
    }


    /**
     * Enable or disable dimming of the content view while a sheet is presented. If enabled a
     * transparent black dim is overlaid on top of the content view indicating that the sheet is the
     * foreground view. This dim is animated into place is coordination with the sheet view.
     * Defaults to true.
     *
     * @param shouldDimContentView whether or not to dim the content view.
     */
    public void setShouldDimContentView(boolean shouldDimContentView) {
        this.shouldDimContentView = shouldDimContentView;
    }

    /**
     * Enable or disable the use of a hardware layer for the presented sheet while animating.
     * This settings defaults to true and should only be changed if you know that putting the
     * sheet in a layer will negatively effect performance. One such example is if the sheet contains
     * a view which needs to frequently be re-drawn.
     *
     * @param useHardwareLayerWhileAnimating whether or not to use a hardware layer.
     */
    public void setUseHardwareLayerWhileAnimating(boolean useHardwareLayerWhileAnimating) {
        this.useHardwareLayerWhileAnimating = useHardwareLayerWhileAnimating;
    }

    /**
     * @return whether the content view is being dimmed while presenting a sheet or not.
     */
    public boolean shouldDimContentView() {
        return shouldDimContentView;
    }

    /**
     * Convenience for showWithSheetView(sheetView, null, null).
     *
     * @param sheetView The sheet to be presented.
     */
    public void showWithSheetView(View sheetView) {
        showWithSheetView(sheetView, null);
    }

    /**
     * Present a sheet view to the user.
     * If another sheet is currently presented, it will be dismissed, and the new sheet will be shown after that
     *
     * @param sheetView       The sheet to be presented.
     * @param viewTransformer The view transformer to use when presenting the sheet.
     */
    public void showWithSheetView(final View sheetView, final IdentityViewTransformer viewTransformer) {
        if (state != State.HIDDEN) {
            Runnable runAfterDismissThis = new Runnable() {
                @Override
                public void run() {
                    showWithSheetView(sheetView, viewTransformer);
                }
            };
            dismissSheet(runAfterDismissThis);
            return;
        }
        setState(State.PREPARING);
        LayoutParams params = (LayoutParams) sheetView.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL);
        }
        super.addView(sheetView, -1, params);
        initializeSheetValues();
        this.viewTransformer = viewTransformer;
        // Don't start animating until the sheet has been drawn once. This ensures that we don't do layout while animating and that
        // the drawing cache for the view has been warmed up. tl;dr it reduces lag.
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);
                post(new Runnable() {
                    @Override
                    public void run() {
                        // Make sure sheet view is still here when first draw happens.
                        // In the case of a large lag it could be that the view is dismissed before it is drawn resulting in sheet view being null here.
                        if (getSheetView() != null) {
                            peekSheet();
                        }
                    }
                });
                return true;
            }
        });
        // sheetView should always be anchored to the bottom of the screen
        currentSheetViewHeight = sheetView.getMeasuredHeight();
        sheetViewOnLayoutChangeListener = new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View sheetView, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int newSheetViewHeight = sheetView.getMeasuredHeight();
                if (state != State.HIDDEN) {
                    // The sheet can no longer be in the expanded state if it has shrunk
                    if (newSheetViewHeight < currentSheetViewHeight) {
                        if (state == State.EXPANDED) {
                            setState(State.PEEKED);
                        }
                        setSheetTranslation(newSheetViewHeight);
                    } else if (currentSheetViewHeight > 0 && newSheetViewHeight > currentSheetViewHeight && state == State.PEEKED) {
                        if (newSheetViewHeight == getMaxSheetTranslation()) {
                            setState(State.EXPANDED);
                        }
                        setSheetTranslation(newSheetViewHeight);
                    }
                }
                currentSheetViewHeight = newSheetViewHeight;
            }
        };
        sheetView.addOnLayoutChangeListener(sheetViewOnLayoutChangeListener);
    }

    private static <T> T checkNotNull(T value, String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
        return value;
    }

    private boolean canScrollUp(View view, float x, float y) {
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                int childLeft = child.getLeft() - view.getScrollX();
                int childTop = child.getTop() - view.getScrollY();
                int childRight = child.getRight() - view.getScrollX();
                int childBottom = child.getBottom() - view.getScrollY();
                boolean intersects = x > childLeft && x < childRight && y > childTop && y < childBottom;
                if (intersects && canScrollUp(child, x - childLeft, y - childTop)) {
                    return true;
                }
            }
        }
        return view.canScrollVertically(-1);
    }

    private void cancelCurrentAnimation() {
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }
    }

    private void dismissSheet(Runnable runAfterDismissThis) {
        if (state == State.HIDDEN) {
            runAfterDismiss = null;
            return;
        }
        // This must be set every time, including if the parameter is null
        // Otherwise a new sheet might be shown when the caller called dismiss after a showWithSheet call, which would be
        runAfterDismiss = runAfterDismissThis;
        final View sheetView = getSheetView();
        sheetView.removeOnLayoutChangeListener(sheetViewOnLayoutChangeListener);
        cancelCurrentAnimation();
        ObjectAnimator anim = ObjectAnimator.ofFloat(this, SHEET_TRANSLATION, 0);
        anim.setDuration(ANIMATION_DURATION);
        anim.setInterpolator(animationInterpolator);
        anim.addListener(new CancelDetectionAnimationListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!canceled) {
                    currentAnimator = null;
                    setState(State.HIDDEN);
                    setSheetLayerTypeIfEnabled(LAYER_TYPE_NONE);
                    removeView(sheetView);
                    for (OnSheetDismissedListener onSheetDismissedListener : onSheetDismissedListeners) {
                        onSheetDismissedListener.onDismissed(BottomSheetLayout.this);
                    }
                    // Remove sheet specific properties
                    viewTransformer = null;
                    if (runAfterDismiss != null) {
                        runAfterDismiss.run();
                        runAfterDismiss = null;
                    }
                }
            }
        });
        anim.start();
        currentAnimator = anim;
        sheetStartX = 0;
        sheetEndX = screenWidth;
    }

    private float getDefaultPeekTranslation() {
        return hasTallerKeylineHeightSheet() ? peekKeyline : getSheetView().getHeight();
    }

    private float getDimAlpha(float sheetTranslation) {
        if (viewTransformer != null) {
            return viewTransformer.getDimAlpha(sheetTranslation, getMaxSheetTranslation(), getPeekSheetTranslation(), this, getContentView());
        } else if (defaultViewTransformer != null) {
            return defaultViewTransformer.getDimAlpha(sheetTranslation, getMaxSheetTranslation(), getPeekSheetTranslation(), this, getContentView());
        }
        return 0;
    }

    private boolean hasFullHeightSheet() {
        return getSheetView() == null || getSheetView().getHeight() == getHeight();
    }

    private boolean hasTallerKeylineHeightSheet() {
        return getSheetView() == null || getSheetView().getHeight() > peekKeyline;
    }

    private void init() {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        minFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        touchSlop = viewConfiguration.getScaledTouchSlop();
        dimView = new View(getContext());
        dimView.setBackgroundColor(Color.BLACK);
        dimView.setAlpha(0);
        dimView.setVisibility(INVISIBLE);
        setFocusableInTouchMode(true);
        Point point = new Point();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(point);
        screenWidth = point.x;
        sheetEndX = screenWidth;
        peek = 0; //getHeight() return 0 at start!
        peekKeyline = point.y - (screenWidth / (16.0f / 9.0f));
    }

    /**
     * Set dim and translation to the initial state
     */
    private void initializeSheetValues() {
        this.sheetTranslation = 0;
        this.contentClipRect.set(0, 0, getWidth(), getHeight());
        getSheetView().setTranslationY(getHeight());
        dimView.setAlpha(0);
        dimView.setVisibility(INVISIBLE);
    }

    private boolean isAnimating() {
        return currentAnimator != null;
    }

    private boolean isXInSheet(float x) {
        return x >= sheetStartX && x <= sheetEndX;
    }

    private void setSheetLayerTypeIfEnabled(int layerType) {
        if (useHardwareLayerWhileAnimating) {
            getSheetView().setLayerType(layerType, null);
        }
    }

    private void setSheetTranslation(float newTranslation) {
        this.sheetTranslation = Math.min(newTranslation, getMaxSheetTranslation());
        int bottomClip = (int) (getHeight() - Math.ceil(sheetTranslation));
        this.contentClipRect.set(0, 0, getWidth(), bottomClip);
        getSheetView().setTranslationY(getHeight() - sheetTranslation);
        transformView(sheetTranslation);
        if (shouldDimContentView) {
            float dimAlpha = getDimAlpha(sheetTranslation);
            dimView.setAlpha(dimAlpha);
            dimView.setVisibility(dimAlpha > 0 ? VISIBLE : INVISIBLE);
        }
    }

    private void transformView(float sheetTranslation) {
        if (viewTransformer != null) {
            viewTransformer.transformView(sheetTranslation, getMaxSheetTranslation(), getPeekSheetTranslation(), this, getContentView());
        } else if (defaultViewTransformer != null) {
            defaultViewTransformer.transformView(sheetTranslation, getMaxSheetTranslation(), getPeekSheetTranslation(), this, getContentView());
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        velocityTracker = VelocityTracker.obtain();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        velocityTracker.clear();
        cancelCurrentAnimation();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int bottomClip = (int) (getHeight() - Math.ceil(sheetTranslation));
        this.contentClipRect.set(0, 0, getWidth(), bottomClip);
    }

    /**
     * Don't call addView directly, use setContentView() and showWithSheetView()
     */
    @Override
    public void addView( View child) {
        if (getChildCount() > 0) {
            throw new IllegalArgumentException("You may not declare more then one child of bottom sheet. The sheet view must be added dynamically with showWithSheetView()");
        }
        setContentView(child);
    }

    @Override
    public void addView( View child, int index) {
        addView(child);
    }

    @Override
    public void addView( View child, int index,  ViewGroup.LayoutParams params) {
        addView(child);
    }

    @Override
    public void addView( View child,  ViewGroup.LayoutParams params) {
        addView(child);
    }

    @Override
    public void addView( View child, int width, int height) {
        addView(child);
    }

    public boolean onInterceptTouchEvent( MotionEvent ev) {
        boolean downAction = ev.getActionMasked() == MotionEvent.ACTION_DOWN;
        if (downAction) {
            hasIntercepted = false;
        }
        if (interceptContentTouch || (ev.getY() > getHeight() - sheetTranslation && isXInSheet(ev.getX()))) {
            hasIntercepted = downAction && isSheetShowing();
        } else {
            hasIntercepted = false;
        }
        return hasIntercepted;
    }

    @Override
    public boolean onKeyPreIme(int keyCode,  KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && isSheetShowing()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                KeyEvent.DispatcherState state = getKeyDispatcherState();
                if (state != null) {
                    state.startTracking(event, this);
                }
                return true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                KeyEvent.DispatcherState dispatcherState = getKeyDispatcherState();
                if (dispatcherState != null) {
                    dispatcherState.handleUpEvent(event);
                }
                if (isSheetShowing() && event.isTracking() && !event.isCanceled()) {
                    if (state == State.EXPANDED && peekOnDismiss) {
                        peekSheet();
                    } else {
                        dismissSheet();
                    }
                    return true;
                }
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onTouchEvent( MotionEvent event) {
        if (!isSheetShowing()) {
            return false;
        }
        if (isAnimating()) {
            return false;
        }
        if (!hasIntercepted) {
            return onInterceptTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Snapshot the state of things when finger touches the screen.
            // This allows us to calculate deltas without losing precision which we would have if we calculated deltas based on the previous touch.
            bottomSheetOwnsTouch = false;
            sheetViewOwnsTouch = false;
            downY = event.getY();
            downX = event.getX();
            downSheetTranslation = sheetTranslation;
            downState = state;
            velocityTracker.clear();
        }
        velocityTracker.addMovement(event);
        // The max translation is a hard limit while the min translation is where we start dragging more slowly and allow the sheet to be dismissed.
        float maxSheetTranslation = getMaxSheetTranslation();
        float peekSheetTranslation = getPeekSheetTranslation();
        float deltaY = downY - event.getY();
        float deltaX = downX - event.getX();
        if (!bottomSheetOwnsTouch && !sheetViewOwnsTouch) {
            bottomSheetOwnsTouch = Math.abs(deltaY) > touchSlop;
            sheetViewOwnsTouch = Math.abs(deltaX) > touchSlop;
            if (bottomSheetOwnsTouch) {
                if (state == State.PEEKED) {
                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.offsetLocation(0, sheetTranslation - getHeight());
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    getSheetView().dispatchTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }
                sheetViewOwnsTouch = false;
                downY = event.getY();
                downX = event.getX();
                deltaY = 0;
                deltaX = 0;
            }
        }
        // This is not the actual new sheet translation but a first approximation it will be adjusted to account for max and min translations etc.
        float newSheetTranslation = downSheetTranslation + deltaY;
        if (bottomSheetOwnsTouch) {
            // If we are scrolling down and the sheet cannot scroll further, go out of expanded mode.
            boolean scrollingDown = deltaY < 0;
            boolean canScrollUp = canScrollUp(getSheetView(), event.getX(), event.getY() + (sheetTranslation - getHeight()));
            if (state == State.EXPANDED && scrollingDown && !canScrollUp) {
                // Reset variables so deltas are correctly calculated from the point at which the sheet was 'detached' from the top.
                downY = event.getY();
                downSheetTranslation = sheetTranslation;
                velocityTracker.clear();
                setState(State.PEEKED);
                setSheetLayerTypeIfEnabled(LAYER_TYPE_HARDWARE);
                newSheetTranslation = sheetTranslation;
                // Dispatch a cancel event to the sheet to make sure its touch handling is cleaned up nicely.
                MotionEvent cancelEvent = MotionEvent.obtain(event);
                cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                getSheetView().dispatchTouchEvent(cancelEvent);
                cancelEvent.recycle();
            }
            // If we are at the top of the view we should go into expanded mode.
            if (state == State.PEEKED && newSheetTranslation > maxSheetTranslation) {
                setSheetTranslation(maxSheetTranslation);
                // Dispatch a down event to the sheet to make sure its touch handling is initiated correctly.
                newSheetTranslation = Math.min(maxSheetTranslation, newSheetTranslation);
                MotionEvent downEvent = MotionEvent.obtain(event);
                downEvent.setAction(MotionEvent.ACTION_DOWN);
                getSheetView().dispatchTouchEvent(downEvent);
                downEvent.recycle();
                setState(State.EXPANDED);
                setSheetLayerTypeIfEnabled(LAYER_TYPE_NONE);
            }
            if (state == State.EXPANDED) {
                // Dispatch the touch to the sheet if we are expanded so it can handle its own internal scrolling.
                event.offsetLocation(0, sheetTranslation - getHeight());
                getSheetView().dispatchTouchEvent(event);
            } else {
                // Make delta less effective when sheet is below the minimum translation.
                // This makes it feel like scrolling in jello which gives the user an indication that the sheet will be dismissed if they let go.
                if (newSheetTranslation < peekSheetTranslation) {
                    newSheetTranslation = peekSheetTranslation - (peekSheetTranslation - newSheetTranslation) / 4f;
                }
                setSheetTranslation(newSheetTranslation);
                if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    // If touch is canceled, go back to previous state, a canceled touch should never commit an action.
                    if (downState == State.EXPANDED) {
                        expandSheet();
                    } else {
                        peekSheet();
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (newSheetTranslation < peekSheetTranslation) {
                        dismissSheet();
                    } else {
                        // If touch is released, go to a new state depending on velocity.
                        // If the velocity is not high enough we use the position of the sheet to determine the new state.
                        velocityTracker.computeCurrentVelocity(1000);
                        float velocityY = velocityTracker.getYVelocity();
                        if (Math.abs(velocityY) < minFlingVelocity) {
                            if (sheetTranslation > getHeight() / 2) {
                                expandSheet();
                            } else {
                                peekSheet();
                            }
                        } else {
                            if (velocityY < 0) {
                                expandSheet();
                            } else {
                                peekSheet();
                            }
                        }
                    }
                }
            }
        } else {
            // If the user clicks outside of the bottom sheet area we should dismiss the bottom sheet.
            boolean touchOutsideBottomSheet = event.getY() < getHeight() - sheetTranslation || !isXInSheet(event.getX());
            if (event.getAction() == MotionEvent.ACTION_UP && touchOutsideBottomSheet && interceptContentTouch) {
                dismissSheet();
                return true;
            }
            event.offsetLocation(0, sheetTranslation - getHeight());
            getSheetView().dispatchTouchEvent(event);
        }
        return true;
    }

    public enum State {
        HIDDEN,
        PREPARING,
        PEEKED,
        EXPANDED
    }

    public interface OnSheetStateChangeListener {
        void onSheetStateChanged(State state);
    }

    /**
     * Utility class which registers if the animation has been canceled so that subclasses may respond differently in onAnimationEnd
     */
    private static class CancelDetectionAnimationListener extends AnimatorListenerAdapter {

        protected boolean canceled;

        @Override
        public void onAnimationCancel(Animator animation) {
            canceled = true;
        }

    }

    private static class IdentityViewTransformer {
        public static final float MAX_DIM_ALPHA = 0.7f;

        public float getDimAlpha(float translation, float maxTranslation, float peekedTranslation, BottomSheetLayout parent, View view) {
            float progress = translation / maxTranslation;
            return progress * MAX_DIM_ALPHA;
        }

        public void transformView(float translation, float maxTranslation, float peekedTranslation, BottomSheetLayout parent, View view) {
            // no-op
        }
    }

    public interface OnSheetDismissedListener {

        /**
         * Called when the presented sheet has been dismissed.
         *
         * @param bottomSheetLayout The bottom sheet which contained the presented sheet.
         */
        void onDismissed(BottomSheetLayout bottomSheetLayout);

    }

}
