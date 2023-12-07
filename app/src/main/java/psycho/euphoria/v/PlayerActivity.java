package psycho.euphoria.v;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import java.io.File;

import psycho.euphoria.v.TimeBar.OnScrubListener;


public class PlayerActivity extends Activity implements VideoView.Listener, OnScrubListener, OnTouchListener {

    public static final String KEY_VIDEO_FILE = "VideoFile";
    public static final String KEY_VIDEO_TITLE = "VideoTitle";
    private final Handler mHandler = new Handler();
    private VideoView mVideoView;
    private SimpleTimeBar mTimeBar;
    private View mWrapper;
    private VideoForwardDrawable videoForwardDrawable;

    public static void launchActivity(Context context, File file, int sort) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(KEY_VIDEO_FILE, file.getAbsolutePath());
        intent.putExtra(KEY_VIDEO_TITLE, file.getName());
        context.startActivity(intent);
    }

    public static void launchActivity(Context context, String source, String title) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(KEY_VIDEO_TITLE, title);
        intent.setData(Uri.parse(source));
        context.startActivity(intent);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mHandler.removeCallbacksAndMessages(null);
            mLastedTime = 0;
            mIsPressing = true;
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (x > mWrapper.getMeasuredWidth() / 2 && y > mWrapper.getMeasuredHeight() / 2) {
                forward(true);
            } else if (x < mWrapper.getMeasuredWidth() / 2 && y > mWrapper.getMeasuredHeight() / 2) {
                forward(false);
            } else if (x < mWrapper.getMeasuredWidth() / 2 && y < mWrapper.getMeasuredHeight() / 2) {
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                } else {
                    mVideoView.start();
                }
            } else if (x < mWrapper.getMeasuredWidth() / 2 && y < mWrapper.getMeasuredHeight() / 2) {
                finish();
            }


        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            mIsPressing = false;
            hideSchedule();

        }
        return true;
    }

    private long mLastedTime;
    private volatile boolean mIsPressing;

    private void forward(boolean isForward) {
        new Thread(() -> {
            mLastedTime = SystemClock.elapsedRealtime();
            while (mIsPressing) {
                long now = SystemClock.elapsedRealtime();
                if (now - mLastedTime > 50) {
                    runOnUiThread(() -> {
                        mVideoView.forward(isForward);
                    });
                    mLastedTime = now;
                }
            }
        }).start();


    }

    private void hideSchedule() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mWrapper.setVisibility(View.INVISIBLE);
            }
        }, 5000);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.video);
        mVideoView = findViewById(R.id.video_view);
        mVideoView.setListener(this);
        mWrapper = findViewById(R.id.wrapper);
        mWrapper.setOnTouchListener(this);
        mTimeBar = findViewById(R.id.time_bar);
        mTimeBar.addListener(this);
//        videoForwardDrawable = new VideoForwardDrawable(this, false);
//        videoForwardDrawable.setDelegate(new VideoForwardDrawable.VideoForwardDrawableDelegate() {
//            @Override
//            public void onAnimationEnd() {
//            }
//
//            @Override
//            public void invalidate() {
//                mWrapper.invalidate();
//            }
//        });
        hideSchedule();
        Intent intent = getIntent();
        if (intent.hasExtra(KEY_VIDEO_FILE)) {
            mVideoView.playVideo(intent.getStringExtra(KEY_VIDEO_FILE));
            Toast.makeText(this, intent.getStringExtra(KEY_VIDEO_TITLE), Toast.LENGTH_LONG).show();
        } else {
            Uri uri = intent.getData();
            mVideoView.playVideo(uri);
            Toast.makeText(this, intent.getStringExtra(KEY_VIDEO_TITLE), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.suspend();
    }

    @Override
    public void onDurationChange(int duration) {
        mTimeBar.setDuration(duration);
        mTimeBar.setPosition(0);
    }

    @Override
    public void onScrubMove(TimeBar timeBar, long position) {
        mVideoView.seekTo((int) position);
    }

    @Override
    public void onScrubStart(TimeBar timeBar, long position) {
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
        mVideoView.seekTo((int) position);
        hideSchedule();
    }

    @Override
    public void onVideoClick() {
        mWrapper.setVisibility(View.VISIBLE);
        hideSchedule();
    }
}
// https://github.com/devlucem/ZoomableVideo
// https://medium.com/@ali.muzaffar/android-detecting-a-pinch-gesture-64a0a0ed4b41
// https://github.com/tidev/titanium-sdk/blob/9f5fc19ecbdf97cd49233ead298bda3a0c4fcf06/android/modules/ui/src/java/ti/modules/titanium/ui/widget/TiImageView.java#L446