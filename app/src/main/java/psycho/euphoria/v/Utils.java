package psycho.euphoria.v;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Process;
import android.util.Log;
import android.util.Pair;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import psycho.euphoria.v.VideoDatabase.Video;

public class Utils {

    private static String mRealAddress;

    public static int findFixedPart(String IPPrefix, int i) {
        String f = IPPrefix.split("\\.")[i];
        return Integer.valueOf(f);
    }

    public static String getRealAddress() {
        if (mRealAddress == null) {
            try {
                mRealAddress = getRealAddressInternal();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mRealAddress;
    }

    public static int findRange(int mask) {
        int x = 8 - mask;
        int sum = 0;
        for (int i = 0; i < x; i++) {
            sum += Math.pow(2, i);
        }
        return sum;
    }

    public static String generateRandomIP(String IPPrefix, Integer mask) {
        String IP = "";
        Random r = new Random();
        if (mask < 8)
            IP = (findFixedPart(IPPrefix, 0) + r.nextInt(findRange(mask))) + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256);
        else if (mask > 7 && mask < 16)
            IP = findFixedPart(IPPrefix, 0) + "." + (findFixedPart(IPPrefix, 1) + r.nextInt(findRange(mask - 8))) + "." + r.nextInt(256) + "." + r.nextInt(256);
        else if (mask > 15 && mask < 24)
            IP = findFixedPart(IPPrefix, 0) + "." + findFixedPart(IPPrefix, 1) + "." + (findFixedPart(IPPrefix, 2) + r.nextInt(findRange(mask - 16))) + "." + r.nextInt(256);
        else if (mask > 23 && mask < 33)
            IP = findFixedPart(IPPrefix, 0) + "." + findFixedPart(IPPrefix, 1) + "." + findFixedPart(IPPrefix, 2) + "." + (findFixedPart(IPPrefix, 3) + r.nextInt(findRange(mask - 24)));
        return IP;
    }

    public static String getCookie(String uri, String[][] headers) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        if (headers != null) {
            for (String[] header : headers) {
                urlConnection.setRequestProperty(header[0], header[1]);
            }
        }
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36");
        urlConnection.setInstanceFollowRedirects(false);
        for (Entry<String, List<String>> header : urlConnection.getRequestProperties().entrySet()) {
            Log.e("B5aOx2", String.format("getCookie, %s", header.getKey()));
        }
        Map<String, List<String>> listMap = urlConnection.getHeaderFields();
        StringBuilder stringBuilder = new StringBuilder();
        for (Entry<String, List<String>> header : listMap.entrySet()) {
            Log.e("B5aOx2", String.format("getCookie, %s", header.getKey()));
            if (header.getKey() != null && header.getKey().equalsIgnoreCase("set-cookie")) {
                for (String s : header.getValue()) {
                    stringBuilder.append(Shared.substringBefore(s, "; "))
                            .append("; ");
                }
            }
        }
        return stringBuilder.toString();
    }

