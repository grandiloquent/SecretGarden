package psycho.euphoria.v;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;

import psycho.euphoria.v.ZoomableTextureView.Listener;

public class PlayerActivity extends Activity implements TextureView.SurfaceTextureListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener {

    public static final String KEY_VIDEO_FILE = "VideoFile";
    public static final String KEY_VIDEO_TITLE = "VideoTitle";
    private int videoWidth;
    private int videoHeight;
    ZoomableTextureView textureView;
    private MediaPlayer mediaPlayer;
    int mWidth;
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
    private void adjustTextureViewSize(int viewWidth, int viewHeight) {
        if (videoWidth == 0 || videoHeight == 0) {
            return; // Video size not yet known
        }
        float aspectRatioVideo = (float) videoWidth / videoHeight;
        float aspectRatioView = (float) viewWidth / viewHeight;
        ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
        if (aspectRatioVideo > aspectRatioView) {
            // Video is wider than the view - fit width
            layoutParams.width = viewWidth;
            layoutParams.height = (int) (viewWidth / aspectRatioVideo);
        } else {
            // Video is taller than or has the same aspect ratio as the view - fit height
            layoutParams.height = viewHeight;
            layoutParams.width = (int) (viewHeight * aspectRatioVideo);
        }
        mWidth = layoutParams.width;
        textureView.setLayoutParams(layoutParams);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN; // Optional: hide status bar as well
        decorView.setSystemUiVisibility(uiOptions);
        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(this);
        textureView.setListener(new Listener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (e.getX() > (float) mWidth / 2) {
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() +
                            mediaPlayer.getDuration() / 5);
                } else {
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() -
                            mediaPlayer.getDuration() / 5);
                }
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (e.getX() > (float) mWidth / 2) {
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + mediaPlayer.getDuration() / 10);
                } else {
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - mediaPlayer.getDuration() / 10);
                }
                return true;
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
    }

    @Override
    public void onSurfaceTextureAvailable( SurfaceTexture surface, int width, int height) {
        Surface s = new Surface(surface);
        try {
            Intent intent = getIntent();

            mediaPlayer = new MediaPlayer();
            if (intent.hasExtra(KEY_VIDEO_FILE)) {
                mediaPlayer.setDataSource(intent.getStringExtra(KEY_VIDEO_FILE));
                Toast.makeText(this, intent.getStringExtra(KEY_VIDEO_TITLE), Toast.LENGTH_LONG).show();
            } else {
                Uri uri = intent.getData();
                mediaPlayer.setDataSource(uri.toString());
                Toast.makeText(this, intent.getStringExtra(KEY_VIDEO_TITLE), Toast.LENGTH_LONG).show();
            }
            mediaPlayer.setSurface(s);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnVideoSizeChangedListener(this);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed( SurfaceTexture surface) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureUpdated( SurfaceTexture surface) {
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        videoWidth = width;
        videoHeight = height;
        adjustTextureViewSize(textureView.getWidth(), textureView.getHeight()); // Adjust based on current TextureView dimensions
    }
}