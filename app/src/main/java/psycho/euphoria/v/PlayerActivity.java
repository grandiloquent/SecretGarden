package psycho.euphoria.v;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Matrix;
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
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.WindowManager;
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

public class PlayerActivity extends Activity {


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
    Matrix matrix = new Matrix();
    float lastFocusX = 0;
    float lastFocusY = 0;
    ScaleGestureDetector scaleGestureDetector;

    public boolean applyLimitsToMatrix() {
        // Do not continue if matrix has no translations or scales applied to it. (This is an optimization.)
        if (this.matrix.isIdentity()) {
            return false;
        }
        // Fetch matrix values.
        float[] matrixValues = new float[9];
        this.matrix.getValues(matrixValues);
        // Do not allow scale to be less than 1x.
        if ((matrixValues[Matrix.MSCALE_X] < 1.0f) || (matrixValues[Matrix.MSCALE_Y] < 1.0f)) {
            this.matrix.reset();
            return true;
        }
        // Do not allow scale to be greater than 5x.
        final float MAX_SCALE = 5.0f;
        if ((matrixValues[Matrix.MSCALE_X] > MAX_SCALE) || (matrixValues[Matrix.MSCALE_Y] > MAX_SCALE)) {
            this.matrix.postScale(
                    MAX_SCALE / matrixValues[Matrix.MSCALE_X], MAX_SCALE / matrixValues[Matrix.MSCALE_Y],
                    mTextureView.getWidth() / 2.0f, mTextureView.getHeight() / 2.0f);
            this.matrix.getValues(matrixValues);
        }
        // Fetch min/max bounds the image can be scrolled to, preventing image from being scrolled off-screen.
        float translateX = -matrixValues[Matrix.MTRANS_X];
        float translateY = -matrixValues[Matrix.MTRANS_Y];
        float maxTranslateX = (mTextureView.getWidth() * matrixValues[Matrix.MSCALE_X]) - mTextureView.getWidth();
        float maxTranslateY = (mTextureView.getHeight() * matrixValues[Matrix.MSCALE_Y]) - mTextureView.getHeight();
        // Apply translation limits.
        boolean wasChanged = false;
        if (translateX < 0) {
            this.matrix.postTranslate(translateX, 0);
            wasChanged = true;
        } else if (translateX > maxTranslateX) {
            this.matrix.postTranslate(translateX - maxTranslateX, 0);
            wasChanged = true;
        }
        if (translateY < 0) {
            this.matrix.postTranslate(0, translateY);
            wasChanged = true;
        } else if (translateY > maxTranslateY) {
            this.matrix.postTranslate(0, translateY - maxTranslateY);
            wasChanged = true;
        }
        return wasChanged;
    }

    public static void launchActivity(Context context, String videoFile, String title) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(KEY_VIDEO_FILE, videoFile);
        intent.putExtra(KEY_VIDEO_TITLE, title);
        context.startActivity(intent);
    }

    public static void launchActivity(Context context, File videoFile, int sort) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(KEY_VIDEO_FILE, videoFile.getAbsolutePath());
        intent.putExtra("sort", sort);
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
        findViewById(R.id.action_file_download).setOnClickListener(v ->{
            this.finish();
        });
        //  findViewById(R.id.action_file_download).setOnClickListener(v -> new AlertDialog.Builder(PlayerActivity.this).setTitle("询问").setMessage("确定要删除 \"" + Shared.substringAfterLast(mPlayList.get(mPlayIndex), "/") + "\" 视频吗？").setPositiveButton(android.R.string.ok, (dialog, which) -> {
          //  deleteVideo();
            //dialog.dismiss();
        //}).setNegativeButton(android.R.string.cancel, (dialog, which) -> {
          //  dialog.dismiss();
        //}).show());
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
        //hideSystemUI(this);
        //zoomIn();
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
            if (url == null) url = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/1.v";
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

    Matrix mMatrix;

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
        //setOnSystemUiVisibilityChangeListener();
        hideSystemUI(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
        mMatrix = mTextureView.getMatrix();
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
        scaleGestureDetector = new ScaleGestureDetector(this, new SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                matrix.postScale(
                        detector.getScaleFactor(), detector.getScaleFactor(),
                        detector.getFocusX(), detector.getFocusY());
                matrix.postTranslate(detector.getFocusX() - lastFocusX, detector.getFocusY() - lastFocusY);
                applyLimitsToMatrix();
                lastFocusX = detector.getFocusX();
                lastFocusY = detector.getFocusY();
                mTextureView.setTransform(matrix);
                mTextureView.invalidate();
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                lastFocusX = detector.getFocusX();
                lastFocusY = detector.getFocusY();
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                if (applyLimitsToMatrix()) {
                    mTextureView.invalidate();
                }
            }
        });
        GestureDetector gestureDetector = new GestureDetector(this, new SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
//                // Do not continue if we're in the middle of a pinch-zoom.
//                if (scaleGestureDetector.isInProgress()) {
//                    return false;
//                }
//                // Fetch zoom position and translation coordinates.
//                float[] matrixValues = new float[9];
//                matrix.getValues(matrixValues);
//                final boolean isZoomingIn = matrix.isIdentity();
//                final float zoomInScaleFactor = isZoomingIn ? 2.5f : matrixValues[Matrix.MSCALE_X];
//                final float translateX = matrixValues[Matrix.MTRANS_X];
//                final float translateY = matrixValues[Matrix.MTRANS_Y];
//                final float zoomInX = e.getX();
//                final float zoomInY = e.getY();
//                float scaleFactor = 1;
//                matrix.postScale(scaleFactor, scaleFactor);
//                matrix.postTranslate(-translateX, -translateY);
                matrix.set(mMatrix);
                mTextureView.setTransform(matrix);
                mTextureView.invalidate();
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                showControls();
                scheduleHideControls();
                return true;
            }
        });
        mTextureView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent) || scaleGestureDetector.onTouchEvent(motionEvent);
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
        mPlayPause = findViewById(R.id.play_pause);
        mPlayPause.setOnClickListener(this::onPlayPause);
        mScaledTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        //mTextureView.setOnTouchListener(this);
        ImageButton prev = findViewById(R.id.prev);
        ImageButton next = findViewById(R.id.next);
//        mShuffle = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(KEY_SHUFFLE, false);
//        findViewById(R.id.action_shuffle).setOnClickListener(v -> {
//            if (!mShuffle) {
//                Collections.shuffle(mPlayList);
//                mPlayIndex = mPlayList.indexOf(mPlayList.get(mPlayIndex));
//            }
//            mShuffle = !mShuffle;
//            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(KEY_SHUFFLE, mShuffle).apply();
//
//        });
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
                            total += (int) (Integer.parseInt(pieces[i]) * Math.pow(60, j));
                        }
                        total *= 1000;
                        mMediaPlayer.seekTo(total, MediaPlayer.SEEK_CLOSEST);
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

}
// https://github.com/devlucem/ZoomableVideo
// https://medium.com/@ali.muzaffar/android-detecting-a-pinch-gesture-64a0a0ed4b41
// https://github.com/tidev/titanium-sdk/blob/9f5fc19ecbdf97cd49233ead298bda3a0c4fcf06/android/modules/ui/src/java/ti/modules/titanium/ui/widget/TiImageView.java#L446