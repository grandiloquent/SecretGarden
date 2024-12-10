package psycho.euphoria.v;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.webkit.CookieManager;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import psycho.euphoria.v.VideoDatabase.Video;

import static psycho.euphoria.v.Utils.saveLog;
import static psycho.euphoria.v.Utils.toSeconds;

public class NineOneHelper {
    public static String UserAgent;
    public static String Cookie;


    public static List<Video> scrap91Porn(int page, String cookie, String userAgent) throws Exception {
        String home = "https://91porn.com/index.php";
        if (page != 0) {
            home = "https://91porn.com/v.php?page=" + page;
        }
        HttpURLConnection u = (HttpURLConnection) new URL(home).openConnection();
        u.addRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        u.addRequestProperty("Accept-Encoding", "gzip, deflate");
        u.addRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
        u.addRequestProperty("Cache-Control", "no-cache");
        u.addRequestProperty("Cookie", cookie);
        u.addRequestProperty("Host", "91porn.com");
        u.addRequestProperty("Pragma", "no-cache");
        u.addRequestProperty("Proxy-Connection", "keep-alive");
        u.addRequestProperty("Upgrade-Insecure-Requests", "1");
        u.addRequestProperty("User-Agent", userAgent);
        //saveLog(311, home, Integer.toString(u.getResponseCode()));
        if (u.getResponseCode() != 200) {
            throw new IllegalStateException(Integer.toString(u.getResponseCode()));
        }
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(u.getInputStream())));
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append('\n');
        }
        //saveLog(312, home, stringBuilder.toString());
        Document document = Jsoup.parse(stringBuilder.toString());
        Elements videos = document.select(".videos-text-align");
        ZoneId zoneId = ZoneId.systemDefault();
        Pattern pattern = Pattern.compile("\\s+?(\\d+)\\s+?小*?([时天年])\\s+?前");
        List<Video> vs = new ArrayList<>();
        for (Element v : videos) {
            Video video = new Video();
            video.Title = v.select(".thumb-overlay + span").text();
            if (video.Title == null || video.Title.isEmpty()) continue;
            video.Thumbnail = v.select(".thumb-overlay img").attr("src");
            try {
                video.Duration = toSeconds(v.select(".thumb-overlay .duration").text());
            } catch (Exception e) {
                video.Duration = 0;
            }
            LocalDateTime localDate = LocalDateTime.now();
            Matcher matcher = pattern.matcher(v.html());
            if (matcher.find()) {
                if (matcher.group(2).equals("时")) {
                    localDate = localDate.minusHours(Integer.parseInt(matcher.group(1)));
                } else if (matcher.group(2).equals("天")) {
                    localDate = localDate.minusDays(Integer.parseInt(matcher.group(1)));
                } else if (matcher.group(2).equals("年")) {
                    localDate = localDate.minusYears(Integer.parseInt(matcher.group(1)));
                }
            }
            video.CreateAt = localDate.atZone(zoneId).toEpochSecond();
            String href = v.select("a").attr("href");
            video.Url = Shared.substringBefore(href, "?") + "?viewkey=" + Uri.parse(href).getQueryParameter("viewkey");
            video.VideoType = 1;
            vs.add(video);
        }
        return vs;
    }

    public static Pair<String, String> process91Porn(Context context, String videoAddress) {
        String response = null;
        //Native.fetch91Porn(Uri.parse(videoAddress).getQueryParameter("viewkey"));
//        if (response == null) {
//            return null;
//        }

        String title = null;
        //videoAddress="http://192.168.8.34:8080/";
        try {
            // new Proxy(Type.HTTP, new InetSocketAddress("127.0.0.1", 10809))
            HttpURLConnection c = (HttpURLConnection) new URL(videoAddress).openConnection();
            c.setUseCaches(false);
            c.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            c.addRequestProperty("Connection", "close");
            c.addRequestProperty("Host", "91porn.com");
            // "Mozilla/5.0 (Linux; Android 11; Mi 10 Build/RKQ1.200826.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/90.0.4430.210 Mobile Safari/537.36"
            c.addRequestProperty("User-Agent", UserAgent);  // Mozilla/5.0 (Linux; Android 11; Redmi K30 Build/RKQ1.200826.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/94.0.4606.71 Mobile
            c.addRequestProperty("Accept-Encoding", "gzip, deflate");
            c.addRequestProperty("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8");
            c.addRequestProperty("Upgrade-Insecure-Requests", "1");
            c.addRequestProperty("X-Requested-With", "psycho.euphoria.v");
            Log.e("B5aOx2", String.format("process91Porn, %s", CookieManager.getInstance().getCookie(videoAddress)));
            c.addRequestProperty("Cookie", CookieManager.getInstance().getCookie(videoAddress));

            //c.addRequestProperty("X-Forwarded-For", Shared.generateRandomIp());
            //c.setInstanceFollowRedirects(false);
            //saveLog(311, videoAddress, Integer.toString(c.getResponseCode()));
            // if (c.getResponseCode() != 200) {
//                BufferedReader reader = new BufferedReader(new InputStreamReader(c.getErrorStream()));
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    //Log.e("B5aOx2", String.format("process91Porn, %s", line));
//                }
//                reader.close();
            //   return null;
            // }
            response = Shared.readString(c);
            if (response == null) {
                return null;
            }
            //saveLog(312, videoAddress, response);
            String needle = "document.write(strencode2(\"";
            if (response.contains(needle)) {
                title = Shared.substring(response, "<title>", "Chinese homemade video");
                response = Shared.substring(response, needle, "\"");
                response = Uri.decode(response);
                response = Shared.substring(response, "<source src='", "'");
            } else {
                saveLog(312, videoAddress, response);
                CookieManager.getInstance().removeAllCookie();
                title = Shared.substring(response, "<title>", "Chinese homemade video");
                response = Shared.substring(response, "encryptedUrl: '", "'");
                response = get91PornVideo(response);
            }

        } catch (Exception e) {
            Log.e("B5aOx2", String.format("process91Porn, %s>>>>>>>>>>>>>>>", e));
        }
        //title
        if (title == null || response == null) return null;
        return Pair.create(title.trim(), response);
//        JSONObject jsonObject = null;
//        try {
//            jsonObject = new JSONObject(response);
//            String title = jsonObject.getString("title");
//            String src = jsonObject.getString("videoUri");
//            return Pair.create(title, src);
//        } catch (JSONException ignored) {
//            Log.e("B5aOx2", String.format("process91Porn, %s", ignored));
//        }
        //return null;
    }

    public static String get91PornVideo(String encryptedUrl) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("https://91porn.com/get_decrypted_video.php").openConnection(new Proxy(Type.HTTP, new InetSocketAddress("127.0.0.1", 10809)));
            c.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            c.addRequestProperty("Accept-Encoding", "gzip, deflate");
            c.addRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
            c.addRequestProperty("Cache-Control", "max-age=0");
            c.addRequestProperty("Host", "91porn.com");
            c.addRequestProperty("Proxy-Connection", "keep-alive");
            c.addRequestProperty("Upgrade-Insecure-Requests", "1");
            c.addRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Mobile Safari/537.36");  // Mozilla/5.0 (Linux; Android 11; Redmi K30 Build/RKQ1.200826.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/94.0.4606.71 Mobile
            //c.addRequestProperty("Cookie", "CLIPSHARE=go7ituvaubvo9mr85sls0i13hc; cf_clearance=0toFCitHSJbTZ.51xaqL5fgtTDtdhx4O41lJIxg_by0-1730997079-1.2.1.1-0t6tdfXd5Nr5o_gkeQwIHFOddvk7IvYs5Fl3gvA9jKOGfuj6S65MBncppbTrkYOgqIlMDWVjBl7MgRq1W5T8Yex3svpKjvcKCYQqhW2PQ0UOYScMHUTsUwMtopm.rWd54StoFz6SzaCG3nYB0dIXRVtZOkCJUPSeq.mEWCpS4IwlA2.C008XtZRR5zSDuLS66Esg8LAlhlejli2KCpiDrSiMF60LnG78xtncQZlebaqom1Xpqd36kaPeQ4jQOBWSms2hD47jwYO9XbHqUbe2_McTVad.ETN5G.jvfQGc7oRhhCWwhgvo2SPnzMXyLhG5b_RZ3pnpo7S0qNkrfsbcf5XlAiTZi6gGo_P8eQ9r5ikN3lVxEXj8Yzjf1hq5w39V_3qZoJXna.lty.kASCd9ZQ");
            c.addRequestProperty("X-Forwarded-For", Shared.generateRandomIp());
            c.setRequestMethod("POST");
            OutputStream os = c.getOutputStream();
            os.write(String.format("{\"encryptedUrl\":\"%s\"}", encryptedUrl).getBytes(StandardCharsets.UTF_8));
            String response = Shared.readString(c);
            JSONObject jsonObject = new JSONObject(response);
            return jsonObject.getString("videoUrl");
        } catch (Exception e) {
            return null;
        }

    }
}