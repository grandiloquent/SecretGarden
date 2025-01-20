package psycho.euphoria.v.server;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import psycho.euphoria.v.Shared;
import psycho.euphoria.v.VideoDatabase;
import psycho.euphoria.v.server.NanoHTTPD.IHTTPSession;
import psycho.euphoria.v.server.NanoHTTPD.Method;
import psycho.euphoria.v.server.NanoHTTPD.Response.Status;
import psycho.euphoria.v.tasks.Database;

import static psycho.euphoria.v.server.WebServerUtils.assetFiles;
import static psycho.euphoria.v.server.WebServerUtils.error;
import static psycho.euphoria.v.server.WebServerUtils.loadVideos;


public class WebServer extends NanoHTTPD {
    private VideoDatabase mVideoDatabase;
    private Context mContext;
    private Response mOkResponse;


    public WebServer(Context context, String hostname, int port) {
        super(hostname, port);
        mContext = context;
        mVideoDatabase = new VideoDatabase(mContext,
                new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "videos.db").getAbsolutePath());
        mOkResponse = new Response(Status.OK, MIME_PLAINTEXT, "Ok");

    }

    @Override
    public Response serve(IHTTPSession session) {
        if (session.getMethod() == Method.OPTIONS) {
            return mOkResponse;
        }
        if (session.getUri().equals("/api/videos")) {
            if (session.getMethod() == Method.GET) {
                return loadVideos(session, mVideoDatabase);
            }
        } else if (session.getUri().equals("/api/video")) {
            if (session.getMethod() == Method.GET) {
                Map<String, String> parameters = session.getParms();
                int id;
                if (parameters.containsKey("id")) {
                    try {
                        id = Integer.parseInt(parameters.get("id"));
                        mVideoDatabase.updateDisplay(id);
                        return new Response(Status.OK, MIME_PLAINTEXT, "OK");
                    } catch (Exception e) {
                    }
                }
            } else if (session.getMethod() == Method.PUT) {
                Map<String, String> parameters = session.getParms();
                int id;
                if (parameters.containsKey("id")) {
                    try {
                        id = Integer.parseInt(parameters.get("id"));
                        return WebServerUtils.refreshVideo(mContext, mVideoDatabase, id);
                    } catch (Exception e) {
                    }
                }
            }
        } else {
            Response response = assetFiles(mContext, session.getUri().substring(1));
            if (response != null) return response;
        }
        return new Response(Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
    }

}