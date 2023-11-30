package psycho.euphoria.v;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.io.File;


public class PlayerActivity extends Activity {

    public static final String KEY_VIDEO_FILE = "VideoFile";
    public static final String KEY_VIDEO_TITLE = "VideoTitle";

    private VideoView mVideoView;

    public static void launchActivity(Context context, File file, int sort) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(KEY_VIDEO_FILE, file.getAbsolutePath());
        intent.putExtra(KEY_VIDEO_TITLE, file.getName());
        context.startActivity(intent);
    }

    public static void launchActivity(Context context, String source, String title) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video);
        mVideoView = findViewById(R.id.video_view);
        Intent intent = getIntent();
        if (intent.hasExtra(KEY_VIDEO_FILE)) {
            mVideoView.playVideo(intent.getStringExtra(KEY_VIDEO_FILE));
            Toast.makeText(this, intent.getStringExtra(KEY_VIDEO_TITLE), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.suspend();
    }
}
// https://github.com/devlucem/ZoomableVideo
// https://medium.com/@ali.muzaffar/android-detecting-a-pinch-gesture-64a0a0ed4b41
// https://github.com/tidev/titanium-sdk/blob/9f5fc19ecbdf97cd49233ead298bda3a0c4fcf06/android/modules/ui/src/java/ti/modules/titanium/ui/widget/TiImageView.java#L446