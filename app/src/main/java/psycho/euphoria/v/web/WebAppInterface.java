package psycho.euphoria.v.web;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Movie;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import psycho.euphoria.v.Helpers;
import psycho.euphoria.v.MainActivity;
import psycho.euphoria.v.NineOneHelper;
import psycho.euphoria.v.PlayerActivity;
import psycho.euphoria.v.SettingsFragment;
import psycho.euphoria.v.Shared;
import psycho.euphoria.v.Utils;
import psycho.euphoria.v.VideoDatabase;
import psycho.euphoria.v.VideoDatabase.Video;
import psycho.euphoria.v.WebActivity;
import psycho.euphoria.v.tasks.DownloaderService;

import static psycho.euphoria.v.NineOneHelper.process91Porn;
import static psycho.euphoria.v.Utils.saveLog;

public class WebAppInterface {

    private MainActivity mContext;
    SharedPreferences mSharedPreferences;
    TextToSpeech textToSpeech = null;
    private VideoDatabase mVideoDatabase;
    String mAddress;

    public WebAppInterface(MainActivity context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static Intent buildSharedIntent(Context context, File imageFile) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        // https://android.googlesource.com/platform/frameworks/base/+/61ae88e/core/java/android/webkit/MimeTypeMap.java
        sharingIntent.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(Shared.substringAfterLast(imageFile.getName(), ".")));
        Uri uri = PublicFileProvider.getUriForFile(context, "psycho.euphoria.app.files", imageFile);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        return sharingIntent;


    }

    @JavascriptInterface
    public void downloadFile(String fileName, String uri) {
        try {
            DownloadManager dm = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
            Request request = new Request(Uri.parse(uri));
//            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
//                    .setAllowedOverRoaming(false)
//                    .setTitle(fileName)
//                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
//                    .setVisibleInDownloadsUi(false);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            dm.enqueue(request);
        } catch (Exception ignored) {
            Log.e("B5aOx2", String.format("downloadFile, %s", ignored.getMessage()));
        }

    }

    @JavascriptInterface
    public void fetchVideos(int mode, int start, int end) {
        if (mode == 1) {
            new Thread(() -> {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                final String cookie = sharedPreferences.getString(SettingsFragment.KEY_91_COOKIE, null);
                final String userAgent = sharedPreferences.getString(SettingsFragment.KEY_USER_AGENT, null);
                for (int i = start; i < end; i++) {
                    try {
                        List<Video> videos = NineOneHelper.scrap91Porn(i, cookie, userAgent);
                        mVideoDatabase.insertVideos(videos);
                        if (i % 10 == 0 || i + 1 == end)
                            showProgress(String.format("已成功抓取第 %s 页,共 %d 个视频", i + 1, videos.size()));
                    } catch (Exception e) {
                        showProgress(String.format("抓取页面错误：%s", e.getMessage()));
                    }
                }

            }).start();
        } else {
            new Thread(() -> {
                // 500
                for (int i = start; i < end; i++) {
                    try {
                        List<Video> videos = Utils.scrap52Ck(i);
                        mVideoDatabase.insertVideos(videos);
                        if (i % 10 == 0 || i + 1 == end)
                            showProgress(String.format("已成功抓取第 %s 页", i + 1));
                    } catch (Exception e) {
                        showProgress(String.format("抓取页面错误：%s", e.getMessage()));
                    }
                }
            }).start();
        }
    }

    @JavascriptInterface
    public String getRealAddress() {
        if (mAddress == null) {
            Thread thread = new Thread(() -> {
                mAddress = Utils.getRealAddress();
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return mAddress;
    }

    @JavascriptInterface
    public String getString(String key) {
        return mSharedPreferences.getString(key, "");
    }

    @JavascriptInterface
    public String getTitle(String uri) {
        final StringBuilder sb = new StringBuilder();
        try {
            Thread thread = new Thread(() -> {
                try {
                    HttpURLConnection h = (HttpURLConnection) new URL(uri).openConnection();
                    h.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36 Edg/88.0.705.74");
                    String s = Shared.readString(h);
                    s = Shared.substringAfter(s, "<title>");
                    s = Shared.substringBefore(s, "</title>");
                    sb.append(s);
                } catch (Exception e) {
                }
            });
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    @JavascriptInterface
    public String getVideoAddress(String url) {
        AtomicReference<String> result = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                String res = Shared.readString(connection);
                res = Shared.substringAfter(res, "sl: \"");
                res = Shared.substringBefore(res, "\"");
                result.set(res);
            } catch (Exception ignored) {
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result.get();
    }

    @JavascriptInterface
    public void launchApp(String text) {
        Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(text);
        if (launchIntent != null) {
            mContext.startActivity(launchIntent);//null pointer check in case package name was not found
        }
    }

    @JavascriptInterface
    public void launchApp(String text, String uri) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //intent.setPackage("psycho.euphoria.l");
        intent.setClassName("psycho.euphoria.l",
                "psycho.euphoria.l.MainActivity");
        if (uri.startsWith("https://") || uri.startsWith("http://"))
            intent.setData(Uri.parse(uri));
        else
            intent.setData(Uri.parse(String.format("http://%s:8090%s", Shared.getDeviceIP(mContext), uri)));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mContext.startActivity(intent);//null pointer check in case package name was not found

    }

    @JavascriptInterface
    public String listAllPackages() {
        // get list of all the apps installed
        List<ApplicationInfo> infos = mContext.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        // create a list with size of total number of apps
        String[] apps = new String[infos.size()];
        int i = 0;
        // add all the app name in string list
        for (ApplicationInfo info : infos) {
            apps[i] = info.packageName;
            i++;
        }
        return Arrays.stream(apps).sorted(String::compareTo).collect(Collectors.joining("\n"));
    }

    @JavascriptInterface
    public String loadVideos(String search, int sort, int videoType, int limit, int offset) {
        if (mVideoDatabase == null)
            mVideoDatabase = new VideoDatabase(mContext,
                    new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "videos.db").getAbsolutePath());
        List<Video> videos = mVideoDatabase.queryVideos(search, sort, videoType, limit, offset);
        JSONArray array = new JSONArray();
        for (Video video : videos) {
            try {
                JSONObject object = new JSONObject();
                object.put("id", video.Id);
                object.put("thumbnail", video.Thumbnail);
                object.put("title", video.Title);
                object.put("duration", video.Duration);
                object.put("views", video.Views);
                object.put("createAt", video.CreateAt);
                array.put(object);
            } catch (Exception e) {
            }
        }
        return array.toString();
    }

    @JavascriptInterface
    public int indexOfVideo(int sort, int videoType, int id) {
        if (mVideoDatabase == null)
            mVideoDatabase = new VideoDatabase(mContext,
                    new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "videos.db").getAbsolutePath());
        List<Video> videos = mVideoDatabase.queryVideos(null, sort, videoType, 100000, 0);
        for (int i = 0; i < videos.size(); i++) {
            if (videos.get(i).Id == id) {
                return i;
            }
        }
        return 0;
    }

    @JavascriptInterface
    public void moveVideo(int id, int videoType) {
        if (mVideoDatabase == null)
            mVideoDatabase = new VideoDatabase(mContext,
                    new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "videos.db").getAbsolutePath());
        mVideoDatabase.updateVideoType(id, videoType);
    }

    @JavascriptInterface
    public void openFile(String path) {
        mContext.runOnUiThread(() -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(path)), MimeTypeMap.getSingleton().getMimeTypeFromExtension(Shared.substringAfterLast(path, ".")));
            mContext.startActivity(Intent.createChooser(intent, "打开"));
        });
    }

    @JavascriptInterface
    public void play(int id) {
        if (mVideoDatabase == null)
            mVideoDatabase = new VideoDatabase(mContext,
                    new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "videos.db").getAbsolutePath());
        new Thread(() -> {
            Video video = mVideoDatabase.queryVideoSource(id);
            String source = null;
            Video old = mVideoDatabase.queryVideoSource(video.Id);
            if (TextUtils.isEmpty(old.Source)) {
                String[] videos = null;
                if (video.Url.startsWith("/")) {
                    videos = WebActivity.processCk(mContext, Utils.getRealAddress() + video.Url);
                } else {
                    videos = process91Porn(mContext, video.Url);
                    ;
                }
                if (videos != null) {
                    source = videos[1];
                }
                if (!TextUtils.isEmpty(source)) {
                    mVideoDatabase.updateVideoSource(video.Id, videos);
                }
            } else {
                source = old.Source;
            }
            if (!TextUtils.isEmpty(source)) {
                mVideoDatabase.updateViews(video.Id);
                String finalSource = source;
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PlayerActivity.launchActivity(mContext, finalSource, video.Title);
                    }
                });
            } else {
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Helpers.tryGetCookie(mContext, video);
                    }
                });
            }
        }).start();
    }

    @JavascriptInterface
    public String readText() {
        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData.getItemCount() > 0) {
            CharSequence sequence = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (sequence != null) return sequence.toString();
        }
        return null;
    }

    @JavascriptInterface
    public void refreshVideo(int id) {
        if (mVideoDatabase == null)
            mVideoDatabase = new VideoDatabase(mContext,
                    new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "videos.db").getAbsolutePath());
        Thread thread = new Thread(() -> {
            Video video = mVideoDatabase.queryVideoSource(id);
            Helpers.updateSource(mContext, mVideoDatabase, video);
            mContext.runOnUiThread(() -> {
                Toast.makeText(mContext, "成功", Toast.LENGTH_SHORT).show();
            });
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @JavascriptInterface
    public void scanFile(String fileName) {
        MediaScannerConnection.scanFile(mContext, new String[]{fileName}, new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension(Shared.substringAfterLast(fileName, "."))}, new MediaScannerConnectionClient() {
            @Override
            public void onMediaScannerConnected() {
                Log.e("B5aOx2", String.format("onMediaScannerConnected, %s", ""));
            }

            @Override
            public void onScanCompleted(String s, Uri uri) {
            }
        });
    }

    @JavascriptInterface
    public void setString(String key, String value) {
        mSharedPreferences.edit().putString(key, value).apply();
    }

    @JavascriptInterface
    public void share(String path) {
        try {
            mContext.startActivity(buildSharedIntent(mContext, new File(path)));
        } catch (Exception ignored) {
        }
    }

    public void showProgress(String message) {
        mContext.runOnUiThread(() -> Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show());
    }

    @JavascriptInterface
    public void speak(String s) {
        textToSpeech = new TextToSpeech(mContext, new OnInitListener() {
            @Override
            public void onInit(int status) {
                Log.e("B5aOx2", String.format("onInit, %s", status));
                textToSpeech.setLanguage(Locale.CHINA);
                Log.e("B5aOx2", String.format("onInit, %s", textToSpeech.setLanguage(Locale.CHINA)));
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textToSpeech.speak(s, TextToSpeech.QUEUE_FLUSH, null);
                    }
                });
            }
        });
    }

    @JavascriptInterface
    public void switchInputMethod() {
        ((InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
    }

    @JavascriptInterface
    public String translate(String s, String to) {
        final StringBuilder sb = new StringBuilder();
        try {
            Thread thread = new Thread(() -> {
                String uri = "http://translate.google.com/translate_a/single?client=gtx&sl=auto&tl=" + to + "&dt=t&dt=bd&ie=UTF-8&oe=UTF-8&dj=1&source=icon&q=" + Uri.encode(s);
                try {
                    HttpURLConnection h = (HttpURLConnection) new URL(uri).openConnection(new Proxy(Type.HTTP, new InetSocketAddress("127.0.0.1", 10809)));
                    h.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36 Edg/88.0.705.74");
                    h.addRequestProperty("Accept-Encoding", "gzip, deflate, br");
                    String line = null;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(h.getInputStream())));
                    StringBuilder sb1 = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        sb1.append(line).append('\n');
                    }
                    JSONObject object = new JSONObject(sb1.toString());
                    JSONArray array = object.getJSONArray("sentences");
                    for (int i = 0; i < array.length(); i++) {
                        sb.append(array.getJSONObject(i).getString("trans"));
                    }
                } catch (Exception e) {
                }
            });
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    @JavascriptInterface
    public void writeText(String text) {
        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("demo", text);
        clipboard.setPrimaryClip(clip);
    }

    private static void openLocalPage(Context context, String path) {
        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage("com.android.chrome");
        launchIntent.setData(Uri.parse("http://" + Shared.getDeviceIP(context) + ":8500" + path));
        context.startActivity(launchIntent);
    }

    @JavascriptInterface
    public void downloadVideo(int id) {
        if (mVideoDatabase == null)
            mVideoDatabase = new VideoDatabase(mContext,
                    new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "videos.db").getAbsolutePath());
        Video video = mVideoDatabase.queryVideoSource(id);
        new Thread(() -> {
            if (video.Source == null) {
                // Attempt to resolve the address of the video file
                String[] videos = process91Porn(mContext, video.Url);
                if (videos != null && videos[1] != null) {
                    mVideoDatabase.updateVideoSource(video.Id, videos);
                    video.Source = videos[1];
                } else {
                    return;
                }
            }
            Intent starter = new Intent(mContext, DownloaderService.class);
            starter.putExtra(DownloaderService.EXTRA_VIDEO_TITLE, video.Title);
            starter.putExtra(DownloaderService.EXTRA_VIDEO_ADDRESS, video.Source);
            mContext.startService(starter);
        }).start();
    }

    private String mImageHost;

    @JavascriptInterface
    public String getImageHost(int id) {
        if (mVideoDatabase == null)
            mVideoDatabase = new VideoDatabase(mContext,
                    new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "videos.db").getAbsolutePath());
        Video video = mVideoDatabase.queryVideoSource(id);
        Thread t = new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(getRealAddress() + video.Url).openConnection();
                String s = Shared.readString(c);
                mImageHost = Shared.substring(s, "data-original=\"", "/images");
            } catch (Exception e) {
            }

        });
        t.start();
        try {
            t.join();
        } catch (Exception e) {
        }
        return mImageHost;

    }
}