package psycho.euphoria.v;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.GridView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import psycho.euphoria.v.Shared.Listener;

import static psycho.euphoria.v.Shared.requestStoragePremissions;

public class VideoListActivity extends Activity {
    private static final String KEY_FAVORITES_LIST = "key_favorites_list";
    private GridView mGridView;
    private VideoItemAdapter mVideoItemAdapter;
    private String mDirectory;
    private int mSort = 2;
    private String mFilter;
    public static final String KEY_SORT = "sort";

    private void addBookmark() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> strings = preferences.getStringSet(KEY_FAVORITES_LIST, new HashSet<>());
        Set<String> newStrings = new HashSet<>(strings);
        newStrings.add(mDirectory);
        preferences.edit().putStringSet(KEY_FAVORITES_LIST, newStrings).apply();
    }

    private void createFileDirectory() {
        Shared.openTextContentDialog(this, getString(R.string.create_a_directory), new Listener() {
            @Override
            public void onSuccess(String value) {
                File dir = new File(mDirectory, value.trim());
                if (!dir.isDirectory())
                    dir.mkdir();
            }
        });
    }

    private void deleteBookmark() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> strings = preferences.getStringSet(KEY_FAVORITES_LIST, new HashSet<>());
        Set<String> newStrings = new HashSet<>(strings);
        newStrings.remove(mDirectory);
        preferences.edit().putStringSet(KEY_FAVORITES_LIST, newStrings).apply();
    }

    private String getDefaultPath() {
        return getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    }

    private void initialize() {
        mDirectory = SettingsFragment.getString(this, SettingsFragment.KEY_VIDEO_FOLDER, getDefaultPath());
        loadFolder(mFilter, mSort);
    }

    private void loadDirectory() {
        SettingsFragment.setString(this, SettingsFragment.KEY_VIDEO_FOLDER, mDirectory);
        loadFolder(mFilter, mSort);
    }

    private void loadFolder(String filter, int sort) {
        File dir = new File(mDirectory);
        File[] videos = dir.listFiles(pathname -> pathname.isFile() && (pathname.getName().endsWith(".mp4")
                || pathname.getName().endsWith(".MP4")
                || pathname.getName().endsWith(".MOV")
                || pathname.getName().endsWith(".mov")
        )
                && (TextUtils.isEmpty(filter) || pathname.getName().contains(filter)));
        if (videos != null) {
            for (File f : videos) {
                f.renameTo(new File(f.getParentFile(), Shared.substringBefore(f.getName(), ".mp4") + ".v"));
            }
        }
        videos = dir.listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".v") && (TextUtils.isEmpty(filter) || pathname.getName().contains(filter)));
        if (videos == null) {
            return;
        }
        int direction = (sort & 1) == 0 ? -1 : 1;
        Arrays.sort(videos, (o1, o2) -> {
            if ((sort & 2) == 2) {
                final long result = o2.lastModified() - o1.lastModified();
                if (result < 0) {
                    return -1 * direction;
                }
                if (result > 0) {
                    return 1 * direction;
                }
            }
            if ((sort & 4) == 4) {
                final long result = o2.length() - o1.length();
                if (result < 0) {
                    return -1 * direction;
                }
                if (result > 0) {
                    return 1 * direction;
                }
            }
            return 0;
        });
        List<VideoItem> videoItems = new ArrayList<>();
        for (File video : videos) {
            VideoItem videoItem = new VideoItem();
            videoItem.path = video.getAbsolutePath();
            videoItems.add(videoItem);
        }
        mVideoItemAdapter.updateVideos(videoItems);
    }

    private void showBookmarks() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String[] strings = preferences.getStringSet(KEY_FAVORITES_LIST, new HashSet<>()).toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(strings, (dialog, which) -> {
            mDirectory = strings[which];
            loadDirectory();
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        initialize();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSort = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(KEY_SORT, 3);
        requestStoragePremissions(this, true);
        setContentView(R.layout.video_list_activity);
        mGridView = findViewById(R.id.recycler_view);
        mGridView.setNumColumns(2);
        registerForContextMenu(mGridView);
        mVideoItemAdapter = new VideoItemAdapter(this);
        mGridView.setAdapter(mVideoItemAdapter);
        mGridView.setOnItemClickListener((parent, view, position, id) -> VideoActivity.launchActivity(view.getContext(), new File(
                mVideoItemAdapter.getItem(position).path
        ), mSort));
        getActionBar().setDisplayHomeAsUpEnabled(true);
        initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDirectory != null)
            loadFolder(mFilter, mSort);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo contextMenuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        VideoItem videoItem = mVideoItemAdapter.getItem(contextMenuInfo.position);
        if (item.getItemId() == 0) {
            File file = new File(videoItem.path);
            Shared.shareFile(this, file, getString(R.string.send_video));
        } else {
            File dir = new File(mDirectory, item.getTitle().toString());
            File f = new File(videoItem.path);
            f.renameTo(new File(dir, f.getName()));
            loadFolder(mFilter, mSort);
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0, 0, 0, R.string.share);
        File[] directories = new File(mDirectory).listFiles(File::isDirectory);
        if (directories != null) {
            for (int i = 0; i < directories.length; i++) {
                menu.add(0, i + 1, 0, directories[i].getName());
            }
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.video_list, menu);
        menu.findItem(R.id.action_selector).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.findItem(R.id.action_bookmark).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mFilter = query;
                loadFolder(mFilter, mSort);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_selector) {
            Intent starter = new Intent(this, FileListActivity.class);
            startActivityForResult(starter, 0);
        } else if (item.getItemId() == R.id.action_fav) {
            addBookmark();
        } else if (item.getItemId() == R.id.action_favorite_border) {
            deleteBookmark();
        } else if (item.getItemId() == R.id.action_bookmark) {
            showBookmarks();
        } else if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.action_video) {
            mDirectory = getDefaultPath();
            loadDirectory();
        } else if (item.getItemId() == R.id.action_create_directory) {
            createFileDirectory();
        }
        if (item.getItemId() == R.id.action_sort_by_create_time_ascending) {
            actionSortByCreateTimeAscending();
        }
        if (item.getItemId() == R.id.action_sort_by_create_time_descending) {
            actionSortByCreateTimeDescending();
        }
        if (item.getItemId() == R.id.action_sort_by_size_ascending) {
            actionSortBySizeAscending();
        }
        if (item.getItemId() == R.id.action_sort_by_size_descending) {
            actionSortBySizeDescending();
        }
        return super.onOptionsItemSelected(item);
    }

    private void sort() {
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .edit()
                .putInt(KEY_SORT, mSort)
                .apply();
        loadFolder(mFilter, mSort);
    }

    private void actionSortByCreateTimeAscending() {
        mSort = 2;
        sort();
    }


    private void actionSortByCreateTimeDescending() {
        mSort = 3;
        sort();
    }


    private void actionSortBySizeAscending() {
        mSort = 4;
        sort();
    }


    private void actionSortBySizeDescending() {
        mSort = 5;
        sort();
    }


}
