package psycho.euphoria.v.server;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import psycho.euphoria.v.Helpers;
import psycho.euphoria.v.Shared;
import psycho.euphoria.v.VideoDatabase;
import psycho.euphoria.v.VideoDatabase.Video;
import psycho.euphoria.v.server.NanoHTTPD.IHTTPSession;
import psycho.euphoria.v.server.NanoHTTPD.Response;
import psycho.euphoria.v.server.NanoHTTPD.Response.Status;
import psycho.euphoria.v.tasks.Database;

import static psycho.euphoria.v.server.NanoHTTPD.MIME_PLAINTEXT;


public class WebServerUtils {


    public static Response assetFiles(Context context, String uri) {
        Pattern pattern = Pattern.compile("\\.(?:js|css|html|svg|png|jpg|json|txt|md|webp|ico)$");
        Matcher matcher = pattern.matcher(uri);
        if (matcher.find()) {
            try {
                InputStream in = context.getAssets().open(uri);
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        Shared.substringAfterLast(uri, ".")
                );
                return new Response(Status.OK, mimeType, in);
            } catch (IOException e) {
                return error(e);

            }
        }
        return null;
    }


    public static Response error(Exception e) {
        return new Response(Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
    }

    public static Response get(IHTTPSession session, Database database, int mode) {
        try {
            int id = Integer.parseInt(session.getParms().get("id"));

        } catch (Exception e) {
            return error(e);
        }
        return null;
    }

    public static Response json(String contents) {
        return new Response(Status.OK, "application/json", contents);
    }

    public static Response list(Database database, int mode) {
        try {
        } catch (Exception e) {
            return error(e);
        }
        return null;
    }

    public static String loadVideos(VideoDatabase database, String search, int sort, int videoType, int limit, int offset) {
        List<Video> videos = database.queryVideos(search, sort, videoType, limit, offset);
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

    public static Response loadVideos(IHTTPSession session, VideoDatabase database) {
        Map<String, String> parameters = session.getParms();
        String search = null;
        if (parameters.containsKey("search")) {
            search = parameters.get("search");
            if (TextUtils.isEmpty(search))
                search = null;
        }
        int sort = 0;
        if (parameters.containsKey("sort")) {
            try {
                sort = Integer.parseInt(parameters.get("sort"));
            } catch (Exception e) {
            }
        }
        int videoType = 0;
        if (parameters.containsKey("videoType")) {
            try {
                videoType = Integer.parseInt(parameters.get("videoType"));
            } catch (Exception e) {
            }
        }
        int limit = 0;
        if (parameters.containsKey("limit")) {
            try {
                limit = Integer.parseInt(parameters.get("limit"));
            } catch (Exception e) {
            }
        }
        int offset = 0;
        if (parameters.containsKey("offset")) {
            try {
                offset = Integer.parseInt(parameters.get("offset"));
            } catch (Exception e) {
            }
        }
        String videos = loadVideos(database, search, sort, videoType, limit, offset);
        return json(videos);
    }

    public static Response put(IHTTPSession session, Database database, int mode) {
        try {
            String contents = readString(session);
            return new Response(Status.OK, MIME_PLAINTEXT, "Ok");
        } catch (Exception e) {
            return error(e);
        }
    }

    public static String readString(IHTTPSession session) throws Exception {
        Integer contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
        byte[] buffer = new byte[contentLength];
        session.getInputStream().read(buffer, 0, contentLength);
        return new String(buffer);
    }

    public static Bitmap resizeBitmapByScale(
            Bitmap bitmap, float scale, boolean recycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width == bitmap.getWidth()
                && height == bitmap.getHeight()) return bitmap;
        Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap));
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) bitmap.recycle();
        return target;
    }

    public static Bitmap resizeDownBySideLength(
            Bitmap bitmap, int maxLength, boolean recycle) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();
        float scale = Math.min(
                (float) maxLength / srcWidth, (float) maxLength / srcHeight);
        if (scale >= 1.0f) return bitmap;
        return resizeBitmapByScale(bitmap, scale, recycle);
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    public static Response refreshVideo(Context context, VideoDatabase database, int id) {
        Video video = database.queryVideoSource(id);
        String res = Helpers.updateSource(context, database, video);
        return json(res);
    }
}