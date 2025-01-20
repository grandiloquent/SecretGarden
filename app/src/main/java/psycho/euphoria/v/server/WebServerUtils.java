package psycho.euphoria.v.server;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import psycho.euphoria.v.Shared;
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
//1

}