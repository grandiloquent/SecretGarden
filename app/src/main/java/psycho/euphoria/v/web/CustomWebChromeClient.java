package psycho.euphoria.v.web;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;

import psycho.euphoria.v.MainActivity;


public class CustomWebChromeClient extends WebChromeClient {
    private MainActivity mMainActivity;
    private View mCustomView;
    private CustomViewCallback mCustomViewCallback;
    protected FrameLayout mFullscreenContainer;
    private int mOriginalOrientation;
    private int mOriginalSystemUiVisibility;

    public CustomWebChromeClient(MainActivity mainActivity) {
        mMainActivity = mainActivity;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        Log.e("B5aOx2", String.format("onConsoleMessage, %s\n%s\n%s", consoleMessage.message(), consoleMessage.lineNumber(),consoleMessage.sourceId()));
        return super.onConsoleMessage(consoleMessage);
    }

    public void onHideCustomView() {
        ((FrameLayout) mMainActivity.getWindow().getDecorView()).removeView(this.mCustomView);
        this.mCustomView = null;
        mMainActivity.getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
        mMainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.mCustomViewCallback.onCustomViewHidden();
        this.mCustomViewCallback = null;
    }

    @Override
    public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
        if (this.mCustomView != null) {
            onHideCustomView();
            return;
        }
        this.mCustomView = paramView;
        this.mOriginalSystemUiVisibility = mMainActivity.getWindow().getDecorView().getSystemUiVisibility();
        this.mOriginalOrientation = mMainActivity.getRequestedOrientation();
        this.mCustomViewCallback = paramCustomViewCallback;
        ((FrameLayout) mMainActivity.getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
        mMainActivity.getWindow().getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        mMainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

    }

    public ValueCallback<Uri[]> ValueCallback;

    public static final int FILE_CHOOSER_REQUEST_CODE = 111;

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        Log.e("B5aOx2", String.format("onShowFileChooser, %s", "----------------->"));
        if (ValueCallback != null)
            ValueCallback.onReceiveValue(null);
        ValueCallback = filePathCallback;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        Intent chooserIntent = Intent.createChooser(intent, "Choose File");
        mMainActivity.startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE);
        return true;
    }
}