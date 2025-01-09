package psycho.euphoria.v;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import psycho.euphoria.v.tasks.DownloaderService;

import static psycho.euphoria.v.NineOneHelper.process91Porn;

public class WebActivity extends Activity {
    public static final String EXTRA_VIDEO_URL = "extra_video_url";
    private WebView mWebView;

    private static String sUa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_activity);
        mWebView = findViewById(R.id.web_view);
        //mWebView.clearCache(true);
        JavaInterface javaInterface = new JavaInterface();
        mWebView.addJavascriptInterface(javaInterface, "JInterface");
        WebSettings settings = mWebView.getSettings();
        sUa = settings.getUserAgentString();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String videoUri = getIntent().getStringExtra(EXTRA_VIDEO_URL);
                if (videoUri == null) {
                    return;
                }
                javaInterface.parse(videoUri, getIntent().getStringExtra("id"));
            }

            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
//                if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
//                    Date date = new Date();
//                    final String dateString = FORMATTER.format(date);
//                    mHeaders.put("Date", dateString + " GMT");
//                    return new WebResourceResponse("text/plain", "UTF-8", 200, "OK", mHeaders, null);
//                }
                // request.getRequestHeaders().put("Access-Control-Allow-Origin", "*");
                return null;
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient() {
            private View mCustomView;
            private CustomViewCallback mCustomViewCallback;
            private int mOriginalOrientation;
            private int mOriginalSystemUiVisibility;

            @Override
            public Bitmap getDefaultVideoPoster() {
                return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
            }

            // Print web terminal information
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.e("B5aOx2", String.format("onConsoleMessage, %s", consoleMessage.message()));
                return super.onConsoleMessage(consoleMessage);
            }

            public void onHideCustomView() {
                ((FrameLayout) getWindow().getDecorView()).removeView(this.mCustomView);
                this.mCustomView = null;
                getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
                setRequestedOrientation(this.mOriginalOrientation);
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
                this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
                this.mOriginalOrientation = getRequestedOrientation();
                this.mCustomViewCallback = paramCustomViewCallback;
                ((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
                getWindow().getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

            }
        });
        String uri = getIntent().getStringExtra("uri");
        Log.e("B5aOx2", String.format("onCreate, %s", uri));
        if (uri != null)
            mWebView.loadUrl(uri);
        // Native.getUri()
    }

    /*public static Pair<String, String> process91Porn(String videoAddress) {
        String response = Native.fetch91Porn(Uri.parse(videoAddress).getQueryParameter("viewkey"));
        if (response == null) {
            return null;
        }
        String src = Shared.substringAfter(response, '|');
        src = src.replaceAll("\\s+[0-9a-zA-Z]+\\s+", "");
        String[] pieces = src.split("\",\"");
        byte[] bytes = Base64.decode(
                pieces[0].getBytes(StandardCharsets.UTF_8),
                Base64.DEFAULT
        );
        String xx = new String(bytes);
        String n = "";
        for (int i = 0; i < xx.length(); i++) {
            int x = pieces[1].codePointAt(i % pieces[1].length());
            n += (char) (xx.codePointAt(i) ^ x);
        }
        try {
            n = new String(Base64.decode(n,
                    Base64.DEFAULT));
        } catch (Exception e) {
            Log.e("B5aOx2", String.format("process91Porn,%s %s", response, e.getMessage()));
            return null;
        }
        src = Shared.substring(n, "src='", "'");
        return Pair.create(Shared.substringBefore(Shared.substringBefore(response, "|"), "\n").trim(), src);
//        JSONObject jsonObject = null;
//        try {
//            jsonObject = new JSONObject(response);
//            String title = jsonObject.getString("title");
//            String src = jsonObject.getString("videoUri");
//            return Pair.create(title, src);
//        } catch (JSONException ignored) {
//        }
        //  return null;
    }*/


    public static Pair<String, String> processXVideos(String videoAddress) {
        String[] response = null;
        try {
            response = Utils.getXVideosVideoAddress(videoAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (response == null) {
            return null;
        }
        return Pair.create(response[0], response[1]);
    }

    public static String[] processCk(Context context, String videoAddress) {
        String response = Native.fetchCk(videoAddress, SettingsFragment.getString(context,
                        SettingsFragment.KEY_CK_COOKIE, null),
                SettingsFragment.getString(context,
                        SettingsFragment.KEY_USER_AGENT, null));
        if (response == null) {
            return null;
        }
        String title = Shared.substringBefore(response, "\n").trim();
        String src = Shared.substringAfter(response, "\n")
                .replaceAll("\\\\", "");
        return new String[]{title, src};
    }

    private class JavaInterface {
        @JavascriptInterface
        public void download(String videoUri, String title) {
            Intent starter = new Intent(WebActivity.this, DownloaderService.class);
            starter.putExtra(DownloaderService.EXTRA_VIDEO_ADDRESS, videoUri);
            WebActivity.this.startService(starter);
            //Toast.makeText(WebActivity.this, "添加新任务：" + title, Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void parse(String uri, String id) {
            Log.e("B5aOx2", String.format("parse, %s", uri));
            new Thread(() -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                String[] videoUris = null;
                String[] results = null;
                if (uri.contains("91porn.com")) {
                    results = process91Porn(WebActivity.this, uri);
                } else if (uri.contains("xvideos.com")) {
                    // results = processXVideos(uri);
                } else {
                    results = processCk(WebActivity.this, uri);
                }
                if (results != null) {
                    videoUris = new String[]{results[0], results[1]};
                }
//                else if (uri.contains("xvideos.com")) {
//                    videoUris = Native.fetchXVideos(uri);
//                } else {
//                    videoUris = Native.fetch57Ck(uri);
//                }
                String[] finalVideoUris = videoUris;
                runOnUiThread(() -> {
                    if (finalVideoUris == null) {
                        Toast.makeText(WebActivity.this, "无法解析视频", Toast.LENGTH_LONG).show();
                        return;
                    }
                    JSONObject obj = new JSONObject();
                    try {
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.put(finalVideoUris[1]);
                        obj.put("title", finalVideoUris[0]);
                        obj.put("videos", jsonArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    final String finalUri;
                    if (uri.contains("/vodplay")) {
                        finalUri = "/vodplay" + Shared.substringAfter(uri, "/vodplay");
                    } else {
                        finalUri = uri;
                    }
                    mWebView.evaluateJavascript("start('" + obj.toString() + "','" + finalUri + "')", null);
                });
            }).start();
        }
    }
}