    public static String getDouYinString(String videoId) throws IOException {
        URL url = new URL("https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=" + videoId);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36");
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return Shared.readString(urlConnection);
        } else {
            return null;
        }
    }

    public static boolean getDouYinVideo(Activity activity, String query) {
        Pattern douyin = Pattern.compile("douyin");
        Matcher matcher = douyin.matcher(query);
        if (matcher.find()) {
            ProgressDialog dialog = new ProgressDialog(activity);
            dialog.setMessage("解析中...");
            dialog.show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    String location;
                    String response = null;
                    try {
                        location = getLocation("https://" + Shared.substring(query, "https://", " "));
                        String videoId = Shared.substring(location, "video/", "/?");
                        response = getDouYinString(videoId);
                        JSONObject object = new JSONObject(response);
                        response = object.getJSONArray("item_list").getJSONObject(0)
                                .getJSONObject("video")
                                .getJSONObject("play_addr")
                                .getJSONArray("url_list")
                                .getString(0);
                        response = response.replace("playwm", "play");


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String finalResponse = response;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Shared.downloadFile(activity,
                                    Shared.md5(query) + ".mp4",
                                    finalResponse,
                                    Shared.USER_AGENT);
                        }
                    });
                }
            }).start();
            return true;
        }
        return false;
    }

    public static boolean getKuaiShouVideo(Activity activity, String query) {
        Pattern douyin = Pattern.compile("kuaishou");
        Matcher matcher = douyin.matcher(query);
        if (matcher.find()) {
            ProgressDialog dialog = new ProgressDialog(activity);
            dialog.setMessage("解析中...");
            dialog.show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    String[] location;
                    String response = null;
                    try {
                        location = getLocationAddCookie("https://" + Shared.substring(query, "https://", " "), null);
                        location = getLocationAddCookie(location[0], null);
                        response = getString(location);
                        response = Shared.substring(response, "\"srcNoMark\":\"", "\"");
                    } catch (Exception e) {
                    }
                    String finalResponse = response;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Shared.downloadFile(activity,
                                    Shared.md5(query) + ".mp4",
                                    finalResponse,
                                    Shared.USER_AGENT);
                            dialog.dismiss();
                        }
                    });
                }
            }).start();
            return true;
        }
        return false;
    }

    public static String getLocation(String uri) throws IOException {
        URL url = new URL(uri);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36");
        urlConnection.setInstanceFollowRedirects(false);
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return urlConnection.getHeaderField("Location");
        } else {
            return null;
        }
    }

    public static String[] getLocationAddCookie(String uri, String[][] headers) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Cookie", "did=web_72fb31b9cb57408aa7bd20e63183ac49; didv=1640743103000; clientid=3");
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36");
        if (headers != null) {
            for (String[] header : headers) {
                urlConnection.setRequestProperty(header[0], header[1]);
            }
        }
        urlConnection.setInstanceFollowRedirects(false);
        Map<String, List<String>> listMap = urlConnection.getHeaderFields();
        StringBuilder stringBuilder = new StringBuilder();
        for (Entry<String, List<String>> header : listMap.entrySet()) {
            if (header.getKey() != null && header.getKey().equalsIgnoreCase("set-cookie")) {
                for (String s : header.getValue()) {
                    stringBuilder.append(Shared.substringBefore(s, "; "))
                            .append("; ");
                }
            }
        }
        return new String[]{urlConnection.getHeaderField("Location"), stringBuilder.toString()};
    }

    private static String getRealAddressInternal() throws Exception {
        HttpsURLConnection u = (HttpsURLConnection) new URL("https://888tttz.com:8899/?u=http://52ck.cc/&p=/")
                .openConnection();
        u.setInstanceFollowRedirects(false);
        if (u.getResponseCode() == 302) {
            return u.getHeaderField("Location");
        }
        return null;
    }

    public static String getString(String[] uri) throws IOException {
        URL url = new URL("https:" + uri[0]);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.addRequestProperty("Cookie", "did=web_72fb31b9cb57408aa7bd20e63183ac49; didv=1640743103000; clientid=3");
        urlConnection.setRequestProperty("User-Agent", Shared.USER_AGENT);
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return Shared.readString(urlConnection);
        } else {
            return null;
        }
    }

    public static String getString(String uri, String[][] headers) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        if (headers != null) {
            for (String[] header : headers) {
                urlConnection.setRequestProperty(header[0], header[1]);
            }
        }
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return Shared.readString(urlConnection);
        } else {
            return null;
        }
    }

    public static VersionInfo getVersionInformation() {
        VersionInfo versionInfo = new VersionInfo();
        try {
            String response = Shared.getString("https://www.hxz315.com/version.json", null, false, false).contents;
            JSONObject object = new JSONObject(response);
            object = object.getJSONObject("Secret Garden");
            versionInfo.versionCode = object.getInt("VersionCode");
            versionInfo.downloadLink = object.getString("DownloadLink");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionInfo;
    }

    public static String getXVideosString(String uri) throws IOException {
        URL url = new URL(uri);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36");
        int code = urlConnection.getResponseCode();
        if (code < 400 && code >= 200) {
            return Shared.readString(urlConnection);
        } else {
            return null;
        }
    }

    public static String[] getXVideosVideoAddress(String uri) throws IOException {
        String response = getXVideosString(uri);
        if (response == null) {
            return null;
        }
        String title = Shared.substring(response, "html5player.setVideoTitle('", "');");
        String hlsAddress = Shared.substring(response, "html5player.setVideoHLS('", "');");
        String hls = getXVideosString(hlsAddress);
        if (hls == null) {
            return null;
        }
        String[] lines = hls.split("\n");
        List<Pair<Integer, String>> videos = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].contains("#EXT-X-STREAM-INF")) {
                continue;
            }
            videos.add(Pair.create(
                    Integer.parseInt(Shared.substring(lines[i], "NAME=\"", "p\"")),
                    lines[i + 1]
            ));
            i++;
        }
        Collections.sort(videos, (o1, o2) -> o2.first - o1.first);
        return new String[]{title, Shared.substringBeforeLast(hlsAddress, "/") + "/" + videos.get(0).second};
    }

    public static List<Video> scrap52Ck(int page) throws Exception {
        if (mRealAddress == null)
            mRealAddress = getRealAddress();
        Log.e("B5aOx2", String.format("scrap52Ck, %s", mRealAddress));
        String home = page == 0 ? String.format("%s/vodtype/2.html", mRealAddress)
                : String.format("%s/vodtype/2-%d.html", mRealAddress, page);
        HttpURLConnection u = (HttpURLConnection) new URL(home).openConnection();
        u.addRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        u.addRequestProperty("Accept-Encoding", "gzip, deflate");
        u.addRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
        u.addRequestProperty("Cache-Control", "no-cache");
        u.addRequestProperty("Cookie",
                "Hm_lvt_7de8aab9069dc716bfdaa8d21d28b4da=1659408408; Hm_lpvt_7de8aab9069dc716bfdaa8d21d28b4da=1659411295");
        u.addRequestProperty("Pragma", "no-cache");
        u.addRequestProperty("Proxy-Connection", "keep-alive");
        u.addRequestProperty("Upgrade-Insecure-Requests", "1");
        u.addRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36");
        if (u.getResponseCode() != 200) {
            throw new IllegalStateException(Integer.toString(u.getResponseCode()));
        }
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(u.getInputStream())));
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append('\n');
        }
        List<Video> vs = new ArrayList<>();
        Document document = Jsoup.parse(stringBuilder.toString());
        Elements videos = document.select(".stui-vodlist li");
        Pattern pattern = Pattern.compile("(\\d{4})/(\\d{2})/(\\d{2})");
        for (Element v : videos) {
            Video video = new Video();
            video.Title = v.select(".title a").attr("title");
            video.Thumbnail = v.select(".stui-vodlist__thumb").attr("data-original");
            try {
                video.Duration = toSeconds(v.select(".stui-vodlist__thumb .text-right").text().trim());
            } catch (Exception e) {
                video.Duration = 0;
            }
            Matcher matcher = pattern.matcher(video.Thumbnail);
            if (matcher.find()) {
                Calendar calendar = Calendar.getInstance(Locale.CHINA);
                calendar.set(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)) - 1,
                        Integer.parseInt(matcher.group(3)), 0, 0, 0);
                video.CreateAt = calendar.getTimeInMillis() / 1000;
            }
            String href = v.select(".stui-vodlist__thumb").attr("href");
            video.Url = Shared.substringBefore(href, "&");
            video.VideoType = 2;
            vs.add(video);
        }
        return vs;
    }


    public static void setSearchViewStyle(SearchView searchView) {
        LinearLayout linearLayout = ((LinearLayout) searchView.getChildAt(0));
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            if (linearLayout.getChildAt(i) instanceof ImageView) {
                ((ImageView) linearLayout.getChildAt(i))
                        .setColorFilter(Color.WHITE,
                                android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }
        try {
            Field d = SearchView.class.getDeclaredField("mSearchHintIcon");
            d.setAccessible(true);
            Drawable db = (Drawable) d.get(searchView);
            db.setColorFilter(Color.WHITE,
                    android.graphics.PorterDuff.Mode.SRC_IN);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Resources resources = searchView.getContext().getResources();
        ImageView searchClose = searchView.findViewById(resources.getIdentifier(
                "android:id/search_close_btn",
                null, null));
        if (searchClose != null)
            searchClose.setColorFilter(Color.WHITE,
                    android.graphics.PorterDuff.Mode.SRC_IN);
        TextView textView = searchView.findViewById(resources.getIdentifier("android:id/search_src_text", null, null));
        textView.setTextColor(Color.WHITE);
    }

    public static void startVideoList(Context context) {
        Intent starter = new Intent(context, VideoListActivity.class);
        context.startActivity(starter);
    }

    public static int toSeconds(String duration) {
        String[] pieces = duration.split(":");
        int total = 0;
        int max = pieces.length - 1;
        int j = max;
        for (int i = 0; i < max + 1; i++) {
            total += Integer.parseInt(pieces[i]) * Math.pow(60, j);
            j--;
        }
        return total;
    }

    public static void saveLog(int id, String title, String content) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://0.0.0.0:8500/note").openConnection();
        c.setRequestMethod("POST");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("title", title);
        jsonObject.put("content", content);
        OutputStream os = c.getOutputStream();
        os.write(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
        os.close();
        String response = Shared.readString(c);

    }
}
