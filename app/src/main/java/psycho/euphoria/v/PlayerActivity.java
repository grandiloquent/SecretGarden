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

    public static final String KEY_VIDEO_FILE = "VideoFile";
    public static final String KEY_VIDEO_TITLE = "VideoTitle";

}
// https://github.com/devlucem/ZoomableVideo
// https://medium.com/@ali.muzaffar/android-detecting-a-pinch-gesture-64a0a0ed4b41
// https://github.com/tidev/titanium-sdk/blob/9f5fc19ecbdf97cd49233ead298bda3a0c4fcf06/android/modules/ui/src/java/ti/modules/titanium/ui/widget/TiImageView.java#L446