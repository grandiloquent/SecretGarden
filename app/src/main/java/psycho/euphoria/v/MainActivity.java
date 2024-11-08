package psycho.euphoria.v;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Process;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import psycho.euphoria.v.VideoDatabase.Video;
import psycho.euphoria.v.tasks.DownloaderService;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
import static android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM;
import static psycho.euphoria.v.Shared.closeQuietly;
import static psycho.euphoria.v.Shared.requestStoragePremissions;

public class MainActivity extends Activity {

    public static final String KEY_SORT = "sort";
    private static final String KEY_VIDEO_TYPE = "video_type";
    private static int mVideoType = 2;
    private BottomSheetLayout mRoot;
    private VideoDatabase mVideoDatabase;
    private GridView mGridView;
    private VideosAdapter mVideosAdapter;
    private String mSearch;
    private int mSort = 0;

    public void browserEmbededWeb(String uri) {
        Intent intent = new Intent(MainActivity.this, TestActivity.class);
        intent.putExtra("url", uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity.this.startActivity(intent);
    }

    public void browserOnUiThread(String uri) {
        browserEmbededWeb(uri);
    }

    public boolean onQueryTextSubmit(String query) {
        // https://v.douyin.com/8kSH3tK
        if (Utils.getDouYinVideo(this, query)) {
            return true;
        }
        if (Utils.getKuaiShouVideo(this, query)) {
            return true;
        }
        if (Extractor.checkCableAv(query)) {
            new Thread(() -> {
                try {
                    String cookie = SettingsFragment.getString(this, SettingsFragment.KEY_CABLE_AV_COOKIE, null);
                    if (cookie == null) {
                        browserOnUiThread(Extractor.CABEL_TV_HOME_PAGE);
                        return;
                    }
                    Video video = Extractor.CableAv(query, SettingsFragment.getString(this, SettingsFragment.KEY_USER_AGENT, null),
                            cookie);
                    if (video == null) {
                        browserOnUiThread(Extractor.CABEL_TV_HOME_PAGE);
                        return;
                    }
                    List<Video> videos = new ArrayList<>();
                    videos.add(video);
                    mVideoDatabase.insertVideos(videos);
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "完成", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }).start();
            return true;
        }
        int index = mVideosAdapter.search(query);
        if (index != -1) {
            mGridView.post(new Runnable() {
                @Override
                public void run() {
                    mGridView.setSelection(index);
                    mGridView.smoothScrollToPosition(index);
                }
            });
        }
        return true;
    }

    public void showProgress(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    public static void start(Context context, String videoAddress) {
        Intent starter = new Intent(context, DownloaderService.class);
        starter.putExtra(DownloaderService.EXTRA_VIDEO_ADDRESS, videoAddress);
        context.startService(starter);
    }

    private void askUpdate(VersionInfo versionInfo) {
        AlertDialog dialog = new Builder(this)
                .setTitle("询问")
                .setMessage("程序有新版本是否更新？")
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    performUpdate(versionInfo);
                }).setNegativeButton(android.R.string.cancel, (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                })
                .create();
        dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void checkUpdate() {
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            VersionInfo versionInfo = Utils.getVersionInformation();
            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                int version = pInfo.versionCode;
                if (versionInfo.versionCode > version) {
                    runOnUiThread(() -> askUpdate(versionInfo));
                }
            } catch (Exception e) {
            }
        }).start();

    }

    private void chooseVideoType() {
        new Builder(this)
                .setItems(new String[]{
                        "91",
                        "57",
                        "收藏",
                        "屏蔽",
                        "露脸",
                        "其他",
                        "视频"

                }, (dialog1, which) -> {
                    switch (which) {
                        case 0:
                            mVideoType = 1;
                            break;
                        case 1:
                            mVideoType = 2;
                            break;
                        case 2:
                            mVideoType = 3;
                            break;
                        case 3:
                            mVideoType = 4;
                            break;
                        case 4:
                            mVideoType = 5;
                            break;
                        case 5:
                            mVideoType = 6;
                            break;
                        case 6:
                            mVideoType = 9;
                            break;
                    }
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit()
                            .putInt(KEY_VIDEO_TYPE, mVideoType).apply();
                    mVideosAdapter.update(mVideoDatabase.queryVideos(mSearch, mSort, mVideoType));
                })
                .show();
    }

    private void fetch91Videos() {
        EditText editText = new EditText(this);
        AlertDialog dialog = new Builder(this)
                .setView(editText)
                .setPositiveButton("确定", (dialog1, which) -> {
                    dialog1.dismiss();
                    new Thread(() -> {
                        int start = 0;
                        int end = 3;
                        Pattern pattern = Pattern.compile("\\d+");
                        Matcher matcher = pattern.matcher(editText.getText());
                        while (matcher.find()) {
                            if (start == 0)
                                start = Integer.parseInt(matcher.group());
                            else
                                end = Integer.parseInt(matcher.group());
                        }
                        for (int i = start; i < end; i++) {
                            try {
                                List<Video> videos = Utils.scrap91Porn(i);
                                mVideoDatabase.insertVideos(videos);
                                showProgress(String.format("已成功抓取第 %s 页", i + 1));
                            } catch (Exception e) {
                                showProgress(String.format("抓取页面错误：%s", e.getMessage()));
                            }
                        }

                    }).start();
                }).create();
        dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void loadVideos() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSort = preferences.getInt(KEY_SORT, 0);
        mVideoType = preferences.getInt(KEY_VIDEO_TYPE, 1);
        mVideosAdapter.update(mVideoDatabase.queryVideos(mSearch, mSort, mVideoType));
    }

    private void performUpdate(VersionInfo versionInfo) {
        File f = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "HuaYuan.apk");
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("下载中...");
        dialog.show();
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            HttpsURLConnection c;
            try {
                c = (HttpsURLConnection) new URL(versionInfo.downloadLink).openConnection();
                FileOutputStream fos = new FileOutputStream(
                        f);
                Shared.copy(c.getInputStream(), fos);
                closeQuietly(fos);
            } catch (IOException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    Shared.installPackage(MainActivity.this, f);
                }
            });
        }).start();
    }

    private void sortVideos() {
        new Builder(this)
                .setItems(new String[]{
                        "发布时间最晚",
                        "发布时间最早",
                        "更新时间最晚",
                        "播放次数最多",

                }, (dialog1, which) -> {
                    switch (which) {
                        case 0:
                            mSort = 0;
                            break;
                        case 1:
                            mSort = 1;
                            break;
                        case 2:
                            mSort = 3;
                            break;
                        case 3:
                            mSort = 5;
                            break;

                    }
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit()
                            .putInt(KEY_SORT, mSort).apply();
                    mVideosAdapter.update(mVideoDatabase.queryVideos(mSearch, mSort, mVideoType));
                })
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Intent videoService = new Intent(this, VideoService.class);
//        startService(videoService);
        requestStoragePremissions(this, false);
        setContentView(R.layout.main_activity);
        mVideoDatabase = new VideoDatabase(this,
                new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "videos.db").getAbsolutePath());
        mGridView = findViewById(R.id.recycler_view);
        registerForContextMenu(mGridView);
        mGridView.setNumColumns(2);
        mVideosAdapter = new VideosAdapter();
        mGridView.setAdapter(mVideosAdapter);
        loadVideos();
        mRoot = findViewById(R.id.root);
        if (SettingsFragment.getString(this, SettingsFragment.KEY_USER_AGENT, null) == null) {
            String ua = new WebView(this).getSettings().getUserAgentString();
            SettingsFragment.setString(this, SettingsFragment.KEY_USER_AGENT, ua);
        }
        startService(new Intent(this, DownloaderService.class));
        // checkUpdate();
        // if (VERSION.SDK_INT >= VERSION_CODES.O) {
        // try {
        // PlayerActivity.launchActivity(this,
        // Files.list(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toPath())
        // .findFirst().get().toFile(),1
        // );
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // }
        mGridView.setOnItemClickListener((parent, view, position, id) -> new Thread(() -> {
            Video video = mVideosAdapter.getItem(position);
            String source = null;
            Video old = mVideoDatabase.queryVideoSource(video.Id);
            if (TextUtils.isEmpty(old.Source)) {
                Pair<String, String> videos = null;
                if (video.Url.startsWith("/")) {
                    videos = WebActivity.processCk(view.getContext(), Utils.getRealAddress() + video.Url);
                } else {
                    videos = WebActivity.process91Porn(view.getContext(), video.Url);
                    ;
                }
                if (videos != null) {
                    source = videos.second;
                }
                if (!TextUtils.isEmpty(source)) {
                    mVideoDatabase.updateVideoSource(video.Id, source);
                }
            } else {
                source = old.Source;
            }
            if (!TextUtils.isEmpty(source)) {
                mVideoDatabase.updateViews(video.Id);
                PlayerActivity.launchActivity(view.getContext(), source, video.Title);
            } else {
                Helpers.tryGetCookie(this, video);
            }
        }).start());
