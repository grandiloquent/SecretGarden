package psycho.euphoria.v;

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

    public static Video CableAv(String uri) throws Exception {
        // new Proxy(Type.HTTP, new InetSocketAddress(DEFAULT_HOSTNAME, DEFAULT_PORT)
        HttpURLConnection c = (HttpURLConnection) new URL(uri).openConnection();
        c.addRequestProperty("User-Agent", USER_AGENT_IPHONE_X);
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