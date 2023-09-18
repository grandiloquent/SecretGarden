package psycho.euphoria.v;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.media.TimedMetaData;
import android.opengl.GLES20;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import psycho.euphoria.v.TimeBar.OnScrubListener;

import static psycho.euphoria.v.PlayerUtils.hideSystemUI;
import static psycho.euphoria.v.Shared.getStringForTime;

public class PlayerActivity extends Activity implements OnTouchListener {

    public static final int DEFAULT_HIDE_TIME_DELAY = 5000;
    public static final String KEY_SHUFFLE = "shuffle";
    public static final String KEY_VIDEO_FILE = "VideoFile";
    public static final String KEY_VIDEO_TITLE = "VideoTitle";
    private static final int TOUCH_IGNORE = -1;
    private static final int TOUCH_NONE = 0;
    private final Handler mHandler = new Handler();
    private final StringBuilder mStringBuilder = new StringBuilder();
    private final Formatter mFormatter = new Formatter(mStringBuilder);
    private TextureView mTextureView;
    private MediaPlayer mMediaPlayer;
    private Surface mSurface;
    private FrameLayout mRoot;
    private boolean mLayout = false;
    private FrameLayout mBottomBar;
    private TextView mDuration;
    private SimpleTimeBar mTimeBar;
    private TextView mPosition;
    private LinearLayout mCenterControls;
    private List<String> mPlayList;
    private int mPlayIndex;
    private ImageButton mPlayPause;
    private int mScaledTouchSlop;
    private int mDelta = 0;
    private int mCurrentPosition;
    private float mLastFocusX;
    private int mLastSystemUiVis;
    private PlayerSizeInformation mPlayerSizeInformation;
    private final Runnable mHideAction = this::hiddenControls;
    private boolean mShuffle;
    private float mSpeed = .5f;