//        new Thread(() -> {
//            mVideoDatabase.updateThumbnails();
//            MainActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(MainActivity.this, "完成", Toast.LENGTH_SHORT).show();
//                }
//            });
//        }).start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo contextMenuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        Video video = mVideosAdapter.getItem(contextMenuInfo.position);
        if (item.getItemId() == 0) {
            new Thread(() -> {
                if (video.Source == null) {
                    // Attempt to resolve the address of the video file
                    Pair<String, String> videos = WebActivity.process91Porn(this, video.Url);
                    if (videos != null && videos.second != null) {
                        mVideoDatabase.updateVideoSource(video.Id, videos.second);
                        video.Source = videos.second;
                    } else {
                        return;
                    }
                }
                Intent starter = new Intent(this, DownloaderService.class);
                starter.putExtra(DownloaderService.EXTRA_VIDEO_TITLE, video.Title);
                starter.putExtra(DownloaderService.EXTRA_VIDEO_ADDRESS, video.Source);
                startService(starter);
            }).start();
        } else if (item.getItemId() == 1) {
            mVideoDatabase.updateVideoType(video.Id, 3);
            mVideosAdapter.update(mVideoDatabase.queryVideos(mSearch, mSort, mVideoType));
        } else if (item.getItemId() == 2) {
            mVideoDatabase.updateVideoType(video.Id, 4);
            mVideosAdapter.update(mVideoDatabase.queryVideos(mSearch, mSort, mVideoType));
        } else if (item.getItemId() == 3) {
            mVideoDatabase.updateVideoType(video.Id, 5);
            mVideosAdapter.update(mVideoDatabase.queryVideos(mSearch, mSort, mVideoType));
        } else if (item.getItemId() == 4) {
            //mVideoDatabase.updateVideoType(video.Id, 6);
            //mVideosAdapter.update(mVideoDatabase.queryVideos(mSearch, mSort, mVideoType));
            ClipboardManager manager = getSystemService(ClipboardManager.class);
            manager.setPrimaryClip(ClipData.newPlainText(null, video.Url));
        } else if (item.getItemId() == 5) {
            new Thread(() -> {
                Helpers.updateSource(this, mVideoDatabase, video);
                runOnUiThread(() -> {
                    Toast.makeText(this, "成功", Toast.LENGTH_SHORT).show();
                });
            }).start();
        }
        return super.onContextItemSelected(item);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0, 0, 0, "下载");
        menu.add(0, 1, 0, "收藏");
        menu.add(0, 2, 0, "屏蔽");
        menu.add(0, 3, 0, "露脸");
        menu.add(0, 4, 0, "其他");
        menu.add(0, 5, 0, "刷新");
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_menu).setShowAsAction(SHOW_AS_ACTION_ALWAYS);
        menu.findItem(R.id.action_refresh).setShowAsAction(SHOW_AS_ACTION_IF_ROOM);
        MenuItem menuItem = menu.add(0, 0, 0, "搜索");
        Drawable drawable = menuItem.getIcon();
        if (drawable != null) {
            // If we don't mutate the drawable, then all drawable's with this id will have a
            // color
            // filter applied to it.
            drawable.mutate();
            drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
            drawable.setAlpha(255);
        }
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        SearchView searchView = new SearchView(this);
        searchView.setIconified(true);
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearch = query;
                mVideosAdapter.update(mVideoDatabase.queryVideos(mSearch, mSort, mVideoType));
                searchView.clearFocus();
                // searchView.setIconified(true);
                return true;
            }
        });
        Utils.setSearchViewStyle(searchView);
        menuItem.setActionView(searchView);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_menu) {
            GridView gridView = (GridView) LayoutInflater.from(this).inflate(R.layout.modal_bottom_sheet_content, null);
            gridView.setNumColumns(3);
            List<BottomSheetItem> bottomSheetItems = new ArrayList<>();
            int[][] items = new int[][]{
                    new int[]{R.drawable.ic_action_search, R.string.search},
                    new int[]{R.drawable.ic_action_playlist_play, R.string.video},
                    new int[]{R.drawable.ic_action_settings, R.string.set_up},
                    new int[]{R.drawable.ic_action_sort_by_alpha, R.string.sort},
                    new int[]{R.drawable.ic_action_sort_by_alpha, R.string.video_type},
                    new int[]{R.drawable.ic_action_refresh_dark, R.string.porn91},
                    new int[]{R.drawable.ic_action_refresh_dark, R.string.ck52}
            };
            for (int[] ints : items) {
                BottomSheetItem bottomSheetItem = new BottomSheetItem();
                bottomSheetItem.title = getString(ints[1]);
                bottomSheetItem.icon = ints[0];
                bottomSheetItems.add(bottomSheetItem);
            }
            BottomSheetItemAdapter ba = new BottomSheetItemAdapter(this, bottomSheetItems);
            gridView.setAdapter(ba);
            gridView.setOnItemClickListener((parent, view, position, id) -> {
                if (position == 0) {
                    Shared.openTextContentDialog(MainActivity.this,
                            getString(R.string.search),
                            this::onQueryTextSubmit);
                }
                if (position == 1) {
                    Utils.startVideoList(MainActivity.this);
                } else if (position == 2) {
                    Intent starter = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(starter);
                } else if (position == 3) {
                    sortVideos();
                } else if (position == 4) {
                    chooseVideoType();
                } else if (position == 5) {
                    fetch91Videos();
                } else if (position == 6) {
                    new Thread(() -> {
                        // 500
                        for (int i = 0; i <100; i++) {
                            try {
                                List<Video> videos = Utils.scrap52Ck(i);
                                mVideoDatabase.insertVideos(videos);
                                showProgress(String.format("已成功抓取第 %s 页", i + 1));
                            } catch (Exception e) {
                                showProgress(String.format("抓取页面错误：%s", e.getMessage()));
                            }
                        }
                    }).start();
                }
                mRoot.dismissSheet();
            });
            mRoot.showWithSheetView(gridView);
        } else if (item.getItemId() == R.id.action_refresh) {
//            mVideoType = 1;
//            mVideosAdapter.update(mVideoDatabase.queryVideos(mSearch, mSort, mVideoType));
//            if (mSort == 0) {
//                mSort = 3;
//            } else if (mSort == 3) {
//                mSort = 5;
//            } else if (mSort == 5) {
//                mSort = 0;
//            }
            Process.killProcess(Process.myPid());
        }
        return super.onOptionsItemSelected(item);
    }

    private class VideosAdapter extends BaseAdapter {
        private List<Video> mVideos = new ArrayList<>();
        private LruCache<String, BitmapDrawable> mLruCache = new LruCache<>(1000);
        private ExecutorService mExecutorService = Executors.newFixedThreadPool(3);
        private Handler mHandler = new Handler();

        public int search(String pattern) {
            int i = 0;
            for (Video video : mVideos) {
                if (video.Title.contains(pattern)) {
                    return i;
                }
                i++;
            }
            return -1;
        }

        public void update(List<Video> videos) {
            mVideos.clear();
            mVideos.addAll(videos);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mVideos.size();
        }

        @Override
        public Video getItem(int position) {
            return mVideos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.video_item, null);
                viewHolder = new ViewHolder();
                viewHolder.thumbnail = convertView.findViewById(R.id.thumbnail);
                viewHolder.title = convertView.findViewById(R.id.title);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.title.setText(mVideos.get(position).Title);
            viewHolder.thumbnail.setTag(mVideos.get(position).Thumbnail);
            viewHolder.thumbnail.setBackground(null);
            mExecutorService.submit(new Loader(MainActivity.this, viewHolder, mLruCache, mHandler));
            return convertView;
        }
    }

    public class Loader implements Runnable {
        private final Handler mHandler;
        private ViewHolder mViewHolder;
        private String mPath;
        private int mSize;
        private File mDirectory;
        private LruCache<String, BitmapDrawable> mLruCache;

        public Loader(Context context, ViewHolder viewHolder, LruCache<String, BitmapDrawable> lruCache,
                      Handler handler) {
            mViewHolder = viewHolder;
            mPath = viewHolder.thumbnail.getTag().toString();
            mSize = context.getResources().getDisplayMetrics().widthPixels / 2;
            mDirectory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            mLruCache = lruCache;
            mHandler = handler;
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            if (mLruCache.get(mPath) != null) {
                mHandler.post(() -> mViewHolder.thumbnail.setBackground(mLruCache.get(mPath)));
                return;
            }
            Bitmap bitmap = null;
            File image = new File(mDirectory, Shared.md5(mPath));
            if (image.exists()) {
                bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
            }
            if (bitmap == null) {
                Bitmap source = null;
                try {
                    source = BitmapFactory.decodeStream(new URL(mPath).openConnection().getInputStream());
                } catch (Exception e) {
                }
                if (source == null)
                    return;
                bitmap = Shared.resizeAndCropCenter(source, mSize, true);
                try {
                    FileOutputStream fos = new FileOutputStream(image);
                    bitmap.compress(CompressFormat.JPEG, 80, fos);
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (mViewHolder.thumbnail.getTag().toString().equals(mPath)) {
                BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
                mLruCache.put(mPath, bitmapDrawable);
                mHandler.post(() -> mViewHolder.thumbnail.setBackground(bitmapDrawable));
            }

        }
    }

    private class ViewHolder {
        ImageView thumbnail;
        TextView title;
    }
}
// adb connect 192.168.8.55:5000