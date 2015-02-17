package com.example.oigami.twimpt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.oigami.twimpt.debug.Logger;
import com.example.oigami.twimpt.image.FileDownloadThread;
import com.example.oigami.twimpt.image.FileDownloader;
import com.example.oigami.twimpt.image.ImageCacheDB;
import com.example.oigami.twimpt.twimpt.TwimptJson;
import com.example.oigami.twimpt.twimpt.TwimptLogData;
import com.example.oigami.twimpt.twimpt.TwimptNetwork;
import com.example.oigami.twimpt.twimpt.TwimptRoom;
import com.example.oigami.twimpt.twimpt.token.AccessTokenData;

import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoomActivity extends ActionBarActivity {
  public enum CONTEXT_MENU {
    CANCEL,
    COMMENT,
    OPEN_ROOM,
    WRITE_ROOM,
  }

  private enum DIALOG {
    POST_ALERT
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
  ImageCacheDB userImageDB;
  ImageCacheDB uploadImageDB;
  /** コア数 */
  private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
  ExecutorService exec;
  /** ここまで読んだタグの位置 */
  private int readHere = 0;
  private DrawableListener mIconListener, mImageListener;

  //private String accessToken, accessTokenSecret;
  enum WhatHandler {
    ERROR,
    SUCCESS,
  }

  Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      WhatHandler whats[] = WhatHandler.values();
      WhatHandler what = whats[message.what];
      switch (what) {
        case ERROR:
          Toast.makeText(RoomActivity.this, (String) message.obj, Toast.LENGTH_LONG).show();
          break;
        case SUCCESS:
          break;
      }
      RefreshEnd();
    }
  };

  private void ShowErrorMessage(String errMsg) {
    Message msg = Message.obtain(mHandler, WhatHandler.ERROR.ordinal(), errMsg);
    mHandler.sendMessage(msg);
  }

  void deleteDatabase() {
    deleteDatabase("imagecache.db");
    deleteDatabase("PostedImage.db");
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
    }
    Editor e = sharedPref.edit();
    e.putInt("code", nowVersion);
    e.commit();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_my);
    exec = Executors.newFixedThreadPool(NUMBER_OF_CORES >= 1 ? NUMBER_OF_CORES + 1 : 2);
    //deleteDatabase();
    VersionCheck();
    userImageDB = new ImageCacheDB(RoomActivity.this, "imagecache.db");
    uploadImageDB = new ImageCacheDB(RoomActivity.this, "PostedImage.db");
