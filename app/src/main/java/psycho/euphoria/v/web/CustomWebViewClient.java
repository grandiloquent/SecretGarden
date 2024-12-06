package psycho.euphoria.v.web;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import psycho.euphoria.v.MainActivity;


public class CustomWebViewClient extends WebViewClient {


    private final MainActivity mContext;

    public CustomWebViewClient(MainActivity context) {
        mContext = context;
    }


    @Override
    public void onPageFinished(WebView view, String url) {
        //  String cookie;
//        if (url.startsWith("https://www.xvideos.com/") && (cookie = CookieManager.getInstance().getCookie(url)) != null) {
//            mContext.setString(MainActivity.KEY_XVIDEOS_COOKIE, cookie);
//        }
        view.evaluateJavascript("(() => {\n" +
                "    function deleteRedundantItems() {\n" +
                "        const elements = [...document.querySelectorAll('.search_prolist_item')];\n" +
                "        elements.forEach(element => {\n" +
                "            const searchProlistAd = element.querySelector('.search_prolist_ad');\n" +
                "            if (searchProlistAd) {\n" +
                "\n" +
                "\n" +
                "                element.remove();\n" +
                "            }\n" +
                "            const searchProlistOther = element.querySelector('.search_prolist_other img');\n" +
                "            if (!searchProlistOther || !searchProlistOther.src.endsWith('c5ab4d78f8bf4d90.png')) {\n" +
                "                element.remove();\n" +
                "            }\n" +
                "\n" +
                "        });\n" +
                "    }\n" +
                "    if (window.location.host.endsWith('m.jd.com')) {\n" +
                "        //" +
                "" +
                "" +
                "" +
                "" +
                "deleteRedundantItems();\n" +
                "        window.addEventListener('scroll', evt => {\n" +
                "            deleteRedundantItems()\n" +
                "        })\n" +
                "    }\n" +
                "})();", null);
    }


    @Override
    @SuppressWarnings("deprecation")
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return super.shouldInterceptRequest(view, url);

    }


    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        if ((url.startsWith("https://") || url.startsWith("http://") || url.startsWith("file://"))) {
            view.loadUrl(url);
        }
        return true;
    }




}