    public static void launchActivity(Context context, File videoFile, int sort) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(KEY_VIDEO_FILE, videoFile.getAbsolutePath());
        intent.putExtra("sort", sort);
        context.startActivity(intent);
    }

    public static void launchActivity(Context context, String videoFile, String title) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(KEY_VIDEO_FILE, videoFile);
        intent.putExtra(KEY_VIDEO_TITLE, title);
        context.startActivity(intent);
    }

    static File[] listVideoFiles(String dir) {
        File directory = new File(dir);
        Pattern pattern = Pattern.compile("\\.(?:mp4|vm|crdownload)$");
        File[] files = directory.listFiles(file -> file.isFile() && pattern.matcher(file.getName()).find());
        if (files == null || files.length == 0) return null;
        Arrays.sort(files, (o1, o2) -> {
            final long result = o2.lastModified() - o1.lastModified();
            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            } else {
                return 0;
            }
        });
        return files;
    }

    private void bindingDeleteVideoEvent() {
        findViewById(R.id.action_file_download).setOnClickListener(v -> new AlertDialog.Builder(PlayerActivity.this).setTitle("询问").setMessage("确定要删除 \"" + Shared.substringAfterLast(mPlayList.get(mPlayIndex), "/") + "\" 视频吗？").setPositiveButton(android.R.string.ok, (dialog, which) -> {
            deleteVideo();
            dialog.dismiss();
        }).setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            dialog.dismiss();
        }).show());
    }

    private void deleteVideo() {
        if (mPlayList == null || mPlayList.size() == 0) {
            return;
        }
        mMediaPlayer.reset();
        File videoFile = new File(mPlayList.get(mPlayIndex));
//        File dir = new File(
//                videoFile.getParentFile(),
//                "videos"
//        );
//        if (!dir.isDirectory()) {
//            dir.mkdir();
//        }
//        videoFile.renameTo(new File(dir, videoFile.getName()));
        if (videoFile.exists()) {
            videoFile.delete();
        }
        loadPlaylist(videoFile.getParentFile().getAbsolutePath());
        if (mPlayList.size() > 0) {
            if (mPlayList.size() - 1 < mPlayIndex) mPlayIndex = 0;
            try {
                play();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void hiddenControls() {
        mTimeBar.setVisibility(View.GONE);
        mBottomBar.setVisibility(View.GONE);
        mCenterControls.setVisibility(View.GONE);
        hideSystemUI(this);
        zoomIn();
    }

    private void initializePlayer() {
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setOnBufferingUpdateListener(this::onBufferingUpdate);
            mMediaPlayer.setOnCompletionListener(this::onCompletion);
            mMediaPlayer.setOnErrorListener(this::onError);
            mMediaPlayer.setOnInfoListener(this::onInfo);
            mMediaPlayer.setOnPreparedListener(this::onPrepared);
            mMediaPlayer.setOnSeekCompleteListener(this::onSeekComplete);
            mMediaPlayer.setOnTimedMetaDataAvailableListener(this::onTimedMetaDataAvailable);
            mMediaPlayer.setOnVideoSizeChangedListener(this::onVideoSizeChanged);
            mMediaPlayer.setSurface(mSurface);
            play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPlaylist(String folder) {
        File fileDirectory = new File(folder);
        if (!fileDirectory.isDirectory()) return;
        File[] files = listVideoFiles(folder);
        if (files == null) {
            return;
        }
        int sort = getIntent().getIntExtra("sort", 2);
        int direction = (sort & 1) == 0 ? -1 : 1;
        Arrays.sort(files, (o1, o2) -> {
            if ((sort & 2) == 2) {
                final long result = o2.lastModified() - o1.lastModified();
                if (result < 0) {
                    return -1 * direction;
                }
                if (result > 0) {
                    return 1 * direction;
                }
            }
            if ((sort & 4) == 4) {
                final long result = o2.length() - o1.length();
                if (result < 0) {
                    return -1 * direction;
                }
                if (result > 0) {
                    return 1 * direction;
                }
            }
            return 0;
        });
        mPlayList = new ArrayList<>();
        for (File file : files) {
            mPlayList.add(file.getAbsolutePath());
        }
        if (mShuffle) {
            Collections.shuffle(mPlayList);
        }
    }

    private void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        mTimeBar.setBufferedPosition(i);
    }

    private void onCompletion(MediaPlayer mediaPlayer) {
        mMediaPlayer.reset();
        try {
            play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        Toast.makeText(this, i + " " + i1, Toast.LENGTH_SHORT).show();
        return true;
    }

    private boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return true;
    }

    private void onNext(View view) {
        if (mPlayList.size() < 2) return;
        mLayout = false;
        if (mPlayIndex + 1 < mPlayList.size()) {
            mPlayIndex++;
        } else {
            mPlayIndex = 0;
        }
        mMediaPlayer.reset();
        try {
            play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onPlayPause(View view) {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPlayPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.exo_ic_play_circle_filled));
        } else {
            mMediaPlayer.start();
            mPlayPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.exo_ic_pause_circle_filled));
        }
    }

    private void onPrepared(MediaPlayer mediaPlayer) {
        mDuration.setText(getStringForTime(mStringBuilder, mFormatter, mediaPlayer.getDuration()));
        mTimeBar.setDuration(mediaPlayer.getDuration());
        mMediaPlayer.start();
//        PlaybackParams playbackParams = new PlaybackParams();
//        playbackParams.setSpeed(mSpeed);
//        mMediaPlayer.setPlaybackParams(playbackParams);
        mPlayPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.exo_ic_pause_circle_filled));
        updateProgress();
        hiddenControls();
        new VideoDatabase(this, new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "videos.db").getAbsolutePath()).updateVideoInformation(getIntent().getStringExtra(KEY_VIDEO_FILE), mMediaPlayer.getDuration() / 1000, mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight());
    }

    private void onPrev(View view) {
        if (mPlayList.size() < 2) return;
        mLayout = false;
        if (mPlayIndex - 1 > -1) {
            mPlayIndex--;
        } else {
            mPlayIndex = 0;
        }
        mMediaPlayer.reset();
        try {
            play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onSeekComplete(MediaPlayer mediaPlayer) {
    }

    private void onTimedMetaDataAvailable(MediaPlayer mediaPlayer, TimedMetaData timedMetaData) {
    }

    private void onVideoSizeChanged(MediaPlayer mediaPlayer, int videoWidth, int videoHeight) {
        zoomIn();
    }

    private void play() throws IOException {
        if (mPlayList == null) {
            Log.e("B5aOx2", String.format("play, %s", getIntent().getStringExtra(KEY_VIDEO_FILE) == null));
            String url = getIntent().getStringExtra(KEY_VIDEO_FILE);
            if (url == null) url = PlayerHelper.TEST_M3U8_URL;
            mMediaPlayer.setDataSource(url);
            mMediaPlayer.prepareAsync();
            return;
        }
        getActionBar().setTitle(Shared.substringAfterLast(mPlayList.get(mPlayIndex), "/"));
        mMediaPlayer.setDataSource(mPlayList.get(mPlayIndex));
        mMediaPlayer.prepareAsync();
    }

    private void scheduleHideControls() {
        mHandler.removeCallbacks(mHideAction);
        mHandler.postDelayed(mHideAction, DEFAULT_HIDE_TIME_DELAY);
    }

    private void setOnSystemUiVisibilityChangeListener() {
        // When the user touches the screen or uses some hard key, the framework
        // will change system ui visibility from invisible to visible. We show
        // the media control and enable system UI (e.g. ActionBar) to be visible at this point
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {

            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                int diff = mLastSystemUiVis ^ visibility;
                mLastSystemUiVis = visibility;
                if ((diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0 && (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                    showControls();
                    getActionBar().show();
                    scheduleHideControls();
                    zoomOut();
                }
            }
        });
    }

    private void showControls() {
        mTimeBar.setVisibility(View.VISIBLE);
        mBottomBar.setVisibility(View.VISIBLE);
        mCenterControls.setVisibility(View.VISIBLE);
        updateProgress();
    }

    private void updateProgress() {
        if (mMediaPlayer == null || mBottomBar.getVisibility() != View.VISIBLE) {
            return;
        }
        mTimeBar.setPosition(mMediaPlayer.getCurrentPosition());
        mPosition.setText(getStringForTime(mStringBuilder, mFormatter, mMediaPlayer.getCurrentPosition()));
        mHandler.postDelayed(this::updateProgress, 1000);
    }

    private void zoomIn() {
        if (mMediaPlayer == null) {
            return;
        }
        if (mPlayerSizeInformation == null) {
            mPlayerSizeInformation = new PlayerSizeInformation(this, mRoot, mBottomBar, mTimeBar);
        }
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == 1) {
            int videoHeight = mMediaPlayer.getVideoHeight();
            int videoWidth = mMediaPlayer.getVideoWidth();
            double ratio = mRoot.getMeasuredWidth() / (videoWidth * 1.0);
            int height = (int) (ratio * videoHeight);
            int top = (mRoot.getMeasuredHeight() - height) >> 1;
            LayoutParams layoutParams = new LayoutParams(mRoot.getMeasuredWidth(), height);
            layoutParams.topMargin = top;
            mTextureView.setLayoutParams(layoutParams);
        } else {
            int videoHeight = mMediaPlayer.getVideoHeight();
            int videoWidth = mMediaPlayer.getVideoWidth();
            float x = ((float) mPlayerSizeInformation.getAvailableHeight()) / videoWidth;
            float y = ((float) mPlayerSizeInformation.getAvailableWidth()) / videoHeight;
            x = Math.min(x, y);
            int screenWidth = (int) (videoWidth * x);
            int screenHeight = (int) (videoHeight * x);
            LayoutParams layoutParams = new LayoutParams(screenWidth, screenHeight);
            layoutParams.topMargin = (mPlayerSizeInformation.getAvailableWidth() - screenHeight) >> 1;
            layoutParams.leftMargin = (mPlayerSizeInformation.getAvailableHeight() - screenWidth) >> 1;
            mTextureView.setLayoutParams(layoutParams);
        }

    }

    private void zoomOut() {
        if (mMediaPlayer == null) {
            return;
        }
        if (mPlayerSizeInformation == null) {
            mPlayerSizeInformation = new PlayerSizeInformation(this, mRoot, mBottomBar, mTimeBar);
        }
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == 1) {
            mPlayerSizeInformation = new PlayerSizeInformation(this, mRoot, mBottomBar, mTimeBar);
            int videoHeight = mMediaPlayer.getVideoHeight();
            int videoWidth = mMediaPlayer.getVideoWidth();
            float x = ((float) mPlayerSizeInformation.getAvailableWidth()) / videoWidth;
            float y = ((float) mPlayerSizeInformation.getPortraitHeight()) / videoHeight;
            x = Math.min(x, y);
            int screenWidth = (int) (videoWidth * x);
            int screenHeight = (int) (videoHeight * x);
            LayoutParams layoutParams = new LayoutParams(screenWidth, screenHeight);
            layoutParams.topMargin = (mPlayerSizeInformation.getPortraitHeight() - screenHeight) >> 1;
            layoutParams.leftMargin = (mPlayerSizeInformation.getAvailableWidth() - screenWidth) >> 1;
            mTextureView.setLayoutParams(layoutParams);
        } else {
            int videoHeight = mMediaPlayer.getVideoHeight();
            int videoWidth = mMediaPlayer.getVideoWidth();
            float x = ((float) mPlayerSizeInformation.getAvailableHeight()) / videoWidth;
            float y = ((float) mPlayerSizeInformation.getLandscapeHeight()) / videoHeight;
            x = Math.min(x, y);
            int screenWidth = (int) (videoWidth * x);
            int screenHeight = (int) (videoHeight * x);
            LayoutParams layoutParams = new LayoutParams(screenWidth, screenHeight);
            layoutParams.topMargin = (mPlayerSizeInformation.getLandscapeHeight() - screenHeight) >> 1;
            layoutParams.leftMargin = (mPlayerSizeInformation.getAvailableHeight() - screenWidth - mPlayerSizeInformation.getNavigationBarLanscapeHeight()) >> 1;
            mTextureView.setLayoutParams(layoutParams);
            //Log.e("B5aOx2", String.format("zoomOut, %s", mPlayerSizeInformation.toString()));
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);
        bindingDeleteVideoEvent();
        // 绑定全屏按钮
        findViewById(R.id.action_fullscreen).setOnClickListener(v -> PlayerHelper.toggleFullscreen(this, mMediaPlayer, mTextureView, mRoot));
        mRoot = findViewById(R.id.root);
//        mRoot.setOnClickListener(v -> {
//            showSystemUi(true);
//            showControls();
//            scheduleHideControls();
//
//        });
        setOnSystemUiVisibilityChangeListener();
        hideSystemUI(this);
//        View decorView = getWindow().getDecorView();
//        decorView.setOnSystemUiVisibilityChangeListener
//                (visibility -> {
//                    // Note that system bars will only be "visible" if none of the
//                    // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
//                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
//                        // TODO: The system bars are visible. Make any desired
//                        // adjustments to your UI, such as showing the action bar or
//                        // other navigational controls.
//                        mHandler.postDelayed(this::hideSystemUI, DEFAULT_HIDE_TIME_DELAY);
//                    } else {
//                        // TODO: The system bars are NOT visible. Make any desired
//                        // adjustments to your UI, such as hiding the action bar or
//                        // other navigational controls.
//                    }
//                });
        mCenterControls = findViewById(R.id.exo_center_controls);
        mTextureView = findViewById(R.id.texture_view);
        mPosition = findViewById(R.id.position);
        mBottomBar = findViewById(R.id.exo_bottom_bar);
        mDuration = findViewById(R.id.duration);
        mTextureView.setSurfaceTextureListener(new SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mSurface = new Surface(surface);
                initializePlayer();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
        mTimeBar = findViewById(R.id.timebar);
        mTimeBar.addListener(new OnScrubListener() {
            @Override
            public void onScrubMove(TimeBar timeBar, long position) {
                mPosition.setText(getStringForTime(mStringBuilder, mFormatter, position));
            }

            @Override
            public void onScrubStart(TimeBar timeBar, long position) {
                mHandler.removeCallbacks(mHideAction);
                mPosition.setText(getStringForTime(mStringBuilder, mFormatter, position));
            }

            @Override
            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                mMediaPlayer.seekTo((int) position);
                updateProgress();
                scheduleHideControls();
            }

        });
        Button rewWithAmount = findViewById(R.id.exo_rew_with_amount);
        rewWithAmount.setText("2");
        rewWithAmount.setOnClickListener(v -> {
//            int dif = mMediaPlayer.getCurrentPosition() - 10000;
//            if (dif < 0) {
//                dif = 0;
//            }
//            if (VERSION.SDK_INT >= VERSION_CODES.O) {
//                mMediaPlayer.seekTo(dif);
//            } else {
//                mMediaPlayer.seekTo(dif);
//            }
            PlaybackParams playbackParams = new PlaybackParams();
//            mSpeed /= 2;
//            if (mSpeed > 1 && mSpeed < 2) {
//                mSpeed = 1;
//            }
//            Toast.makeText(this, Float.toString(mSpeed), Toast.LENGTH_SHORT).show();
//            mSpeed -= .5f; //mSpeed >> 1 == 0 ? 1 : mSpeed >> 1;
//            if (mSpeed <= 0) mSpeed = 1;
//            playbackParams.setSpeed(mSpeed);
//            mMediaPlayer.setPlaybackParams(playbackParams);
            if (mSpeed == .5f) {
                mSpeed = 20f;
            } else if (mSpeed == 20f) {
                mSpeed = 6f;
            } else if (mSpeed == 6f) {
                mSpeed = 1f;
            }
            playbackParams.setSpeed(mSpeed);
            mMediaPlayer.setPlaybackParams(playbackParams);
            scheduleHideControls();
            updateProgress();
        });
        Button ffwdWithAmount = findViewById(R.id.exo_ffwd_with_amount);
        ffwdWithAmount.setText("2");
        Typeface typeface = null;
        typeface = getResources().getFont(R.font.roboto_medium_numbers);
        rewWithAmount.setTypeface(typeface);
        ffwdWithAmount.setTypeface(typeface);
        ffwdWithAmount.setOnClickListener(v -> {
//            int dif = mMediaPlayer.getCurrentPosition() + 10000;
//            if (dif > mMediaPlayer.getDuration()) {
//                dif = mMediaPlayer.getDuration();
//            }
//            if (VERSION.SDK_INT >= VERSION_CODES.O) {
//                mMediaPlayer.seekTo(dif);
//
//            } else {
//                mMediaPlayer.seekTo(dif);
//            }
            PlaybackParams playbackParams = new PlaybackParams();
            // mSpeed += .5f; //= mSpeed << 1;
//            if (mSpeed >= 10) {
//                mSpeed = 6;
//            }
//            Toast.makeText(this, Float.toString(mSpeed), Toast.LENGTH_SHORT).show();
            mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() + 30 * 1000);
            scheduleHideControls();
            updateProgress();
        });
        mPlayPause = findViewById(R.id.play_pause);
        mPlayPause.setOnClickListener(this::onPlayPause);
        mScaledTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        //mTextureView.setOnTouchListener(this);
        ImageButton prev = findViewById(R.id.prev);
        ImageButton next = findViewById(R.id.next);
        mShuffle = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KEY_SHUFFLE, false);
        findViewById(R.id.action_shuffle).setOnClickListener(v -> {
            if (!mShuffle) {
                Collections.shuffle(mPlayList);
                mPlayIndex = mPlayList.indexOf(mPlayList.get(mPlayIndex));
            }
            mShuffle = !mShuffle;
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(KEY_SHUFFLE, mShuffle).apply();

        });
        String videoFile = getIntent().getStringExtra(KEY_VIDEO_FILE);
        if (getIntent().getStringExtra(KEY_VIDEO_TITLE) != null)
            this.setTitle(getIntent().getStringExtra(KEY_VIDEO_TITLE));
        findViewById(R.id.action_speed).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Shared.openTextContentDialog(PlayerActivity.this, "跳转", value -> {
                    Pattern pattern = Pattern.compile("\\b(\\d+ )+\\d+\\b");
                    Matcher matcher = pattern.matcher(value);
                    if (matcher.matches()) {
                        String[] pieces = value.split(" ");
                        int total = 0;
                        for (int i = pieces.length - 1, j = 0; i > -1; i--, j++) {
                            total += Integer.parseInt(pieces[i]) * Math.pow(60, j);
                        }
                        total *= 1000;
                        if (VERSION.SDK_INT >= VERSION_CODES.O) {
                            mMediaPlayer.seekTo(total, MediaPlayer.SEEK_CLOSEST);
                        } else {
                            mMediaPlayer.seekTo(total);
                        }
                    }
                });
            }
        });
        if (videoFile != null) {
            if (new File(videoFile).exists()) {
                loadPlaylist(new File(videoFile).getParentFile().getAbsolutePath());
                if (mPlayList.size() < 2) {
                    prev.setAlpha(75);
                    next.setAlpha(75);
                    mPlayIndex = 0;
                } else {
                    prev.setOnClickListener(this::onPrev);
                    next.setOnClickListener(this::onNext);
                    mPlayIndex = mPlayList.indexOf(videoFile);
                }
                return;
            }

        }


    }


    @Override
    protected void onStart() {
        super.onStart();
        if (mSurface != null) initializePlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHandler.removeCallbacks(null);
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        PlayerHelper.clearSurface(mSurface);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mLastFocusX = event.getX();
                showControls();
                mMediaPlayer.pause();
                mCurrentPosition = mMediaPlayer.getCurrentPosition();
                mDelta = 0;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final float scrollX = mLastFocusX - event.getX();
                if (Math.abs(scrollX) > 1) {
                    if (scrollX < 0) {
                        mDelta++;
                    } else {
                        mDelta--;
                    }
                }
                mPosition.setText(getStringForTime(mStringBuilder, mFormatter, mCurrentPosition + mDelta * 1000));
                mLastFocusX = event.getX();
                break;
            }
            case MotionEvent.ACTION_UP: {
                mMediaPlayer.seekTo(mCurrentPosition + mDelta * 1000);
                mMediaPlayer.start();
                scheduleHideControls();
            }
        }
        return true;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private int mScaledTouchSlop;
        private int mCurrentTime;

        public GestureListener() {
            mScaledTouchSlop = ViewConfiguration.get(PlayerActivity.this).getScaledTouchSlop();
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDown(MotionEvent event) {
            // don't return false here or else none of the other
            // gestures will work
            mCurrentTime = mMediaPlayer.getCurrentPosition();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    // && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                    if (Math.abs(diffX) > mScaledTouchSlop) {
                        if (diffX > 0) {
                            mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() + 1);
                        } else {
                        }
                    }
                }
//                else {
//                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
//                        if (diffY > 0) {
//                        } else {
//                        }
//                    }
//                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            showControls();
            scheduleHideControls();
            return true;
        }
    }

}

// https://github.com/devlucem/ZoomableVideo

