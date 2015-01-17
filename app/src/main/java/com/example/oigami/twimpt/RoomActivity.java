package com.example.oigami.twimpt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import android.text.Html;
import android.text.Spannable;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.example.oigami.twimpt.debug.Logger;
import com.example.oigami.twimpt.image.FileDownloadNonThread;
import com.example.oigami.twimpt.image.ImageCacheDB;
import com.example.oigami.twimpt.image.MultiThreadImageDownloader;
import com.example.oigami.twimpt.textLink.MutableLinkMovementMethod;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
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
    OPEN,
    WRITE_ROOM,
  }

  private enum DIALOG {
    POST_ALERT
  }

  public static String INTENT_ROOM_NAME_HASH = "NOW_ROOM_HASH";
  public static String INTENT_NAME_TYPE = "DATA_TYPE";

  private SwipeRefreshLayout mSwipeRefreshWidget;
  private ListView mListView;
  private boolean mNowUpdate = false;
  private DataApplication mGlobals;
  private String mNowHash;  //現在の部屋のハッシュ "room", "user", "public", etc..

  private TwimptRoomsAdapter adapter = new TwimptRoomsAdapter();
  private Button listEndButton;
  ImageCacheDB userImageDB = new ImageCacheDB(RoomActivity.this, "imagecache.db");
  ImageCacheDB uploadImageDB = new ImageCacheDB(RoomActivity.this, "PostedImage.db");
  /** コア数 */
  private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
  ExecutorService exec = Executors.newFixedThreadPool(NUMBER_OF_CORES >= 1 ? NUMBER_OF_CORES : 1);
  /** ここまで読んだタグの位置 */
  private int readHere = 0;

  //private String accessToken, accessTokenSecret;
  enum What {
    ERROR,
    SUCCESS,
  }

  Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      What whats[] = What.values();
      What what = whats[message.what];
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
    Message msg = Message.obtain(mHandler, What.ERROR.ordinal(), errMsg);
    mHandler.sendMessage(msg);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_my);
