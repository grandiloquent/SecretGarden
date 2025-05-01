package psycho.euphoria.v;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.Map;
public class TestActivity extends Activity {
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         webView = new WebView(this);
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
                    NineOneHelper.UserAgent = webView.getSettings().getUserAgentString();
                    NineOneHelper.Cookie = CookieManager.getInstance().getCookie(url);
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
                if (request.getUrl().toString().startsWith("https://91porn.com/view_video.php?"))
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
                    NineOneHelper.UserAgent = webView.getSettings().getUserAgentString();
                    NineOneHelper.Cookie = CookieManager.getInstance().getCookie(url);
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
        Log.e("B5aOx2", String.format("onCreate, %s", getIntent().getStringExtra("url")));
        webView.loadUrl(getIntent().getStringExtra("url"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "刷新");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case 0:
                webView.reload();
                Toast.makeText(this,webView.getUrl(),Toast.LENGTH_LONG).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}