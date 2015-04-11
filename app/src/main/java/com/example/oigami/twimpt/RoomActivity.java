package com.example.oigami.twimpt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.oigami.twimpt.debug.Logger;
import com.example.oigami.twimpt.file.FileDownloadThread;
import com.example.oigami.twimpt.file.FileDownloader;
import com.example.oigami.twimpt.image.database.ImageCacheDB;
import com.example.oigami.twimpt.image.database.TwimptImageTable;
import com.example.oigami.twimpt.image.database.TwimptIconTable;
import com.example.oigami.twimpt.twimpt.ParsedData;
import com.example.oigami.twimpt.twimpt.ParsedRoomData;
import com.example.oigami.twimpt.twimpt.ParsedUserData;
import com.example.oigami.twimpt.twimpt.TwimptLogData;
import com.example.oigami.twimpt.twimpt.TwimptNetwork;
import com.example.oigami.twimpt.twimpt.room.TwimptRoom;
import com.example.oigami.twimpt.twimpt.token.AccessTokenData;
import com.example.oigami.twimpt.util.Network;
import com.example.oigami.twimpt.util.TimeDiff;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoomActivity extends ActionBarActivity {
  private enum CONTEXT_MENU {
    CANCEL,
    COMMENT,
    OPEN_ROOM,
    OPEN_USER_ROOM,
    OPEN_ID_ROOM,
    WRITE_ROOM,
  }

  public static final String INTENT_ROOM_NAME_HASH = "NOW_ROOM_HASH";
  public static final String INTENT_NAME_TYPE = "DATA_TYPE";

  private SwipeRefreshLayout mSwipeRefreshWidget;
  private ListView mListView;
  private boolean mNowUpdate = false;
  private DataApplication mGlobals;
  private String mNowHash;  //現在の部屋のハッシュ "room", "user", "public", etc..

  private TwimptListAdapter adapter;
  private Button listEndButton;
  private ImageCacheDB mImageCacheDB;
  /** コア数 */
  private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
  private ExecutorService exec;

  private TwimptImageAdapter.DrawableListener mImageListener;
  private TwimptListAdapter.DrawableListener mIconListener;

  //private String accessToken, accessTokenSecret;
  private static final class WhatHandler {
    static final int SUCCESS = 0;
    static final int ERROR = 1;
  }


  private Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
      case WhatHandler.ERROR:
        Toast.makeText(RoomActivity.this, (String) message.obj, Toast.LENGTH_LONG).show();
        break;
      case WhatHandler.SUCCESS:
        break;
      }
      RefreshEnd();
    }
  };

  private void ShowErrorMessage(String errMsg) {
    Message msg = Message.obtain(mHandler, WhatHandler.ERROR, errMsg);
    mHandler.sendMessage(msg);
  }

  void deleteDatabase() {
    for (String s : databaseList()) {
      deleteDatabase(s);
    }
    for (String s : fileList()) {
      deleteFile(s);
    }
  }

  /*アップデートしてデータ構造などが変わった時の差分処理 */
  private void VersionCheck() {
    SharedPreferences sharedPref = getSharedPreferences("version", MODE_PRIVATE);
    int version = sharedPref.getInt("code", 0);
    int nowVersion = BuildConfig.VERSION_CODE;
    if (nowVersion == version)
      return;
    //バージョンを管理してなかった時の番号
    if (version == 0) {
      deleteDatabase();
    } else if (version == 1) {
      deleteDatabase();
    } else if (version == 2) {
      deleteDatabase();
    }
    Editor e = sharedPref.edit();
    e.putInt("code", nowVersion);
    e.commit();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_my);
    exec = Executors.newFixedThreadPool(NUMBER_OF_CORES >= 1 ? NUMBER_OF_CORES : 1);
    //deleteDatabase();
    VersionCheck();
    mImageCacheDB = new ImageCacheDB(RoomActivity.this);
    //    deleteDatabase(uploadImageDB.getDbFileName());
    //    uploadImageDB = new ImageCacheDB(RoomActivity.this, "PostedImage.db");

    mGlobals = (DataApplication) this.getApplication();
    // ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します

    mSwipeRefreshWidget = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_widget);
    mSwipeRefreshWidget.setColorSchemeResources(
            R.color.blue_bright,
            R.color.green_light,
            R.color.orange_light,
            R.color.red_light);
    mSwipeRefreshWidget.setOnRefreshListener(new OnRefreshListener() {
      @Override
      public void onRefresh() {
        UpdateRequest();
      }
    });
    mListView = (ListView) findViewById(R.id.content);
    listEndButton = new Button(this);
    listEndButton.setText(R.string.load_log);
    listEndButton.setVisibility(View.GONE);
    listEndButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        LogRequest();
      }
    });
    mListView.addFooterView(listEndButton);
    mListView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        openContextMenu(view);

        //        if (i == adapter.GetReadHerePosition()) return;
        //        TwimptLogData data = adapter.getItem(i);
        //        AlertDialogFragment dlg = AlertDialogFragment.CreateSelectedDataDialog(data);
        //        dlg.show(getSupportFragmentManager(), "selected");
      }
    });
    mIconListener = new TwimptIconListener(RoomActivity.this, mImageCacheDB.getTwimptIconTable(), exec);
    mImageListener = new TwimptImageListener(RoomActivity.this, mImageCacheDB.getTwimptImageTable(), exec);

    Intent intent = getIntent();
    mNowHash = intent.getStringExtra(INTENT_ROOM_NAME_HASH);
    if (mNowHash == null) mNowHash = "public";

    String action = intent.getAction();
    if (!Intent.ACTION_VIEW.equals(action)) {
      TwimptRoom nowRoom = mGlobals.twimptRooms.get(mNowHash);
      getSupportActionBar().setTitle(nowRoom.name);
      createListAdapter(nowRoom);
      mListView.setAdapter(adapter);
      UpdateRequest();
      Logger.log("update");
    }
    //mSwipeRefreshWidget.setRefreshing(true);

    registerForContextMenu(mListView);
  }

  private void createListAdapter(TwimptRoom nowRoom) {
    if (adapter != null) {
      adapter.reset(this, nowRoom, mIconListener, mImageListener);
      return;
    }
    adapter = new TwimptListAdapter(RoomActivity.this, nowRoom,
                                    mIconListener, mImageListener);
    adapter.setOnImageItemClickListener(new AdapterView.OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TwimptImageAdapter imageAdapter = (TwimptImageAdapter) parent.getAdapter();
        //drawableをbitmapに変換する
        String url = imageAdapter.getItemUrl(position);
        String filename = mImageCacheDB.getTwimptImageTable().getFileName(url);
        Intent intent = new Intent(RoomActivity.this, ImageViewActivity.class);
        intent.putExtra(ImageViewActivity.INTENT_DRAWABLE_FILENAME, filename);
        startActivity(intent);
      }
    });
    mListView.setAdapter(adapter);
  }

  private void ChangeRoom(String newHash) {
    if (mNowHash.equals(newHash)) return;
    TwimptRoom newRoom = mGlobals.twimptRooms.get(newHash);
    createListAdapter(newRoom);
    getSupportActionBar().setTitle(newRoom.name);
    mNowHash = newHash;
  }

  @Override
  public void onStart() {
    super.onStart();
    //TextView textView = (TextView) findViewById(R.id.textview);
    //textView.setText(mGlobals.twimptRooms.get(mNowHash).name);
  }

  @Override
  public void onResume() {
    super.onResume();
    Intent intent = getIntent();
    String action = intent.getAction();
    if (!Intent.ACTION_VIEW.equals(action))
      return;
    //2回目は来ないようにする
    intent.setAction(Intent.ACTION_MAIN);
    Uri uri = intent.getData();
    if (uri == null)
      return;
    String path = uri.getPath();
    if (path == null) return;
    path = path.substring(1);
    if (path.equals("public") || path.equals("monologue")) {
      mNowHash = path;
      Map<String, TwimptRoom> twimptRoomMap = mGlobals.twimptRooms;
      adapter.reset(this, twimptRoomMap.get(mNowHash), mIconListener, mImageListener);
      adapter.notifyDataSetChanged();
      return;
    }

    final int ROOM = 0;
    final int USER = 1;
    final int LOG = 2;
    int type;
    String id;
    if (path.startsWith("room/")) {
      type = ROOM;
      id = path.substring(5);
    } else if (path.startsWith("user/")) {
      type = USER;
      id = path.substring(5);
    } else if (path.startsWith("log/")) {
      type = LOG;
      id = path.substring(4);
    } else {
      return;
    }

    Collection<TwimptRoom> twimptRooms = mGlobals.twimptRooms.values();
    for (TwimptRoom twimptRoom : twimptRooms) {
      if (id.equals(twimptRoom.id)) {
        ChangeRoom(twimptRoom.hash);
        UpdateRequest();
        return;
      }
    }
    /** 最新ログの取得スレッドを実行 */
    switch (type) {
    case ROOM:
      UpdateRequest(path, UpdateRoomRequestListener());
      return;
    case USER:
      UpdateRequest(path, UpdateUserRequestListener());
      return;
    case LOG:
    default:
      Logger.log(path);
      ShowErrorMessage(path + "error");
    }
  }

  interface RequestListener {
    public Runnable OnRequest(String path) throws IOException, JSONException;
  }

  private Runnable UpdateRunnable(final TwimptRoom nowRoom, final ParsedData parsedData,
                                  final ConcurrentHashMap<String, TwimptRoom> twimptRooms) {
    return new Runnable() {
      @Override
      public void run() {
        final int readHere = parsedData.mTwimptLogData.length;
        PushUpdateData(twimptRooms, nowRoom, parsedData);
        ChangeRoom(nowRoom.hash);
        RefreshEnd();
        adapter.SetReadHerePosition(readHere);
        //リストビューのスクロール位置をずらす
        if (0 < readHere && 20 < adapter.getCount())
          mListView.setSelectionFromTop(readHere - 1, 10);
      }
    };
  }

  private RequestListener UpdateRoomRequestListener() {
    return new RequestListener() {
      @Override
      public Runnable OnRequest(String path) throws IOException, JSONException {
        ConcurrentHashMap<String, TwimptRoom> twimptRooms = mGlobals.twimptRooms;
        JSONObject json = TwimptNetwork.UpdateRequest(path);
        ParsedRoomData parsedData = new ParsedRoomData(json, twimptRooms);
        return UpdateRunnable(parsedData.nowTwimptRoom, parsedData, twimptRooms);
      }
    };
  }

  private RequestListener UpdateUserRequestListener() {
    return new RequestListener() {
      @Override
      public Runnable OnRequest(String path) throws IOException, JSONException {
        ConcurrentHashMap<String, TwimptRoom> twimptRooms = mGlobals.twimptRooms;
        JSONObject json = TwimptNetwork.UpdateRequest(path);
        ParsedUserData parsedData = new ParsedUserData(json, twimptRooms);
        return UpdateRunnable(parsedData.nowUserRoomData, parsedData, twimptRooms);
      }
    };
  }

  private void UpdateRequestRun(String path, RequestListener listener) {
    try {
      mHandler.post(listener.OnRequest(path));
    } catch (Exception e) {
      e.printStackTrace();
      ShowErrorMessage(e.getMessage());
    }
  }

  private void UpdateRequest(final String path, final RequestListener listener) {
    if (!CanRequest()) return;
    exec.execute(new Runnable() {
      @Override
      public void run() {
        UpdateRequestRun(path, listener);
      }
    });
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    class ViewHolder {
      TextView text, name, roomName, time;
      ImageView icon;
      GridView mGridView;
    }
    // コンテキストメニューを作る
    AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) menuInfo;
    ListView listView = (ListView) v;
    //「選んだものが「ここまで読んだ」タグの場合は何もしない
    if (adapterInfo.position == adapter.GetReadHerePosition()) return;
    TwimptLogData twimptLogData = (TwimptLogData) listView.getItemAtPosition(adapterInfo.position);
    //TwimptLogData twimptLogData = mGlobals.twimptRooms.get(mGlobals.mNowHash).dataList.get(mListViewClickedNum);
    menu.setHeaderTitle(twimptLogData.text);
    View view = (View) v.getTag();
    ViewHolder holder;
    if (view == null) {
      LayoutInflater inflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      view = inflate.inflate(R.layout.selected_data_popup, null);
      holder = new ViewHolder();
      holder.text = (TextView) view.findViewById(R.id.text);
      holder.name = (TextView) view.findViewById(R.id.name);
      holder.roomName = (TextView) view.findViewById(R.id.room_name);
      holder.icon = (ImageView) view.findViewById(R.id.user_image);
      holder.time = (TextView) view.findViewById(R.id.time);
      holder.mGridView = (GridView) view.findViewById(R.id.gridImageView);
      holder.mGridView.setVisibility(View.GONE);
      view.setTag(holder);
    } else {
      holder = (ViewHolder) view.getTag();
    }
    holder.icon.setImageDrawable(mIconListener.getDrawable(twimptLogData.icon));
    holder.text.setText(twimptLogData.decodedText);
    holder.name.setText(twimptLogData.name);
    holder.roomName.setText(twimptLogData.roomData.name);
    holder.time.setText(TimeDiff.toDiffDate(twimptLogData.time));
    menu.setHeaderView(view);
    //menu.add(0, CONTEXT_MENU.COMMENT.ordinal(), 0, "書き込みにコメントする");
    //String roomName = twimptLogData.roomHash != null ? mGlobals.twimptRooms.get(twimptLogData.roomHash).name : "ひとりごと";
    if (mNowHash.equals("public"))
      menu.add(0, CONTEXT_MENU.OPEN_ROOM.ordinal(), 0, R.string.open_room);
    String userHash = twimptLogData.user.hash;
    if (twimptLogData.user.hash != null && userHash.length() != 0)
      menu.add(0, CONTEXT_MENU.OPEN_USER_ROOM.ordinal(), 0, R.string.open_user_room);
    menu.add(0, CONTEXT_MENU.OPEN_ID_ROOM.ordinal(), 0, R.string.open_id_room);
    menu.add(0, CONTEXT_MENU.WRITE_ROOM.ordinal(), 0, R.string.write_room);
    menu.add(0, CONTEXT_MENU.CANCEL.ordinal(), 0, R.string.cancel);
    super.onCreateContextMenu(menu, v, menuInfo);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    // 選択されたヤツをテキストビューに設定する
    //Toast.makeText(this, item.getTitle(), Toast.LENGTH_LONG).show();
    //info.positionはListViewのクリックされたリストの番号
    TwimptLogData twimptLogData = (TwimptLogData) mListView.getItemAtPosition(info.position);
    CONTEXT_MENU menu = CONTEXT_MENU.values()[item.getItemId()];
    switch (menu) {
    case CANCEL:
      return super.onContextItemSelected(item);

    case COMMENT:
      //StartPostActivity(twimptLogData.roomHash, mNowHash, twimptLogData.name);
      Toast.makeText(this, "未実装", Toast.LENGTH_LONG).show();
      return true;
    case OPEN_ROOM: {
      //mNowHash = twimptLogData.roomHash;
      //      ChangeRoom(twimptLogData.roomData.hash);
      //      UpdateRequest();
      Intent intent = new Intent(RoomActivity.this, RoomActivity.class);
      //選択されたログのルームハッシュを送る
      String hash = twimptLogData.roomData.hash;
      if (hash == null) hash = twimptLogData.roomData.type;
      intent.putExtra(INTENT_ROOM_NAME_HASH, hash);
      startActivity(intent);
      return true;
    }
    case OPEN_USER_ROOM: {
      //      ChangeRoom(twimptLogData.user.hash);
      //      UpdateRequest();
      Intent intent = new Intent(RoomActivity.this, RoomActivity.class);
      intent.putExtra(INTENT_ROOM_NAME_HASH, twimptLogData.user.hash);
      startActivity(intent);
      return true;
    }
    case OPEN_ID_ROOM: {
      Intent intent = new Intent(RoomActivity.this, RoomActivity.class);
      intent.putExtra(INTENT_ROOM_NAME_HASH, twimptLogData.host.hash);
      startActivity(intent);
      return true;
    }
    case WRITE_ROOM:
        /* publicのルームはここには来ない */
      StartPostActivity(twimptLogData.roomData.hash, mNowHash);
      return true;
    default:
      Toast.makeText(this, "エラー", Toast.LENGTH_LONG).show();
      return super.onContextItemSelected(item);
    }
  }

  /**
   * @param twimptRooms 部屋の一覧
   * @param twimptRoom  現在表示してる部屋
   * @param parsedData  ログのjsonがパースされたデータ
   */
  public static void PushUpdateData(Map<String, TwimptRoom> twimptRooms, TwimptRoom twimptRoom,
                                    ParsedData parsedData) {
    final TwimptLogData[] twimptLogData = parsedData.mTwimptLogData;
    final int size = twimptLogData.length;
    final ArrayList<TwimptLogData> dataList = twimptRoom.dataList;
    for (int i = size - 1; i >= 0; i--) {
      dataList.add(0, twimptLogData[i]);
    }
    twimptRooms.putAll(parsedData.mTwimptRooms);
  }

  /**
   * @param twimptRooms 部屋の一覧
   * @param twimptRoom  現在表示してる部屋
   * @param parsedData  ログのjsonがパースされたデータ
   */
  public static void PushLogData(Map<String, TwimptRoom> twimptRooms, TwimptRoom twimptRoom,
                                 ParsedData parsedData) {
    final TwimptLogData[] twimptLogData = parsedData.mTwimptLogData;
    final int size = twimptLogData.length;
    final ArrayList<TwimptLogData> dataList = twimptRoom.dataList;
    for (int i = 0; i < size; i++) {
      dataList.add(twimptLogData[i]);
    }
    twimptRooms.putAll(parsedData.mTwimptRooms);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.my, menu);
    //TODO 途中で認証した時の切り替えを考えるまで保留
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    Logger.log("onPrepareOptionsMenu");
    if (isAuth()) {
      //認証している場合は認証メニューを削除
      menu.removeItem(R.id.action_auth);
      if (menu.findItem(R.id.action_deauthentication) == null)
        menu.add(0, R.id.action_deauthentication, 0, R.string.action_deauth);
    } else {
      //認証していない場合は認証解除メニューを削除
      menu.removeItem(R.id.action_deauthentication);
      if (menu.findItem(R.id.action_auth) == null)
        menu.add(0, R.id.action_auth, 0, R.string.action_auth);
    }
    return super.onPrepareOptionsMenu(menu);
  }


  /*  menuが押されたときに呼ばれる  */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.post_request:
      if (mNowHash.equals("public")) {
        //警告ダイアログ表示
        AlertDialogFragment dlg = AlertDialogFragment.CreatePostAlert();
        dlg.show(getSupportFragmentManager(), "tag");
      } else {
        StartPostActivity(mNowHash, mNowHash);
      }
      break;
    case R.id.action_auth://認証activityを開く
      if (!isAuth()) {
        Intent intent = new Intent(RoomActivity.this, TwimptAuthActivity.class);
        startActivity(intent);
      } else {
        Toast.makeText(this, R.string.authenticated, Toast.LENGTH_LONG).show();
      }
      break;
    case R.id.action_deauthentication: //認証を解除
      if (isAuth()) {
        TwimptToken.ClearAccessToken(this);
        Toast.makeText(this, R.string.authenticated_release, Toast.LENGTH_LONG).show();
      } else {
        Toast.makeText(this, R.string.not_authenticated, Toast.LENGTH_LONG).show();
      }
      break;
    case R.id.open_official_now_page: //公式ページをブラウザで開く
      TwimptRoom twimptRoom = mGlobals.twimptRooms.get(mNowHash);
      Uri uri = Uri.parse(TwimptNetwork.GetWebPage(twimptRoom.type, twimptRoom.id));
      Intent i = new Intent(Intent.ACTION_VIEW, uri);
      startActivity(i);
      break;
    case R.id.update_request:
      UpdateRequest();
      break;
    default:
      Toast.makeText(this, "selected menu, but Unimplemented", Toast.LENGTH_LONG).show();
      break;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
  }

  private void StartPostActivity(String postRoomHash, String nowRoomHash) {
    AccessTokenData accessToken = TwimptToken.GetAccessToken(this);
    if (accessToken == null) {
      Toast.makeText(this, R.string.encourage_auth, Toast.LENGTH_LONG).show();
      return;
    }
    Intent intent = new Intent(this, PostActivity.class);
    intent.putExtra(PostActivity.INTENT_NOW_HASH, nowRoomHash);
    intent.putExtra(PostActivity.INTENT_POST_HASH, postRoomHash);
    //intent.setFlags(FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
    startActivity(intent);
  }

  //TODO 認証チェックをメンバ変数に持つようにする アクティビティ起動時？にチェック
  private boolean isAuth() {
    AccessTokenData accessToken = TwimptToken.GetAccessToken(this);
    return accessToken != null;
  }

  private void UpdateRequestRun() {
    try {
      final ConcurrentHashMap<String, TwimptRoom> twimptRooms = mGlobals.twimptRooms;
      final TwimptRoom twimptRoom = twimptRooms.get(mNowHash);
      String latestHash = twimptRoom.getLatestLogHash();
      final JSONObject json = TwimptNetwork.UpdateRequest(twimptRoom.type, twimptRoom.hash, latestHash, twimptRoom.LatestModifyHash);
      final ParsedData parsedData = new ParsedData(json, twimptRooms);
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          final int readHere = parsedData.mTwimptLogData.length;
          PushUpdateData(twimptRooms, twimptRoom, parsedData);
          RefreshEnd();
          adapter.SetReadHerePosition(readHere);
          //リストビューのスクロール位置をずらす
          if (readHere > 0 && twimptRoom.dataList.size() > 20)
            mListView.setSelectionFromTop(readHere - 1, 10);
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
      ShowErrorMessage(e.getMessage());
    }
  }

  private void LogRequestRun() {
    try {
      final ConcurrentHashMap<String, TwimptRoom> twimptRooms = mGlobals.twimptRooms;
      final TwimptRoom twimptRoom = twimptRooms.get(mNowHash);
      final JSONObject json = TwimptNetwork.LogRequest(twimptRoom.type, twimptRoom.hash, twimptRoom.getOldestLogHash());
      final ParsedData parsedData = new ParsedData(json, twimptRooms);
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          PushLogData(twimptRooms, twimptRoom, parsedData);
          RefreshEnd();
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
      ShowErrorMessage(e.getMessage());
    }
  }

  private boolean CanRequest() {
    if (!Network.isConnected(this)) {
      RefreshEnd();
      Toast.makeText(RoomActivity.this, R.string.update_network_error, Toast.LENGTH_LONG).show();
      return false;
    }
    if (!RefreshStart()) {
      RefreshEnd();
      Toast.makeText(RoomActivity.this, R.string.updating, Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  /** 最新ログの取得スレッドを実行 */
  private void UpdateRequest() {
    if (!CanRequest()) return;
    exec.execute(new Runnable() {
      @Override
      public void run() {
        UpdateRequestRun();
      }
    });
  }

  private void LogRequest() {
    if (!CanRequest()) return;
    exec.execute(new Runnable() {
      @Override
      public void run() {
        LogRequestRun();
      }
    });
  }

  private boolean RefreshStart() {
    //すでにアップデート中の場合
    if (mNowUpdate) return false;

    mNowUpdate = true;
    mSwipeRefreshWidget.setRefreshing(true);
    mSwipeRefreshWidget.setEnabled(false);
    return true;
  }

  private void RefreshEnd() {
    mNowUpdate = false;
    mSwipeRefreshWidget.setRefreshing(false);
    mSwipeRefreshWidget.setEnabled(true);
    adapter.notifyDataSetChanged();
    int cnt = adapter.getCount();
    if (0 < cnt) {
      listEndButton.setVisibility(View.VISIBLE);

    }
  }

  /** アラートダイアログ */
  public static class AlertDialogFragment extends DialogFragment {
    private final static String BUNDLE_TWIMPT_LOG_DATA = "TwimptLogData";

    private enum DIALOG {
      POST_ALERT,
      SELECTED_DATA,
    }

    public AlertDialogFragment() {
    }

    public static AlertDialogFragment CreateSelectedDataDialog(TwimptLogData data) {
      AlertDialogFragment dlg = new AlertDialogFragment();
      Bundle bundle = new Bundle();
      bundle.putInt("id", DIALOG.SELECTED_DATA.ordinal());
      bundle.putSerializable(BUNDLE_TWIMPT_LOG_DATA, data);
      dlg.setArguments(bundle);
      return dlg;
    }

    public static AlertDialogFragment CreatePostAlert() {
      AlertDialogFragment dlg = new AlertDialogFragment();
      Bundle bundle = new Bundle();
      bundle.putInt("id", DIALOG.POST_ALERT.ordinal());
      dlg.setArguments(bundle);
      return dlg;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      Dialog dialog;
      Bundle bundle = getArguments();
      AlertDialog.Builder builder;
      DIALOG[] values = DIALOG.values();
      DIALOG dialogID = values[bundle.getInt("id")];
      switch (dialogID) {
      case POST_ALERT:
        // 確認ダイアログの生成
        builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.confirmation);
        builder.setMessage(R.string.confirmation_post_to_public);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            //親のアクティビティを取得
            RoomActivity This = ((RoomActivity) getActivity());
            This.StartPostActivity("monologue", This.mNowHash);
          }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
          }
        });
        dialog = builder.create();
        break;
      case SELECTED_DATA:
        Activity activity = getActivity();
        LayoutInflater inflate = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflate.inflate(R.layout.selected_data_popup, null);
        TextView text = (TextView) v.findViewById(R.id.text);
        TextView name = (TextView) v.findViewById(R.id.name);
        TextView roomName = (TextView) v.findViewById(R.id.room_name);
        ImageView icon = (ImageView) v.findViewById(R.id.user_image);
        TextView time = (TextView) v.findViewById(R.id.time);
        GridView mGridView = (GridView) v.findViewById(R.id.gridImageView);
        mGridView.setVisibility(View.GONE);
        TwimptLogData data = (TwimptLogData) bundle.getSerializable(BUNDLE_TWIMPT_LOG_DATA);
        text.setText(data.decodedText);
        name.setText(data.name);
        roomName.setText(data.roomData.name);

        builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(null);
        builder.setView(v);
        String[] s = {
                "test1",
                "test2"
        };
        builder.setItems(s, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {

          }
        });
        builder.setMessage(null);
        dialog = builder.create();
        break;
      default:
        dialog = null;
      }
      return dialog;
    }
  }

  private Map<String, Drawable> ImageCacheDrawable = new HashMap<String, Drawable>();

  class TwimptIconListener implements TwimptListAdapter.DrawableListener {
    private TwimptIconTable mIconTable;
    private Context mContext;
    private ExecutorService mExec;

    public TwimptIconListener(Context context, TwimptIconTable iconTable, ExecutorService exec) {
      mIconTable = iconTable;
      mContext = context;
      mExec = exec;
    }

    @Override
    public Drawable getDrawable(String url) {
      Drawable drawable;
      drawable = ImageCacheDrawable.get(url); //キャッシュから画像を取得
      if (drawable != null)
        return drawable;
      //データベースから画像を取得
      drawable = mIconTable.getDrawable(url);
      if (drawable != null)
        ImageCacheDrawable.put(url, drawable);
      return drawable;
    }

    @Override
    public FileDownloadThread downloadDrawable(final String hash) {

      if (ImageCacheDrawable.containsKey(hash)) return null;
      ImageCacheDrawable.put(hash, null);

      return new FileDownloadThread(mExec, "http://twimpt.com/icon/" + hash, new FileDownloader.OnDownloadBeginListener() {
        @Override
        public OutputStream DownloadBegin(URLConnection urlConnection) {
          try {
            final long length = urlConnection.getContentLength();
            return mIconTable.openFileOutput(hash, length, mContext);
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          }
          return null;
        }
      });
    }
  }

  class TwimptImageListener implements TwimptImageAdapter.DrawableListener {
    private TwimptImageTable mImageTable;
    private Context mContext;
    private ExecutorService mExec;

    public TwimptImageListener(Context context, TwimptImageTable imageTable, ExecutorService exec) {
      mImageTable = imageTable;
      mContext = context;
      mExec = exec;
    }

    @Override
    public Drawable getDrawable(String hash) {
      Drawable drawable;
      drawable = ImageCacheDrawable.get(hash); //キャッシュから画像を取得
      if (drawable != null)
        return drawable;
      //データベースから画像を取得
      drawable = mImageTable.getDrawable(hash, mContext);
      if (drawable != null)
        ImageCacheDrawable.put(hash, drawable);
      return drawable;
    }

    @Override
    public FileDownloadThread downloadDrawable(final String hash) {
      if (ImageCacheDrawable.containsKey(hash)) return null;
      ImageCacheDrawable.put(hash, null);
      return new FileDownloadThread(mExec, "http://twimpt.com/upload/original/" + hash, new FileDownloader.OnDownloadBeginListener() {
        @Override
        public OutputStream DownloadBegin(URLConnection urlConnection) {
          try {
            final long length = urlConnection.getContentLength();
            return mImageTable.openFileOutput(hash, length, mContext);
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          }
          return null;
        }
      });
    }
  }
}
