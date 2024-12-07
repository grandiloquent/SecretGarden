package psycho.euphoria.v;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class VideoDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    public VideoDatabase(Context context, String name) {
        super(context, name, null, DATABASE_VERSION);
    }

    public void updateVideoType(int id, int i) {
        getWritableDatabase().execSQL("update videos set video_type = ?,update_at = ? where id = ?", new String[]{
                Integer.toString(i),
                Long.toString(System.currentTimeMillis()),
                Integer.toString(id)
        });
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("    CREATE TABLE \"videos\" (\n" +
                "            \"id\"            INTEGER PRIMARY KEY,\n" +
                "            \"title\"            TEXT NOT NULL,\n" +
                "            \"url\"            TEXT UNIQUE,\n" +
                "            \"thumbnail\"            TEXT,\n" +
                "            \"source\"            TEXT,\n" +
                "              \"width\"            INTEGER,\n" +
                "            \"height\"            INTEGER,\n" +
                "            \"duration\"            INTEGER,\n" +
                "            \"hidden\"            INTEGER,\n" +
                "            \"video_type\"            INTEGER,\n" +
                "            \"create_at\"            INTEGER,\n" +
                "            \"update_at\"            INTEGER,\n" +
                "            \"views\"            INTEGER);");
    }

    public void insertVideos(List<Video> videos) {
        SQLiteDatabase db = getWritableDatabase();
        SQLiteDatabase reader = getReadableDatabase();
        db.beginTransaction();
        try {
            for (Video video : videos) {
                Cursor c = reader.rawQuery("select id from videos where url = ?", new String[]{video.Url});
                if (c.moveToNext()) {
                    db.execSQL("update videos set create_at = ? where url = ?", new String[]{
                            Long.toString(video.CreateAt)
                            , video.Url});
                    c.close();
                    continue;
                } //,video_type = 2
                ContentValues values = new ContentValues();
                values.put("title", video.Title);
                values.put("url", video.Url);
                values.put("thumbnail", video.Thumbnail);
                values.put("source", video.Source);
                values.put("width", video.Width);
                values.put("height", video.Height);
                values.put("duration", video.Duration);
                values.put("hidden", video.Hidden);
                values.put("video_type", video.VideoType);
                values.put("create_at", video.CreateAt);
                values.put("update_at", System.currentTimeMillis());
                db.insertWithOnConflict("videos", null, values, SQLiteDatabase.CONFLICT_IGNORE);

            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateThumbnails() {
        Cursor cursor = getReadableDatabase().rawQuery("select id,thumbnail from videos where video_type = 2", null);
        SQLiteDatabase wd = getWritableDatabase();
        while (cursor.moveToNext()) {
            String thumbnail = cursor.getString(1);
            if (thumbnail.startsWith("https://249999.xyz/")) {
                thumbnail = thumbnail.replace("https://249999.xyz/", "https://666548.xyz/");
                wd.execSQL("update videos set thumbnail = ? where id = ?", new String[]{
                        thumbnail,
                        Integer.toString(cursor.getInt(0))
                });
            }
        }
        cursor.close();
    }

    public void deleteVideos() {
        getWritableDatabase().delete("videos", "ifnull(title,'')=''", null);
    }

    public List<Video> queryVideos(String search, int sortBy, int videoType, int limit, int offset) {
        //getWritableDatabase().execSQL("ALTER TABLE videos ADD views int;");
        Cursor cursor;
        if (sortBy == 0) {
            if (search == null) {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? ORDER by create_at DESC LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType),
                        Integer.toString(limit), Integer.toString(offset)});
            } else {
                // video_type = ? and  Integer.toString(videoType),
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? and title like ? ORDER by create_at DESC LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), "%" + search + "%",
                        Integer.toString(limit), Integer.toString(offset)});
            }
        } else if (sortBy == 1) {
            if (search == null) {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? ORDER by create_at LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType),  Integer.toString(limit), Integer.toString(offset)});
            } else {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? and title like ? ORDER by create_at  LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), "%" + search + "%", Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            }
        } else if (sortBy == 2) {
            if (search == null) {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? ORDER by create_at LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType),Integer.toString(limit), Integer.toString(offset)});
            } else {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? and title like ? ORDER by create_at  LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), "%" + search + "%", Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            }
        } else if (sortBy == 3) {
            if (search == null) {
                cursor = getReadableDatabase().rawQuery("select * from videos where views >= 1 and video_type = ? ORDER by  update_at DESC LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType),Integer.toString(limit), Integer.toString(offset)});
            } else {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? and title like ? ORDER by update_at DESC LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), "%" + search + "%", Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            }
        } else if (sortBy == 5) {
            if (search == null) {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ?  ORDER by views DESC LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            } else {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? and title like ? ORDER by views DESC ", new String[]{Integer.toString(videoType), "%" + search + "%", Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            }
        } else {
            if (search == null) {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ?  ORDER by views LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            } else {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? and title like ? ORDER by views ", new String[]{Integer.toString(videoType), "%" + search + "%", Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            }
        }
        List<Video> videos = new ArrayList<>();
        while (cursor.moveToNext()) {
            Video video = new Video();
            video.Id = cursor.getInt(0);
            video.Title = cursor.getString(1);
            //video.Url = cursor.getString(2);
            video.Thumbnail = cursor.getString(3);
//            video.Source = cursor.getString(4);
//            video.Width = cursor.getInt(5);
//            video.Height = cursor.getInt(6);
            video.Duration = cursor.getInt(7);
//            video.Hidden = cursor.getInt(8);
//            video.VideoType = cursor.getInt(9);
            video.CreateAt = cursor.getLong(10);
            //video.UpdateAt = cursor.getLong(11);
            video.Views = cursor.getInt(12);
            videos.add(video);
        }
        return videos;
    }

    public Video queryVideoSource(int id) {
        Cursor cursor = getReadableDatabase().rawQuery("select id, title,source,url from videos where id = ? limit 1", new String[]{Integer.toString(id)});
        Video video = new Video();
        if (cursor.moveToNext()) {
            video.Id = cursor.getInt(0);
            video.Title = cursor.getString(1);
            video.Source = cursor.getString(2);
            video.Url = cursor.getString(3);
        }
        return video;
    }

    public void updateVideoSource(int id, String source) {
        getWritableDatabase().execSQL("update videos set source = ?,update_at = ? where id = ?", new String[]{
                source,
                Long.toString(System.currentTimeMillis()),
                Integer.toString(id)
        });
    }

    public void updateVideoInformation(String url, int duration, int width, int height) {
        getWritableDatabase().execSQL("update videos set duration = ?,width = ?,height = ? where source = ?", new String[]{
                Integer.toString(duration),
                Integer.toString(width),
                Integer.toString(height),
                url
        });
    }

    public void updateViews(int id) {
        getWritableDatabase().execSQL("update videos set views = coalesce(views, 0)+1,update_at = ? where id = ?", new String[]{
                Long.toString(System.currentTimeMillis()),
                Integer.toString(id)
        });
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public static class Video {
        public int Id;
        public String Title;
        public String Url;
        public String Thumbnail;
        public String Source;
        public int Width;
        public int Height;
        public int Duration;
        public int Hidden;
        public int VideoType;
        public long CreateAt;
        public long UpdateAt;
        public int Views;

        @Override
        public String toString() {
            return "Video{" +
                    "Id=" + Id +
                    ", Title='" + Title + '\'' +
                    ", Url='" + Url + '\'' +
                    ", Thumbnail='" + Thumbnail + '\'' +
                    ", Source='" + Source + '\'' +
                    ", Width=" + Width +
                    ", Height=" + Height +
                    ", Duration=" + Duration +
                    ", Hidden=" + Hidden +
                    ", VideoType=" + VideoType +
                    ", CreateAt=" + CreateAt +
                    ", UpdateAt=" + UpdateAt +
                    ", Views=" + Views +
                    '}';
        }
    }
}