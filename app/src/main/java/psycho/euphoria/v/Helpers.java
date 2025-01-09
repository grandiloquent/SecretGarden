package psycho.euphoria.v;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Pair;

import psycho.euphoria.v.VideoDatabase.Video;

import static psycho.euphoria.v.NineOneHelper.process91Porn;

public class Helpers {
    public static void tryGetCookie(Context context, Video video) {
        Intent v = new Intent(context, TestActivity.class);
        v.putExtra("url", video.Url.startsWith("/") ? Utils.getRealAddress() + video.Url : video.Url);
        context.startActivity(v);
    }

    public static boolean updateSource(Context context, VideoDatabase videoDatabase, Video video) {
        String[] videos = null;
        if (video.Url.startsWith("/")) {
            videos = WebActivity.processCk(context, Utils.getRealAddress() + video.Url);
        } else {
            videos = process91Porn(context, video.Url);
        }
        if (videos != null && !TextUtils.isEmpty(videos[1])) {
            videoDatabase.updateVideoSource(video.Id, videos);
            return true;
        }
        return false;

    }
}
