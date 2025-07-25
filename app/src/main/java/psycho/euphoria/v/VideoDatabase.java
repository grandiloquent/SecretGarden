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

    public void deleteVideos() {
        getWritableDatabase().delete("videos", "ifnull(title,'')=''", null);
    }

    public void insertVideos(List<Video> videos) {
        SQLiteDatabase db = getWritableDatabase();
        SQLiteDatabase reader = getReadableDatabase();
        db.beginTransaction();
        try {
            for (Video video : videos) {
                Cursor c = reader.rawQuery("select id from videos where url = ?", new String[]{video.Url});
                if (c.moveToNext()) {
                    db.execSQL("update videos set title = ?,thumbnail=?, create_at = ?,update_at = ? where url = ?", new String[]{
                            video.Title,
                            video.Thumbnail,
                            Long.toString(video.CreateAt),
                            Long.toString(System.currentTimeMillis()),
                            video.Url});
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

    public List<Video> queryVideos(String search, int sortBy, int videoType, int limit, int offset) {
        //getWritableDatabase().execSQL("ALTER TABLE videos ADD views int;");
        // Log.e("B5aOx2", String.format("queryVideos, search=%s,\nsort=%s,\nvideoType=%s,\nlimit=%s,\noffset=%s", search, sortBy, videoType, limit, offset));
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
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? ORDER by create_at LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            } else {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? and title like ? ORDER by create_at  LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), "%" + search + "%", Integer.toString(limit), Integer.toString(offset)});
            }
        } else if (sortBy == 2) {
            if (search == null) {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? ORDER by create_at LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            } else {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? and title like ? ORDER by create_at  LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), "%" + search + "%", Integer.toString(limit), Integer.toString(offset)});
            }
        } else if (sortBy == 3) {
            if (search == null) {
                // views >= 1 and
                cursor = getReadableDatabase().rawQuery("select * from (select * from videos where video_type = ? ORDER by update_at DESC) AS A ORDER by update_at DESC LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            } else {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? and title like ? ORDER by update_at DESC LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), "%" + search + "%", Integer.toString(limit), Integer.toString(offset)});
            }
        } else if (sortBy == 5) {
            if (search == null) {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ?  ORDER by views DESC LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            } else {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? and title like ? ORDER by views DESC LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), "%" + search + "%", Integer.toString(limit), Integer.toString(offset)});
            }
        } else if (sortBy == 6) {
            if (search == null) {
                // views >= 1 and
                cursor = getReadableDatabase().rawQuery("select * from (select * from videos where views >= 1 and video_type = ? ORDER by update_at DESC) AS A ORDER by update_at DESC LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            } else {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? and title like ? ORDER by update_at DESC LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), "%" + search + "%", Integer.toString(limit), Integer.toString(offset)});
            }
        } else if (sortBy == 7) {
            if (search == null) {
                // views >= 1 and create_at DESC,
                cursor = getReadableDatabase().rawQuery("select * from (select * from videos where video_type = ? ORDER by display ASC,create_at DESC) AS A ORDER by display ASC,create_at DESC LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            } else {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? and title like ? ORDER by create_at DESC,display LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), "%" + search + "%", Integer.toString(limit), Integer.toString(offset)});
            }
        } else {
            if (search == null) {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ?  ORDER by views LIMIT ? OFFSET ?", new String[]{Integer.toString(videoType), Integer.toString(limit), Integer.toString(offset)});
            } else {
                cursor = getReadableDatabase().rawQuery("select * from videos where video_type = ? and title like ? ORDER by views ", new String[]{Integer.toString(videoType), "%" + search + "%", Integer.toString(limit), Integer.toString(offset)});
            }
        }
        List<Video> videos = new ArrayList<>();
        while (cursor.moveToNext()) {
            Video video = new Video();
            video.Id = cursor.getInt(0);
            video.Title = cursor.getString(1);
            //video.Url = cursor.getString(2);
            video.Thumbnail = cursor.getString(3);
            video.Source = cursor.getString(4);
//            video.Width = cursor.getInt(5);
//            video.Height = cursor.getInt(6);
            video.Duration = cursor.getInt(7);
//            video.Hidden = cursor.getInt(8);
            video.VideoType = cursor.getInt(9);
            video.CreateAt = cursor.getLong(10);
            //video.UpdateAt = cursor.getLong(11);
            video.Views = cursor.getInt(12);
            videos.add(video);
        }
        cursor.close();
        return videos;
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

    public void updateVideoInformation(String url, int duration, int width, int height) {
        getWritableDatabase().execSQL("update videos set duration = ?,width = ?,height = ? where source = ?", new String[]{
                Integer.toString(duration),
                Integer.toString(width),
                Integer.toString(height),
                url
        });
    }

    public void updateVideoSource(int id, String[] source) {
        getWritableDatabase().execSQL("update videos set title =coalesce(?,title), source = ?, thumbnail = coalesce(?, thumbnail),update_at = ? where id = ?", new String[]{
                source[0], source[1], source[2],
                Long.toString(System.currentTimeMillis()),
                Integer.toString(id)
        });
    }

    public void updateVideoType(int id, int i) {
        getWritableDatabase().execSQL("update videos set video_type = ?,update_at = ? where id = ?", new String[]{
                Integer.toString(i),
                Long.toString(System.currentTimeMillis()),
                Integer.toString(id)
        });
    }

    public void updateViews(int id) {
        getWritableDatabase().execSQL("update videos set views = coalesce(views, 0)+1,update_at = ? where id = ?", new String[]{
                Long.toString(System.currentTimeMillis()),
                Integer.toString(id)
        });
    }

    public void updateDisplay(int id) {
        //getWritableDatabase().execSQL("ALTER TABLE videos ADD COLUMN display INTEGER;");
        getWritableDatabase().execSQL("update videos set display = coalesce(display, 0)+1,update_at = ? where id = ?", new String[]{
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
                "            \"display\"            INTEGER,\n" +
                "            \"views\"            INTEGER);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void checkTables() {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='image_uri'", null);
        if (!cursor.moveToNext()) {
            getWritableDatabase().execSQL("CREATE TABLE image_uri(id INTEGER PRIMARY KEY,uri TEXT)");
        }
        cursor.close();
    }

    public String queryImageUri() {
        Cursor cursor = getReadableDatabase()
                .rawQuery("select uri from image_uri where id = ?", new String[]{"1"});
        String uri = null;
        if (cursor.moveToNext()) {
            uri = cursor.getString(0);
        }
        cursor.close();
        return uri;
    }

    public long updateImageUri(String uri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("uri", uri);
        // The key here is SQLiteDatabase.insertWithOnConflict()
        long result = db.insertWithOnConflict("image_uri", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return result; // Returns the row ID of the inserted/updated row
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