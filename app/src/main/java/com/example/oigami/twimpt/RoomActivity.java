package com.example.oigami.twimpt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.Spannable;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.example.oigami.twimpt.image.ImageCacheDB;
import com.example.oigami.twimpt.image.MultiThreadImageDownloader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
  private Button list_end_button;
  /**
   * ここまで読んだタグの位置
   */
  private int readHere = 0;
  //private String accessToken, accessTokenSecret;
  Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      RefreshEnd();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_my);

    mGlobals = (DataApplication) this.getApplication();
    // ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します

    Intent i = getIntent();
    mNowHash = i.getStringExtra(INTENT_ROOM_NAME_HASH);

    if (mNowHash == null) {
      mNowHash = "public";
    }
    getSupportActionBar().setTitle(mGlobals.twimptRooms.get(mNowHash).name);

    adapter.notifyDataSetChanged();
    mSwipeRefreshWidget = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_widget);
    //mSwipeRefreshWidget.setColorScheme(0xff66cdaa, 0xffff89c4, 0xffff89c4, 0xffff89c4);
    mListView = (ListView) findViewById(R.id.content);
    list_end_button = new Button(this);
    list_end_button.setText(R.string.load_log);
    list_end_button.setVisibility(View.GONE);
    list_end_button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        LogRequest();
      }
    });
    mListView.addFooterView(list_end_button);
    mListView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        openContextMenu(view);
      }
    });
    mListView.setAdapter(adapter);
    //mSwipeRefreshWidget.setColorScheme(
    /*
     mSwipeRefreshWidget.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light);*/

    mSwipeRefreshWidget.setColorSchemeResources(
            R.color.blue_bright, R.color.green_light, R.color.orange_light, R.color.red_light);
    // 1. スワイプ時のリスナを登録する（implements OnRefreshListener）
    mSwipeRefreshWidget.setOnRefreshListener(new OnRefreshListener() {
      @Override
      public void onRefresh() {
        UpdateRequest();
      }
    });
    mSwipeRefreshWidget.setRefreshing(true);

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
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    // コンテキストメニューを作る
    AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) menuInfo;
    ListView listView = (ListView) v;
    if (adapterInfo.position == readHere) {//「選んだものば「ここまで読んだ」タグの場合は何もしない
      return;
    }
    TwimptLogData twimptLogData = (TwimptLogData) listView.getItemAtPosition(adapterInfo.position);
    //TwimptLogData twimptLogData = mGlobals.twimptRooms.get(mGlobals.mNowHash).dataList.get(mListViewClickedNum);
    menu.setHeaderTitle(twimptLogData.text);
    //TODO 現状APIがないのでコメントアウト
    //menu.add(0, CONTEXT_MENU.COMMENT.ordinal(), 0, "書き込みにコメントする");
    //String roomName = twimptLogData.roomHash != null ? mGlobals.twimptRooms.get(twimptLogData.roomHash).name : "ひとりごと";
    if (mNowHash.equals("public"))
      menu.add(0, CONTEXT_MENU.OPEN.ordinal(), 0,R.string.open_room);
    menu.add(0, CONTEXT_MENU.WRITE_ROOM.ordinal(), 0, R.string.write_room);
    menu.add(0, CONTEXT_MENU.CANCEL.ordinal(), 0,R.string.cancel);
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
   * @param parsedData ログのjsonがパースされたデータ
   * @param top        ログの挿入する位置 trueは上 falseは下 に挿入する
   */
  public static void PushData(Context content, Map<String, TwimptRoom> twimptRooms, TwimptRoom twimptRoom,
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
    MultiThreadImageDownloader.execute(content, icon);
  }

  private void StartPostActivity(String postRoomHash, String nowRoomHash) {
    SharedPreferences sharedPref = getSharedPreferences("token", MODE_PRIVATE);
    String accessToken = sharedPref.getString("access_token", "");
    String accessTokenSecret = sharedPref.getString("access_token_secret", "");
    if (accessTokenSecret == null || accessTokenSecret.equals("") || accessToken == null || accessTokenSecret.equals("")) {
      Toast.makeText(this, R.string.encourage_auth, Toast.LENGTH_LONG).show();
    } else {
      Intent intent = new Intent(this, PostActivity.class);
      intent.putExtra(PostActivity.INTENT_NOW_HASH, nowRoomHash);
      intent.putExtra(PostActivity.INTENT_POST_HASH, postRoomHash);
      //intent.setFlags(FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
      startActivity(intent);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.my, menu);
    //TODO 途中で認証した時の切り替えを考えるまで保留
//    if (isAuth()) {
//      //認証している場合は認証メニューを削除
//      menu.removeItem(R.id.action_auth);
//    } else {
//    //認証していない場合は認証解除メニューを削除
//    menu.removeItem(R.id.action_deauthentication);
//  }
    return true;
  }

  public boolean isAuth() {
    SharedPreferences sharedPref;
    String accessToken, accessTokenSecret;
    sharedPref = getSharedPreferences("token", MODE_PRIVATE);
    accessToken = sharedPref.getString("access_token", "");
    accessTokenSecret = sharedPref.getString("access_token_secret", "");
    return isAuth(accessToken, accessTokenSecret);
  }

  public boolean isAuth(String accessToken, String accessTokenSecret) {
    return !accessToken.equals("") && !accessTokenSecret.equals("");
  }

  /*  menuが押されたときに呼ばれる  */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    SharedPreferences sharedPref;
    String accessToken, accessTokenSecret;
    AlertDialogFragment dlg;
    switch (id) {
      case R.id.post_request:
        if (mNowHash.equals("public")) {
          //警告ダイアログ表示
          dlg = new AlertDialogFragment();
          Bundle bundle = new Bundle();
          bundle.putInt("id", DIALOG.POST_ALERT.ordinal());
          dlg.setArguments(bundle);
          dlg.show(getSupportFragmentManager(), "tag");
        } else {
          StartPostActivity(mNowHash, mNowHash);
        }
        break;
      case R.id.action_auth://認証activityを開く
        sharedPref = getSharedPreferences("token", MODE_PRIVATE);
        accessToken = sharedPref.getString("access_token", "");
        accessTokenSecret = sharedPref.getString("access_token_secret", "");
        if (!isAuth(accessToken, accessTokenSecret)) {
          Intent intent = new Intent(RoomActivity.this, TwimptAuthActivity.class);
          startActivity(intent);
        } else {
          Toast.makeText(this, R.string.authenticated, Toast.LENGTH_LONG).show();
        }
        break;
      case R.id.action_deauthentication: //認証を解除
        sharedPref = getSharedPreferences("token", MODE_PRIVATE);
        accessToken = sharedPref.getString("access_token", "");
        accessTokenSecret = sharedPref.getString("access_token_secret", "");
        if (isAuth(accessToken, accessTokenSecret)) {
          Editor e = sharedPref.edit();
          e.clear();
          e.commit();
          Toast.makeText(this, R.string.authenticated_release, Toast.LENGTH_LONG).show();
        } else {
          Toast.makeText(this,R.string.not_authenticated, Toast.LENGTH_LONG).show();
        }
        break;
      case R.id.open_official_now_page: //公式ページをブラウザで開く
        Uri uri;
        if (mNowHash.equals("public") || mNowHash.equals("monologue")) {
          uri = Uri.parse("http://twist.twimpt.com/" + mNowHash);
        } else {
          uri = Uri.parse("http://twist.twimpt.com/" + mGlobals.twimptRooms.get(mNowHash).type + "/" + mGlobals.twimptRooms.get(mNowHash).id);
        }
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
    /*if (id == R.id.action_settings) {
      SharedPreferences sharedPref;
      sharedPref = getSharedPreferences("token", MODE_PRIVATE);
      Editor e = sharedPref.edit();
      e.putString("request_token", "value");
      e.putString("request_token_secret", "value");
      e.putString("access_token", "value");
      e.putString("access_token_secret", "value");
      e.commit();

      return true;
    }*/
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
  }

  private void UpdateRequest() {
    if (RefreshStart()) {
      if (TwimptAsyncTask.isConnected(this)) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              final Map<String, TwimptRoom> twimptRooms = mGlobals.twimptRooms;
              final TwimptRoom twimptRoom = twimptRooms.get(mNowHash);
              String latestHash = twimptRoom.getLatestLogHash();
              final JSONObject json =
                      Twimpt.UpdateRequest(twimptRoom.type, twimptRoom.hash, latestHash, twimptRoom.LatestModifyHash);
              final TwimptJson.ParsedData parsedData = TwimptJson.UpdateParse(json);
              mHandler.post(new Runnable() {
                @Override
                public void run() {
                  readHere = parsedData.mTwimptLogData.length;
                  PushData(RoomActivity.this, twimptRooms, twimptRoom, parsedData, true);
                  RefreshEnd();

                  if (readHere > 0 && twimptRoom.dataList.size() > 20)
                    mListView.setSelectionFromTop(readHere - 1, 10);
                }
              });
            } catch (JSONException e) {
              e.printStackTrace();
              mHandler.sendEmptyMessage(0);
            } catch (IOException e) {
              e.printStackTrace();
              mHandler.sendEmptyMessage(0);
            }
          }
        }).start();
      } else {
        Toast.makeText(RoomActivity.this, R.string.update_error, Toast.LENGTH_LONG).show();
        RefreshEnd();
      }
    } else {
      Toast.makeText(RoomActivity.this, R.string.updating, Toast.LENGTH_SHORT).show();
    }
  }

  private void LogRequest() {
    //final String mNowHash = mNowHash;
    if (RefreshStart()) {
      if (TwimptAsyncTask.isConnected(RoomActivity.this)) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              final Map<String, TwimptRoom> twimptRooms = mGlobals.twimptRooms;
              final TwimptRoom twimptRoom = twimptRooms.get(mNowHash);
              final JSONObject json = Twimpt.LogRequest(twimptRoom.type, twimptRoom.hash, twimptRoom.getOldestLogHash());
              final TwimptJson.ParsedData parsedData = TwimptJson.LogParse(json);
              mHandler.post(new Runnable() {
                @Override
                public void run() {
                  PushData(RoomActivity.this, twimptRooms, twimptRoom, parsedData, false);
                  RefreshEnd();
                }
              });
            } catch (JSONException e) {
              e.printStackTrace();
              mHandler.sendEmptyMessage(0);
            } catch (IOException e) {
              e.printStackTrace();
              mHandler.sendEmptyMessage(0);
            }
          }
        }).start();
      } else {
        Toast.makeText(RoomActivity.this, R.string.update_error, Toast.LENGTH_LONG).show();
        RefreshEnd();
      }
    } else {
      Toast.makeText(RoomActivity.this, R.string.updating, Toast.LENGTH_SHORT).show();
    }
  }

  private boolean RefreshStart() {
    if (mNowUpdate) {//すでにアップデート中の場合
      return false;
    }
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
    if (adapter.getCount() > 0) {
      list_end_button.setVisibility(View.VISIBLE);
    }
  }

  /**
   * アラートダイアログ
   */
  public static class AlertDialogFragment extends DialogFragment {
    public AlertDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      Dialog dialog;
      AlertDialog.Builder builder;
      final CharSequence items[] = {"red", "Orange", "Yellow", "Blue", "Indigo", "Violet"};
      boolean flags[] = {true, false, true, false, true, false, true};
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
            public void onClick(DialogInterface dialog, int which) {
            }
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
   * リストアダプタ
   */
  private class TwimptRoomsAdapter extends BaseAdapter {
    private Map<String, Drawable> mUrlDrawableMap = new HashMap<String, Drawable>();

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

    class ViewHolder {
      TextView text, name, roomName, time;
      ImageView mImageView;
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
        if (v == null || (int) v.getId() != R.layout.list_view) {
          // XMLからレイアウトを作る
          LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
          v = inflater.inflate(R.layout.list_view, null);
          // ビューホルダーを作る
          holder = new ViewHolder();
          // テキストビューを取り出す
          holder.text = (TextView) v.findViewById(R.id.text);
          // TextView に LinkMovementMethod を登録します
          //holder.text.setMovementMethod(movement_method);
          //http://www.globefish.jp/mt/2011/09/listview-textview-setmovementmethod.html
          //setMovementMethodの後にフォーカスをfalseにしないとlistviewのクリックも持ってかれる
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
          holder.mImageView = (ImageView) v.findViewById(R.id.user_image);
          holder.time = (TextView) v.findViewById(R.id.time);
          // ビューにホルダーを登録する
          v.setTag(holder);
          // もう作られているときはそっちから取り出す
        } else {
          // 登録されているモノを使う
          holder = (ViewHolder) v.getTag();
        }
      /*if (v == null) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.list_view, null);
      }*/
        if (twimptLogData.decodedText == null) {
          twimptLogData.decodedText = Html.fromHtml(twimptLogData.text);
        }

        holder.text.setText(twimptLogData.decodedText);
        holder.text.setFocusable(false);
        holder.text.setClickable(false);
        holder.name.setText(twimptLogData.name);
        String timeString = TimeDiff.toDiffDate(twimptLogData.time * 1000) + "前";
        holder.time.setText(timeString);
        Drawable icon = mUrlDrawableMap.get(twimptLogData.icon);
        if (icon != null) {
          holder.mImageView.setImageDrawable(icon);
        } else {
          // 画像読み込み依頼を投げる
          ImageGetWaitThread("http://twimpt.com/icon/" + twimptLogData.icon, RoomActivity.this);
          Drawable drawable = getDrawable("http://twimpt.com/icon/" + twimptLogData.icon, RoomActivity.this);
          holder.mImageView.setImageDrawable(drawable);
          if (drawable != null) {
            mUrlDrawableMap.put(twimptLogData.icon, drawable);
          }
        }
        if (twimptLogData.roomHash == null) {//|| twimptLogData.hash.isEmpty()
          holder.roomName.setText("ひとりごと");
        } else {
          holder.roomName.setText(mGlobals.twimptRooms.get(twimptLogData.roomHash).name);
        }
        return v;//通常のログデータのviewを返す
      } else {
        //「ここまで読んだ」タグの場合
        if (convertView == null || (int) convertView.getId() != R.layout.read_here) {
          LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
          convertView = inflater.inflate(R.layout.read_here, null);
        }
        return convertView;
      }
    }

    /*  画像をhttpで取得する
        とりえあえず１０回取得できるまで回している    */
    public void ImageGetWaitThread(final String url, final Context cxt) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          for (int i = 0; i < 10; i++) {
            Drawable drawable = getDrawable(url, cxt);
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

    /*  画像をデータベース取得する            */
    public Drawable getDrawable(String url, Context cxt) {
      ImageCacheDB db = ImageCacheDB.getInstance(cxt);
      final Cursor c = db.existsFile(url);
      if (c.moveToFirst()) {
        final String filename = c.getString(c.getColumnIndex(ImageCacheDB.CacheColumn.NAME));
        final String type = c.getString(c.getColumnIndex(ImageCacheDB.CacheColumn.TYPE));
        if (type.equals("image/jpg") || type.equals("image/jpeg") || type.equals("image/png") || type.equals("image/gif")) {
          return Drawable.createFromPath(cxt.getFileStreamPath(filename).getAbsolutePath());
          //setImageDrawable(drawable);
          //setVisibility(RemoteImageView.VISIBLE);
        }
      }
      return null;
    }

    /**
     * これしないとリリースしまくってエラーになる
     * Adapterには必要らしいandroid4.0.3のバグだとか
     */
    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
      if (observer != null) {
        super.unregisterDataSetObserver(observer);
      }
    }
  }
}
