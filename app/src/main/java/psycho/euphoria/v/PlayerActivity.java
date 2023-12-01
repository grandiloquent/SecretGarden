package psycho.euphoria.v;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import java.io.File;

import psycho.euphoria.v.TimeBar.OnScrubListener;


public class PlayerActivity extends Activity implements VideoView.Listener, OnScrubListener {

    public static final String KEY_VIDEO_FILE = "VideoFile";
    public static final String KEY_VIDEO_TITLE = "VideoTitle";
    private final Handler mHandler = new Handler();
    private VideoView mVideoView;
    private SimpleTimeBar mTimeBar;
    private View mWrapper;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video);
        mVideoView = findViewById(R.id.video_view);
        mVideoView.setListener(this);
        mWrapper = findViewById(R.id.wrapper);
        mTimeBar = findViewById(R.id.time_bar);
        mTimeBar.addListener(this);
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
    public void onVideoClick() {
        mWrapper.setVisibility(View.VISIBLE);
        hideSchedule();
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
    }

    @Override
    public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
        mVideoView.seekTo((int) position);
        hideSchedule();
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
}
// https://github.com/devlucem/ZoomableVideo
// https://medium.com/@ali.muzaffar/android-detecting-a-pinch-gesture-64a0a0ed4b41
// https://github.com/tidev/titanium-sdk/blob/9f5fc19ecbdf97cd49233ead298bda3a0c4fcf06/android/modules/ui/src/java/ti/modules/titanium/ui/widget/TiImageView.java#L446