package psycho.euphoria.v;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Map;

public class TestActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
        webView.clearHistory();
        webView.clearCache(true);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageFinished(view, url);
                if (url.contains("vodplay")) {
                    PreferenceManager.getDefaultSharedPreferences(TestActivity.this)
                            .edit()
                            .putString(SettingsFragment.KEY_USER_AGENT, webView.getSettings().getUserAgentString())
                            .putString(SettingsFragment.KEY_CK_COOKIE, CookieManager.getInstance().getCookie(url))
                            .apply();

                }
                if (url.contains("91porn.com")) {
                    PreferenceManager.getDefaultSharedPreferences(TestActivity.this)
                            .edit()
                            .putString(SettingsFragment.KEY_USER_AGENT, webView.getSettings().getUserAgentString())
                            .putString(SettingsFragment.KEY_91_COOKIE, CookieManager.getInstance().getCookie(url))
                            .apply();

                }
                if (url.contains("cableav.tv")) {
                    PreferenceManager.getDefaultSharedPreferences(TestActivity.this)
                            .edit()
                            .putString(SettingsFragment.KEY_USER_AGENT, webView.getSettings().getUserAgentString())
                            .putString(SettingsFragment.KEY_CABLE_AV_COOKIE, CookieManager.getInstance().getCookie(url))
                            .apply();

                }
            }

            @SuppressLint("NewApi")
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request.getUrl().toString().equals("https://91porn.com/view_video.php?viewkey=1647101175"))
                    request.getRequestHeaders().forEach((x, y) -> {
                        Log.e("B5aOx2", String.format("shouldInterceptRequest, %s:%s", x, y));
                    });
                //Log.e("B5aOx2", String.format("shouldInterceptRequest, %s", request.getUrl()));
                return null;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.contains("vodplay")) {
                    PreferenceManager.getDefaultSharedPreferences(TestActivity.this)
                            .edit()
                            .putString(SettingsFragment.KEY_USER_AGENT, webView.getSettings().getUserAgentString())
                            .putString(SettingsFragment.KEY_CK_COOKIE, CookieManager.getInstance().getCookie(url))
                            .apply();

                }
                if (url.contains("91porn.com")) {
                    Log.e("B5aOx2", String.format("onPageFinished, %s",  CookieManager.getInstance().getCookie(url)));
                    PreferenceManager.getDefaultSharedPreferences(TestActivity.this)
                            .edit()
                            .putString(SettingsFragment.KEY_USER_AGENT, webView.getSettings().getUserAgentString())
                            .putString(SettingsFragment.KEY_91_COOKIE, CookieManager.getInstance().getCookie(url))
                            .apply();

                }
                if (url.contains("cableav.tv")) {
                    PreferenceManager.getDefaultSharedPreferences(TestActivity.this)
                            .edit()
                            .putString(SettingsFragment.KEY_USER_AGENT, webView.getSettings().getUserAgentString())
                            .putString(SettingsFragment.KEY_CABLE_AV_COOKIE, CookieManager.getInstance().getCookie(url))
                            .apply();

                }

            }
        });
        setContentView(webView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        //webView.loadUrl("http://192.168.8.34:8080/");
        webView.loadUrl(getIntent().getStringExtra("url"));
    }
}