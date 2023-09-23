package psycho.euphoria.v;

import android.util.Log;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import psycho.euphoria.v.VideoDatabase.Video;

public class Extractor {
    private final static String USER_AGENT_IPHONE_X = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1";
    private final static String DEFAULT_HOSTNAME = "127.0.0.1";
    private final static int DEFAULT_PORT = 10809;

    public static final String CABEL_TV_HOME_PAGE = "https://cableav.tv";

    public static Video CableAv(String uri, String userAgent, String cookie) throws Exception {
        // new Proxy(Type.HTTP, new InetSocketAddress(DEFAULT_HOSTNAME, DEFAULT_PORT)
        HttpsURLConnection c = (HttpsURLConnection) new URL(uri).openConnection();
        c.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        c.addRequestProperty("Accept-encoding", "gzip, deflate, br");
        c.addRequestProperty("User-Agent", userAgent);
        c.addRequestProperty("Cookie", cookie);
        c.addRequestProperty("Referer", "https://cableav.tv/category/chinese-live-porn/");
        Log.e("B5aOx2", String.format("CableAv, %s\n%s\n%s", c.getResponseCode(),
                userAgent, cookie));
        String contents = Shared.readString(c);
        if (contents == null) return null;
        Video video = new Video();
        video.Thumbnail = Shared.substring(contents, "<meta property=\"og:image\" content=\"", "\"");
        video.Url = uri;
        video.Source = Shared.substring(contents, "<meta property=\"og:type\" content=\"video.other\"><meta property=\"og:video:url\" content=\"", "\"");
        video.Title = Shared.substring(contents, "<meta name=\"description\" content=\"", "\"");
        video.VideoType = 9;
        return video;
    }

    public static boolean checkCableAv(String uri) {
        if (uri == null || uri.isEmpty()) return false;
        return uri.contains("//cableav.tv/");
    }
}