//    deleteDatabase(uploadImageDB.getDbFileName());
//    uploadImageDB = new ImageCacheDB(RoomActivity.this, "PostedImage.db");

    mGlobals = (DataApplication) this.getApplication();
    // ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します

    Intent i = getIntent();
    mNowHash = i.getStringExtra(INTENT_ROOM_NAME_HASH);
    if (mNowHash == null) mNowHash = "public";
    getSupportActionBar().setTitle(mGlobals.twimptRooms.get(mNowHash).name);

    adapter.notifyDataSetChanged();
    mSwipeRefreshWidget = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_widget);
    //mSwipeRefreshWidget.setColorScheme(0xff66cdaa, 0xffff89c4, 0xffff89c4, 0xffff89c4);
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

    VersionCheck();
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
      //ファイル名をリネームする
      for (String s : this.fileList()) {
        File originalFile = getFileStreamPath(s);
        String newFileName = userImageDB.getDbFileName() + "_" + s;
        File newFile = new File(originalFile.getParent(), newFileName);
        if (newFile.exists()) {
          // Or you could throw here.
          deleteFile(newFileName);
        }
        originalFile.renameTo(newFile);
      }
    }
    Editor e = sharedPref.edit();
    e.putInt("code", nowVersion);
    e.commit();
  }


  @Override
  public void onStart() {
    super.onStart();
    //TextView textView = (TextView) findViewById(R.id.textview);
    //textView.setText(mGlobals.twimptRooms.get(mNowHash).name);
    adapter.notifyDataSetChanged();
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    // コンテキストメニューを作る
    AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) menuInfo;
    ListView listView = (ListView) v;
    if (adapterInfo.position == readHere)//「選んだものば「ここまで読んだ」タグの場合は何もしない
      return;
    TwimptLogData twimptLogData = (TwimptLogData) listView.getItemAtPosition(adapterInfo.position);
    //TwimptLogData twimptLogData = mGlobals.twimptRooms.get(mGlobals.mNowHash).dataList.get(mListViewClickedNum);
    menu.setHeaderTitle(twimptLogData.text);
    //TODO 現状APIがないのでコメントアウト
    //menu.add(0, CONTEXT_MENU.COMMENT.ordinal(), 0, "書き込みにコメントする");
    //String roomName = twimptLogData.roomHash != null ? mGlobals.twimptRooms.get(twimptLogData.roomHash).name : "ひとりごと";
    if (mNowHash.equals("public"))
      menu.add(0, CONTEXT_MENU.OPEN.ordinal(), 0, R.string.open_room);
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
      case OPEN:
        //mNowHash = twimptLogData.roomHash;
        Intent intent = new Intent(RoomActivity.this, RoomActivity.class);
        //選択されたログのルームハッシュを送る
        intent.putExtra(INTENT_ROOM_NAME_HASH, twimptLogData.roomHash);
        startActivity(intent);
        return true;
      case WRITE_ROOM:
        /* publicのルームはここには来ない */
        StartPostActivity(twimptLogData.roomHash, mNowHash);
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
  public static List<String> PushData(Context content, Map<String, TwimptRoom> twimptRooms, TwimptRoom twimptRoom,
                                      TwimptJson.ParsedData parsedData, final boolean top) {
    ArrayList<String> icon = new ArrayList<String>();
    final int size = parsedData.mTwimptLogData.length;
    int count = 0;
    for (int i = 0; i < size; i++) {
      TwimptLogData it = parsedData.mTwimptLogData[i];
      if (!icon.contains("http://twimpt.com/icon/" + it.icon))
        icon.add("http://twimpt.com/icon/" + it.icon);
      if (top) {
        twimptRoom.dataList.add(count++, it);
      } else {
        twimptRoom.dataList.add(it);
      }
    }
    for (Map.Entry<String, TwimptRoom> e : parsedData.mTwimptRooms.entrySet()) {
      if (!twimptRooms.containsKey(e.getKey()))
        twimptRooms.put(e.getKey(), e.getValue());
    }
    return icon;
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

  //TODO 認証チェックをメンバ変数に持つようにする アクティビティ起動時？にチェック
  private boolean isAuth() {
    AccessTokenData accessToken = TwimptToken.GetAccessToken(this);
    return accessToken != null;
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
        Uri uri = Uri.parse(Twimpt.GetWebPage(twimptRoom.type, twimptRoom.hash, twimptRoom.id));
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


  private void UpdateRequestRun() {
    try {
      final Map<String, TwimptRoom> twimptRooms = mGlobals.twimptRooms;
      final TwimptRoom twimptRoom = twimptRooms.get(mNowHash);
      String latestHash = twimptRoom.getLatestLogHash();
      final JSONObject json = Twimpt.UpdateRequest(twimptRoom.type, twimptRoom.hash, latestHash, twimptRoom.LatestModifyHash);
      final TwimptJson.ParsedData parsedData = TwimptJson.UpdateParse(json);
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          readHere = parsedData.mTwimptLogData.length;
          List<String> icon = PushData(RoomActivity.this, twimptRooms, twimptRoom, parsedData, true);
          MultiThreadImageDownloader.execute(userImageDB, RoomActivity.this, icon);
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
      final JSONObject json = Twimpt.LogRequest(twimptRoom.type, twimptRoom.hash, twimptRoom.getOldestLogHash());
      final TwimptJson.ParsedData parsedData = TwimptJson.LogParse(json);
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          List<String> icon = PushData(RoomActivity.this, twimptRooms, twimptRoom, parsedData, false);
          MultiThreadImageDownloader.execute(userImageDB, RoomActivity.this, icon);
          RefreshEnd();
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
      ShowErrorMessage(e.getMessage());
    }
  }

  /** 最新ログの取得スレッドを実行 */
  private void UpdateRequest() {
    if (!TwimptAsyncTask.isConnected(this)) {
      RefreshEnd();
      Toast.makeText(RoomActivity.this, R.string.update_error, Toast.LENGTH_LONG).show();
      return;
    }
    if (!RefreshStart()) {
      RefreshEnd();
      Toast.makeText(RoomActivity.this, R.string.updating, Toast.LENGTH_SHORT).show();
      return;
    }
    exec.execute(new Runnable() {
      @Override
      public void run() { UpdateRequestRun(); }
    });
  }

  private void LogRequest() {
    if (!TwimptAsyncTask.isConnected(RoomActivity.this)) {
      RefreshEnd();
      Toast.makeText(RoomActivity.this, R.string.update_error, Toast.LENGTH_LONG).show();
      return;
    }
    if (!RefreshStart()) {
      RefreshEnd();
      Toast.makeText(RoomActivity.this, R.string.updating, Toast.LENGTH_SHORT).show();
      return;
    }
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

  /**
   * グリッドビューのアダプタ
   * アダプターは、画像ダウンロード中の文字列表示と画像の表示を行う
   * 画像ダウンロードもここでする
   */
  private Map<String, Drawable> ImageCacheDrawable = new HashMap<String, Drawable>();

  public class ImageAdapter extends BaseAdapter {
    Dictionary<String, String> mStringTextImageDictionary = new Hashtable<String, String>();
    TwimptLogData mTwimptLogData;
    Handler mHandler = new Handler() {
      @Override
      public void handleMessage(Message message) {
        notifyDataSetChanged();
      }
    };

    class ViewHolder {
      TextView mText;
      ImageView mImage;
    }

    ImageAdapter(TwimptLogData twimptLogData) {
      mTwimptLogData = twimptLogData;
      for (Pair<String, Drawable> map : mTwimptLogData.postedImage) {
        mStringTextImageDictionary.put(map.first, "ダウンロード待ち");
      }
    }

    @Override
    public final int getCount() {
      if (mTwimptLogData.postedImage == null) return 0;
      return mTwimptLogData.postedImage.size();
    }

    public final Drawable getItem(int position) {
      Drawable drawable = null;
      //通常の場所から画像を取得
      drawable = mTwimptLogData.postedImage.get(position).second;
      if (drawable != null)
        return drawable;
      String url = mTwimptLogData.postedImage.get(position).first;
      //キャッシュから画像を取得
      drawable = ImageCacheDrawable.get(url);
      if (drawable != null) {
        mTwimptLogData.postedImage.set(position, new Pair<String, Drawable>(url, drawable));
        return drawable;
      }
      //データベースから画像を取得
      drawable = uploadImageDB.getDrawable(url, RoomActivity.this);
      if (drawable != null) {
        mTwimptLogData.postedImage.set(position, new Pair<String, Drawable>(url, drawable));
        ImageCacheDrawable.put(url, drawable);
      }
      return drawable;
    }

    public final long getItemId(int position) {
      return position;
    }

    private ViewHolder CreateViewHolder(View v) {
      ViewHolder holder;
      holder = new ViewHolder();
      // テキストビューを取り出す
      holder.mText = (TextView) v.findViewById(R.id.image_text_text);
      holder.mImage = (ImageView) v.findViewById(R.id.image_text_image);
      return holder;
    }

    //ファイルをダウンロードした又は途中の場合
    private boolean isDownloaded(String url) {
      //キャッシュにあるか判定（ダウンロード中の場合でもtrue）
      if (ImageCacheDrawable.containsKey(url))
        return true;
      //データベースにあるか判定
      Drawable drawable = uploadImageDB.getDrawable(url, RoomActivity.this);
      if (drawable != null) {
        ImageCacheDrawable.put(url, drawable);
        return true;
      }
      return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // 画像表示用のImageView
      View v = convertView;
      ViewHolder holder;
      // convertViewがnullならImageViewを新規に作成する
      if (v == null) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.text_image, null);
        holder = CreateViewHolder(v);
        v.setTag(holder);
      } else {
        // convertViewがnullでない場合は再利用
        holder = (ViewHolder) v.getTag();
      }
      //holder.mGridView.setMinimumHeight(50);
      for (Pair<String, Drawable> postedImage : mTwimptLogData.postedImage) {
        if (postedImage.second == null) {
          final String url = postedImage.first;
          //すでにダウンロードしてキャッシュに読み込んでいるかの確認
          if (isDownloaded(url)) continue;
          //キャッシュにダウンロードしていることを示す
          ImageCacheDrawable.put(url, null);
          //ダウンロードする
          exec.execute(new Runnable() {
            @Override
            public void run() {
              try {
                FileOutputStream fileOutputStream = uploadImageDB.openFileOutput(url, "", RoomActivity.this);
                new FileDownloadNonThread(fileOutputStream, url, new FileDownloadNonThread.Interface() {
                  @Override
                  public void DownloadBegin(URLConnection urlConnection) {
                    Cursor c = uploadImageDB.existsFile(url);
                    if (c.moveToFirst()) {
                      final String filename = c.getString(c.getColumnIndex(ImageCacheDB.CacheColumn.FILENAME));
                      final long id = c.getLong(c.getColumnIndex(ImageCacheDB.CacheColumn.ID));
                      Logger.log(urlConnection.getContentType());
                      uploadImageDB.update(id, filename, urlConnection.getContentType());
                    }
                  }

                  @Override
                  public void Downloading(int loadedByte) {
                    mStringTextImageDictionary.put(url, String.format("%d", loadedByte));
                    mHandler.sendEmptyMessage(0);
                    //Logger.log("loadedByte:" + loadedByte);
                    //notifyDataSetChanged();
                  }

                  @Override
                  public boolean OnCancel() {
                    return false;
                  }
                }).Download();
                mHandler.sendEmptyMessage(0);
                //キャッシュにロードする
                //ImageCacheDrawable.put(url, uploadImageDB.getDrawable(url, RoomActivity.this));
              } catch (FileNotFoundException e) {
                Logger.log(e.toString());
              }
            }
          });
        }
      }

      // ImageViewに画像ファイルを設定
      Drawable drawable1 = getItem(position);
      if (drawable1 == null) {
        String url = mTwimptLogData.postedImage.get(position).first;
        holder.mText.setText(mStringTextImageDictionary.get(url));
      } else {
        holder.mText.setText(null);
        //holder.mText.setVisibility(View.GONE);
      }
      holder.mImage.setImageDrawable(drawable1);
      // ImageViewを返す
      return v;
    }
  }

  /** リストアダプタ */
  private class TwimptRoomsAdapter extends BaseAdapter {
    /** ダウンロードした画像urlと画像 */
    private Map<String, Drawable> mUrlDrawableMap = new HashMap<String, Drawable>();
    private Dictionary<Integer, ImageAdapter> mImageAdaptersDictionary = new Hashtable<Integer, ImageAdapter>();

    @Override
    public boolean isEnabled(int position) {
      return true;
    }

    @Override
    public int getCount() {
      int size = mGlobals.twimptRooms.get(mNowHash).dataList.size();
      if (size == 0) {
        return 0;
      }
      return size + 1;
    }

    @Override
    public Object getItem(int position) {
      if (readHere == position) {
        return null;
      } else if (readHere < position) {
        return mGlobals.twimptRooms.get(mNowHash).dataList.get(position - 1);
      } else {
        return mGlobals.twimptRooms.get(mNowHash).dataList.get(position);
      }
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    private class ViewHolder {
      TextView text, name, roomName, time;
      ImageView icon;
      GridView mGridView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      TwimptLogData twimptLogData = (TwimptLogData) getItem(position);
      if (twimptLogData != null) {
        //ログの場合
        View v = convertView;
        // ビューホルダー
        final ViewHolder holder;
        // 無い場合だけ作る
        int id = v == null ? 0 : v.getId();
        if (v == null || id != R.layout.list_view) {
          // XMLからレイアウトを作る
          LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
          v = inflater.inflate(R.layout.list_view, null);
          // ビューホルダーを作る
          holder = new ViewHolder();
          // テキストビューを取り出す
          holder.text = (TextView) v.findViewById(R.id.text);
          //holder.text.setCompoundDrawablesRelative();
          // TextView に LinkMovementMethod を登録します
          //holder.text.setMovementMethod(movement_method);
          //http://www.globefish.jp/mt/2011/09/listview-textview-setmovementmethod.html
          //setMovementMethodの後にフォーカスをfalseにしないとlistviewのクリックに持ってかれる
          holder.text.setFocusable(false);
          holder.text.setOnTouchListener(new ViewGroup.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
              TextView textView = (TextView) view;
              //継承したLinkMovementMethod
              MutableLinkMovementMethod m = new MutableLinkMovementMethod();
              //MovementMethod m=LinkMovementMethod.getInstance();
              //リンクのチェックを行うため一時的にsetする
              textView.setMovementMethod(m);
              boolean mt = m.onTouchEvent(textView, (Spannable) textView.getText(), event);
              //チェックが終わったので解除する しないと親view(listview)に行けない
              textView.setMovementMethod(null);
              //setMovementMethodを呼ぶとフォーカスがtrueになるのでfalseにする
              textView.setFocusable(false);
              //戻り値がtrueの場合は今のviewで処理、falseの場合は親viewで処理
              return mt;
            }
          });
          holder.name = (TextView) v.findViewById(R.id.name);
          holder.roomName = (TextView) v.findViewById(R.id.room_name);
          holder.icon = (ImageView) v.findViewById(R.id.user_image);
          holder.time = (TextView) v.findViewById(R.id.time);
          holder.mGridView = (GridView) v.findViewById(R.id.gridImageView);
          holder.mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
              ImageAdapter imageAdapter = (ImageAdapter) parent.getAdapter();
              //drawableをbitmapに変換する
              ByteArrayOutputStream bos = new ByteArrayOutputStream();
              Drawable drawable = imageAdapter.getItem(position);
              if (drawable == null) return;
              Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
              //bitmapをbyte[]にする
              if(!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)){
                Toast.makeText(RoomActivity.this,"画像形式が不正です",Toast.LENGTH_LONG).show();
                return;
              }
              byte[] bitmapByte = bos.toByteArray();
              if(bitmapByte==null)return;
              Intent intent = new Intent(RoomActivity.this, ImageViewActivity.class);
              intent.putExtra(ImageViewActivity.INTENT_BITMAP_BYTE_ARRAY, bitmapByte);
              startActivity(intent);
            }
          });
          // ビューにホルダーを登録する
          v.setTag(holder);
          // もう作られているときはそっちから取り出す
        } else {
          // 登録されているモノを使う
          holder = (ViewHolder) v.getTag();
        }
        TextDecode(twimptLogData);
        ImageAdapter imageAdapter = null;
        if (twimptLogData.postedImage != null) {
          holder.mGridView.setVisibility(View.VISIBLE);
          imageAdapter = mImageAdaptersDictionary.get(position);
          if (imageAdapter == null) {
            imageAdapter = new ImageAdapter(twimptLogData);
            mImageAdaptersDictionary.put(position, imageAdapter);
          }
        } else {
          holder.mGridView.setVisibility(View.GONE);
        }
        holder.mGridView.setAdapter(imageAdapter);

        holder.text.setText(twimptLogData.decodedText);
        holder.text.setFocusable(false);
        holder.text.setClickable(false);
        holder.name.setText(twimptLogData.name);
        // ミリ秒で比較するので1000倍する
        String timeString = TimeDiff.toDiffDate(twimptLogData.time * 1000) + "前";
        holder.time.setText(timeString);
        Drawable icon = mUrlDrawableMap.get(twimptLogData.icon);
        if (icon != null) {
          holder.icon.setImageDrawable(icon);
        } else {
          // 画像読み込み依頼を投げる
          ImageGetWaitThread(userImageDB, "http://twimpt.com/icon/" + twimptLogData.icon, RoomActivity.this);
          Drawable drawable = userImageDB.getDrawable("http://twimpt.com/icon/" + twimptLogData.icon, RoomActivity.this);
          holder.icon.setImageDrawable(drawable);
          if (drawable != null)
            mUrlDrawableMap.put(twimptLogData.icon, drawable);
        }
        if (twimptLogData.roomHash == null) {//|| twimptLogData.hash.isEmpty()
          holder.roomName.setText(R.string.monologue_name);
        } else {
          holder.roomName.setText(mGlobals.twimptRooms.get(twimptLogData.roomHash).name);
        }
        return v;//通常のログデータのviewを返す
      } else {
        //「ここまで読んだ」タグの場合
        int id = convertView == null ? 0 : convertView.getId();
        if (convertView == null || id != R.layout.read_here) {
          LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
          convertView = inflater.inflate(R.layout.read_here, null);
        }
        return convertView;
      }
    }

    /**
     * TwimptLogDataのtextをデコードする
     * @param logData デコードするtwimptのデータ
     */
    public void TextDecode(TwimptLogData logData) {
      if (logData.decodedText != null) return;
//      logData.text="<a href=\"http://twimpt.com/upload/original/20150110/CGMIadXM.png\" data-lightbox=\"uploaded-image\"><img src=\"http://twimpt.com/upload/thumbnail/20150110/256/CGMIadXM.png\" class=\"imageThumbnail\" /></a>";
//      logData.text+="\n<a href=\"http://twimpt.com/upload/original/20150110/CGMIadXM.png\" data-lightbox=\"uploaded-image\"><img src=\"http://twimpt.com/upload/thumbnail/20150110/256/CGMIadXM.png\" class=\"imageThumbnail\" /></a>";

      //TODO デコードにraw_textを使った方がいいが全てのタグを変換するのが面倒なので今はtextの方を使う
      //imgタグのデコード
      String regex = "<a.+?href=\"(.*?)\".*?><img.+?src=\"(.*?)\".*?>";
      Pattern p = Pattern.compile(regex);
      Matcher m = p.matcher(logData.text);
      String text;
      if (m.find()) {
        logData.postedImage = new ArrayList<Pair<String, Drawable>>();
        do {
          logData.postedImage.add(new Pair<String, Drawable>(m.group(1), null));
          Logger.log(m.group(1));
        } while (m.find());
      }
      text = m.replaceAll("<a href=\"$1\">$1");

      logData.decodedText = Html.fromHtml(text);
    }

    public void ImageGetWaitThread(final ImageCacheDB database, final String url, final Context cxt) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          for (int i = 0; i < 10; i++) {
            Drawable drawable = database.getDrawable(url, cxt);
            if (drawable != null) {
              mHandler.post(new Runnable() {
                @Override
                public void run() {
                  //取得できた場合は画面更新のためリストビューのアダプターに通知
                  adapter.notifyDataSetChanged();
                }
              });
              break;
            }
            try {
              Thread.sleep(5000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      }).start();
    }


    /**
     * これしないとリリースしまくってエラーになる
     * Adapterには必要らしいandroid4.0.3のバグだとか
     */
    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
      if (observer != null)
        super.unregisterDataSetObserver(observer);
    }
  }
}
