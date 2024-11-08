package psycho.euphoria.v;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class TestActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
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
                if (url.contains("cableav.tv")) {
                    PreferenceManager.getDefaultSharedPreferences(TestActivity.this)
                            .edit()
                            .putString(SettingsFragment.KEY_USER_AGENT, webView.getSettings().getUserAgentString())
                            .putString(SettingsFragment.KEY_CABLE_AV_COOKIE, CookieManager.getInstance().getCookie(url))
                            .apply();

                }
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
        webView.loadUrl(getIntent().getStringExtra("url"));
    }
}