package psycho.euphoria.v;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class PlayerHelper {
    public static final String TEST_M3U8_URL = "https://t23.cdn2020.com/video/m3u8/2023/09/18/c0fe9522/index.m3u8";

    public static void clearSurface(Surface surface) {
        if (surface == null) {
            return;
        }
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, null);
        int[] attribList = {EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_ALPHA_SIZE, 8, EGL10.EGL_RENDERABLE_TYPE, EGL10.EGL_WINDOW_BIT, EGL10.EGL_NONE, 0,      // placeholder for recordable [@-3]
                EGL10.EGL_NONE};
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        egl.eglChooseConfig(display, attribList, configs, configs.length, numConfigs);
        EGLConfig config = configs[0];
        EGLContext context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, new int[]{12440, 2, EGL10.EGL_NONE});
        EGLSurface eglSurface = egl.eglCreateWindowSurface(display, config, surface, new int[]{EGL10.EGL_NONE});
        egl.eglMakeCurrent(display, eglSurface, eglSurface, context);
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        egl.eglSwapBuffers(display, eglSurface);
        egl.eglDestroySurface(display, eglSurface);
        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroyContext(display, context);
        egl.eglTerminate(display);
    }

    public static void toggleFullscreen(Activity activity, MediaPlayer mediaPlayer, TextureView textureView,
                                        View root) {
        int orientation = calculateScreenOrientation(activity);
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            int videoWidth = mediaPlayer.getVideoWidth();
            int videoHeight = mediaPlayer.getVideoHeight();
            double ratio = root.getMeasuredWidth() / (videoHeight * 1.0);
            int width = (int) (ratio * videoWidth);
            int left = (root.getMeasuredHeight() - width) >> 1;
            LayoutParams layoutParams = new LayoutParams(width, activity.getResources().getDisplayMetrics().widthPixels);
            layoutParams.leftMargin = left;
            textureView.setLayoutParams(layoutParams);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            int videoWidth = mediaPlayer.getVideoWidth();
            int videoHeight = mediaPlayer.getVideoHeight();
            double ratio = root.getMeasuredHeight() / (videoWidth * 1.0);
            int height = (int) (ratio * videoHeight);
            int top = (root.getMeasuredWidth() - height) >> 1;
            LayoutParams layoutParams = new LayoutParams(root.getMeasuredHeight(), height);
            layoutParams.topMargin = top;
            textureView.setLayoutParams(layoutParams);
        }
    }

    static int calculateScreenOrientation(Activity activity) {
        int displayRotation = getDisplayRotation(activity);
        boolean standard = displayRotation < 180;
        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (standard) return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            else return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        } else {
            if (displayRotation == 90 || displayRotation == 270) {
                standard = !standard;
            }
            return standard ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }
    }

    static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }
}