//    deleteDatabase(uploadImageDB.getDbFileName());
//    uploadImageDB = new ImageCacheDB(RoomActivity.this, "PostedImage.db");

    mGlobals = (DataApplication) this.getApplication();
    // ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します

    Intent i = getIntent();
    mNowHash = i.getStringExtra(INTENT_ROOM_NAME_HASH);
    if (mNowHash == null) mNowHash = "public";
    TwimptRoom nowRoom = mGlobals.twimptRooms.get(mNowHash);
    getSupportActionBar().setTitle(nowRoom.name);
    mIconListener = new DrawableListener(RoomActivity.this, userImageDB, exec);
    mImageListener = new DrawableListener(RoomActivity.this, uploadImageDB, exec);
    adapter = new TwimptListAdapter(RoomActivity.this, mGlobals.twimptRooms, nowRoom,
            mIconListener, mImageListener);
    mSwipeRefreshWidget = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_widget);
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
      }
    });
    mListView.setAdapter(adapter);
    adapter.setOnImageItemClickListener(new AdapterView.OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TwimptImageAdapter imageAdapter = (TwimptImageAdapter) parent.getAdapter();
        //drawableをbitmapに変換する
        String url = imageAdapter.getItemUrl(position);
        String filename = uploadImageDB.getFileName(url, RoomActivity.this);
        Intent intent = new Intent(RoomActivity.this, ImageViewActivity.class);
        intent.putExtra(ImageViewActivity.INTENT_DRAWABLE_FILENAME, filename);
        startActivity(intent);
      }
    });
    mSwipeRefreshWidget.setColorSchemeResources(
            R.color.blue_bright, R.color.green_light, R.color.orange_light, R.color.red_light);
    // 1. スワイプ時のリスナを登録する（implements OnRefreshListener）
    mSwipeRefreshWidget.setOnRefreshListener(new OnRefreshListener() {
      @Override
      public void onRefresh() {
        UpdateRequest();
      }
    });
    //mSwipeRefreshWidget.setRefreshing(true);

    UpdateRequest();

    registerForContextMenu(mListView);
  }


  @Override
  public void onStart() {
    super.onStart();
    //TextView textView = (TextView) findViewById(R.id.textview);
    //textView.setText(mGlobals.twimptRooms.get(mNowHash).name);
    adapter.notifyDataSetChanged();
  }

  @Override
  public void onResume() {
    super.onResume();
    Intent intent = getIntent();
    String action = intent.getAction();
    if (!Intent.ACTION_VIEW.equals(action))
      return;
    Uri uri = intent.getData();
    if (uri == null)
      return;
    String path = uri.getPath();
    if (path == null) return;
    path = path.substring(1);
    if (path.equals("public") || path.equals("monologue")) {
      mNowHash = path;
      Map<String, TwimptRoom> twimptRoomMap = mGlobals.twimptRooms;
      adapter.reset(this, twimptRoomMap, twimptRoomMap.get(mNowHash), mIconListener, mImageListener);
      adapter.notifyDataSetChanged();
      return;
    }
    String regex = "(room|user|log)/(.*?)";
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(path);
    if (m.find()) {
      final String type = m.group(1);
      final String id = m.group(2);
      /** 最新ログの取得スレッドを実行 */
      if (!CanRequest()) return;
      exec.execute(new Runnable() {
        @Override
        public void run() {
          try {
            final Map<String, TwimptRoom> twimptRooms = mGlobals.twimptRooms;
            final TwimptRoom twimptRoom = twimptRooms.get(mNowHash);
            final JSONObject json = TwimptNetwork.UpdateRequest(type, id);
            final TwimptJson.ParsedData parsedData = TwimptJson.UpdateParse(json);
            mHandler.post(new Runnable() {
              @Override
              public void run() {
                readHere = parsedData.mTwimptLogData.length;
                PushData(RoomActivity.this, twimptRooms, twimptRoom, parsedData, true);
                RefreshEnd();
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
      });
    }
    assert false;
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    // コンテキストメニューを作る
    AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) menuInfo;
    ListView listView = (ListView) v;
    //「選んだものが「ここまで読んだ」タグの場合は何もしない
    if (adapterInfo.position == readHere) return;
    TwimptLogData twimptLogData = (TwimptLogData) listView.getItemAtPosition(adapterInfo.position);
    //TwimptLogData twimptLogData = mGlobals.twimptRooms.get(mGlobals.mNowHash).dataList.get(mListViewClickedNum);
    menu.setHeaderTitle(twimptLogData.text);
    //TODO 現状APIがないのでコメントアウト
    //menu.add(0, CONTEXT_MENU.COMMENT.ordinal(), 0, "書き込みにコメントする");
    //String roomName = twimptLogData.roomHash != null ? mGlobals.twimptRooms.get(twimptLogData.roomHash).name : "ひとりごと";
    if (mNowHash.equals("public"))
      menu.add(0, CONTEXT_MENU.OPEN_ROOM.ordinal(), 0, R.string.open_room);
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
      case OPEN_ROOM:
        //mNowHash = twimptLogData.roomHash;
        Intent intent = new Intent(RoomActivity.this, RoomActivity.class);
        //選択されたログのルームハッシュを送る
        intent.putExtra(INTENT_ROOM_NAME_HASH, twimptLogData.roomData.hash);
        startActivity(intent);
        return true;
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
   * @param content     コンテキスト
   * @param twimptRooms 部屋の一覧
   * @param twimptRoom  現在表示してる部屋
   * @param parsedData  ログのjsonがパースされたデータ
   * @param top         ログの挿入する位置 trueは上 falseは下 に挿入する
   */
  public static void PushData(Context content, Map<String, TwimptRoom> twimptRooms, TwimptRoom twimptRoom,
                              TwimptJson.ParsedData parsedData, final boolean top) {
    final int size = parsedData.mTwimptLogData.length;
    for (int i = 0; i < size; i++) {
      TwimptLogData it = parsedData.mTwimptLogData[i];
      if (top) {
        twimptRoom.dataList.add(i, it);
      } else {
        twimptRoom.dataList.add(it);
      }
    }
    for (Map.Entry<String, TwimptRoom> e : parsedData.mTwimptRooms.entrySet()) {
      if (!twimptRooms.containsKey(e.getKey()))
        twimptRooms.put(e.getKey(), e.getValue());
    }
    return;
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
      final Map<String, TwimptRoom> twimptRooms = mGlobals.twimptRooms;
      final TwimptRoom twimptRoom = twimptRooms.get(mNowHash);
      String latestHash = twimptRoom.getLatestLogHash();
      final JSONObject json = TwimptNetwork.UpdateRequest(twimptRoom.type, twimptRoom.hash, latestHash, twimptRoom.LatestModifyHash);
      final TwimptJson.ParsedData parsedData = TwimptJson.UpdateParse(json);
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          readHere = parsedData.mTwimptLogData.length;
          PushData(RoomActivity.this, twimptRooms, twimptRoom, parsedData, true);
          RefreshEnd();
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
      final Map<String, TwimptRoom> twimptRooms = mGlobals.twimptRooms;
      final TwimptRoom twimptRoom = twimptRooms.get(mNowHash);
      final JSONObject json = TwimptNetwork.LogRequest(twimptRoom.type, twimptRoom.hash, twimptRoom.getOldestLogHash());
      final TwimptJson.ParsedData parsedData = TwimptJson.LogParse(json);
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          PushData(RoomActivity.this, twimptRooms, twimptRoom, parsedData, false);
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
      public void run() { UpdateRequestRun(); }
    });
  }

  private void LogRequest() {
    if (!CanRequest()) return;
    exec.execute(new Runnable() {
      @Override
      public void run() { LogRequestRun(); }
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
    if (adapter.getCount() > 0) listEndButton.setVisibility(View.VISIBLE);
  }

  /** アラートダイアログ */
  public static class AlertDialogFragment extends DialogFragment {
    public AlertDialogFragment() { }

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
      AlertDialog.Builder builder;
      DIALOG[] values = DIALOG.values();
      DIALOG dialogID = values[getArguments().getInt("id")];
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
            public void onClick(DialogInterface dialog, int which) { }
          });
          dialog = builder.create();
          break;
        default:
          dialog = null;
      }
      return dialog;
    }
  }

  private Map<String, Drawable> ImageCacheDrawable = new HashMap<String, Drawable>();

  class DrawableListener implements TwimptImageAdapter.DrawableListener {
    private ImageCacheDB mDatabase;
    private Context mContext;
    private ExecutorService mExec;

    public DrawableListener(Context context, ImageCacheDB database, ExecutorService exec) {
      mDatabase = database;
      mContext = context;
      mExec = exec;
    }

    @Override
    public Drawable getDrawable(String url) {
      Drawable drawable;
      //キャッシュから画像を取得
      drawable = ImageCacheDrawable.get(url);
      if (drawable != null) {
        //mTwimptLogData.postedImageUrl.set(position, new Pair<String, Drawable>(url, drawable));
        return drawable;
      }
      //データベースから画像を取得
      drawable = mDatabase.getDrawable(url, mContext);
      if (drawable != null) {
        ImageCacheDrawable.put(url, drawable);
      }
      return drawable;
    }

    @Override
    public FileDownloadThread downloadDrawable(final String url) {
      if (ImageCacheDrawable.containsKey(url)) return null;
      ImageCacheDrawable.put(url, null);
      return new FileDownloadThread(mExec, url, new FileDownloader.OnDownloadBeginListener() {
        @Override
        public OutputStream DownloadBegin(URLConnection urlConnection) {
          try {
            final String type = urlConnection.getContentType();
            final long length = urlConnection.getContentLength();
            return mDatabase.openFileOutput(url, type, length, mContext);
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          }
          return null;
        }
      });
      //キャッシュにロードする
      //ImageCacheDrawable.put(url, uploadImageDB.getDrawable(url, RoomActivity.this));
    }
  }
}
