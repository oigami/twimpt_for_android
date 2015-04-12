package com.example.oigami.twimpt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.example.oigami.twimpt.debug.Logger;
import com.example.oigami.twimpt.twimpt.ParsedData;
import com.example.oigami.twimpt.twimpt.TwimptNetwork;
import com.example.oigami.twimpt.twimpt.room.TwimptRoom;
import com.example.oigami.twimpt.twimpt.token.AccessTokenData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by oigami on 2014/10/01
 */
public class PostActivity extends ActionBarActivity {
  private enum DIALOG {
    MENU,
    LOTTERY,
  }

  public static final String INTENT_POST_HASH = "POST_HASH";
  public static final String INTENT_NOW_HASH = "NOW_HASH";
  private DataApplication globals;
  boolean mNowUpdate = false;
  EditText mPostMessageEdit;
  private String mNowRoomHash, mPostRoomHash;
  private Handler mHandler = new Handler() {
    //メッセージ受信
    public void handleMessage(Message message) {
      mNowUpdate = false;
      finish();
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.post_activity);
    globals = (DataApplication) this.getApplication();

    mPostMessageEdit = (EditText) findViewById(R.id.post_message);
    //mPostMessageEdit.showContextMenu();
    registerForContextMenu(mPostMessageEdit);

    View button2 = findViewById(R.id.post_button);
    button2.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        PostRequest();

      }
    });
    if (savedInstanceState != null) {
      mNowRoomHash = savedInstanceState.getString(INTENT_NOW_HASH);
      mPostRoomHash = savedInstanceState.getString(INTENT_POST_HASH);
    } else {
      Intent i = getIntent();
      mNowRoomHash = i.getStringExtra(INTENT_NOW_HASH);
      mPostRoomHash = i.getStringExtra(INTENT_POST_HASH);
    }
    TwimptRoom twimptRoom = globals.twimptRooms.get(mPostRoomHash);
    getSupportActionBar().setTitle(twimptRoom.name);

  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(INTENT_NOW_HASH, mNowRoomHash);
    outState.putString(INTENT_POST_HASH, mPostRoomHash);
  }

  private static final int REQUEST_GALLERY = 0;

  private enum CONTEXT_MENU {
    CANCEL,
    OMIKUJI,
    DEFAULT,
    IMAGE_UPLOAD,
    IMAGE_PASTE,
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    // コンテキストメニューを作る
    //menu.clear();
    menu.setHeaderTitle(null);
    menu.add(0, CONTEXT_MENU.OMIKUJI.ordinal(), 0, R.string.insert_lottery_command);
    // menu.add(0, CONTEXT_MENU.DEFAULT.ordinal(), 0, "テキストの選択");
    menu.add(0, CONTEXT_MENU.IMAGE_UPLOAD.ordinal(), 0, R.string.image_upload);
    menu.add(0, CONTEXT_MENU.IMAGE_PASTE.ordinal(), 0, R.string.image_paste);
    menu.add(0, CONTEXT_MENU.CANCEL.ordinal(), 0, R.string.cancel);
    super.onCreateContextMenu(menu, v, menuInfo);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AlertDialogFragment dlg;
    CONTEXT_MENU menu = CONTEXT_MENU.values()[item.getItemId()];
    switch (menu) {
    case CANCEL:
      return super.onContextItemSelected(item);
    case OMIKUJI:
      dlg = AlertDialogFragment.CreateLotteryDialog();
      dlg.show(getSupportFragmentManager(), "tag");
      return true;
    case DEFAULT:
      //TODO 仕様が微妙なので処理方法を考える
      //mPostMessageEdit.startActionMode(mPostMessageEdit.getCustomSelectionActionModeCallback());
      // unregisterForContextMenu(mPostMessageEdit);
      //mPostMessageEdit.selectAll();
      //mPostMessageEdit.performLongClick();
      //registerForContextMenu(mPostMessageEdit);
      return true;
    case IMAGE_PASTE:

      return true;
    case IMAGE_UPLOAD:
      Intent intent = new Intent();
      intent.setType("image/*");
      intent.setAction(Intent.ACTION_GET_CONTENT);
      startActivityForResult(intent, REQUEST_GALLERY);
      return true;
    default:
      Toast.makeText(this, "エラー", Toast.LENGTH_LONG).show();
      return super.onContextItemSelected(item);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != REQUEST_GALLERY || resultCode != RESULT_OK) return;
    try {
      Uri uri = data.getData();
      File file = new File(uri.getPath());
      int length = (int) file.length();
      InputStream in = getContentResolver().openInputStream(data.getData());
      byte[] buf = new byte[length];
      in.read(buf);
      in.close();
      ImageUpload(buf);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void ImageUpload(final byte[] imageBuf) {
    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    Intent intent = new Intent(this, PostActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
    final NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
    builder.setContentIntent(pendingIntent);
    builder.setWhen(System.currentTimeMillis());
    builder.setSmallIcon(R.drawable.ic_launcher);
    builder.setProgress(100, 0, false);
    // ステータスバーに表示されるテキスト
    builder.setTicker("Ticker");
    // Notificationを開いたときに表示されるタイトル
    builder.setContentTitle("ContentTitle");
    // Notificationを開いたときに表示されるサブタイトル
    builder.setContentText("ContentText");
    final Notification notification = builder.build();
    manager.notify(0, notification);
    if (true) return;
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          JSONObject json = TwimptNetwork.ImageUploadRequest(imageBuf);
          Logger.log(json.toString(2));
        } catch (IOException e) {
          e.printStackTrace();
        } catch (JSONException e) {
          e.printStackTrace();
        }

      }
    }).start();
  }

  void PostRequest() {
    if (mNowUpdate)
      return;
    mNowUpdate = true;
    final ConcurrentHashMap<String, TwimptRoom> twimptRooms = globals.twimptRooms;
    final TwimptRoom nowTwimptRoom = twimptRooms.get(mNowRoomHash);
    final TwimptRoom postTwimptRoom = twimptRooms.get(mPostRoomHash);
    final TextView message = (TextView) findViewById(R.id.post_message);
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          AccessTokenData accessToken = TwimptToken.GetAccessToken(PostActivity.this);
          JSONObject json = TwimptNetwork.PostRequest(accessToken.token, accessToken.secret,
                                                      postTwimptRoom.type, postTwimptRoom.hash, message.getText().toString(),
                                                      nowTwimptRoom.type, nowTwimptRoom.hash, nowTwimptRoom.getLatestLogHash(),
                                                      nowTwimptRoom.LatestModifyHash);
          final ParsedData parsedData = new ParsedData(json, twimptRooms);
          mHandler.post(new Runnable() {
            @Override
            public void run() {
              RoomActivity.PushUpdateData(globals.twimptRooms, nowTwimptRoom, parsedData);
              mNowUpdate = false;
              finish();
            }
          });
        } catch (Exception e) {
          e.printStackTrace();
          mHandler.sendEmptyMessage(0);
        }
      }
    }).start();
  }


  /**
   * アラートダイアログ
   * setArguments()で"id"を使い表示するダイアログを制御
   */
  public static class AlertDialogFragment extends DialogFragment {
    public AlertDialogFragment() {
    }

    /*くじ引き挿入ダイアログの作成 */
    public static AlertDialogFragment CreateLotteryDialog() {
      AlertDialogFragment dlg = new AlertDialogFragment();
      Bundle bundle = new Bundle();
      bundle.putInt("id", DIALOG.LOTTERY.ordinal());
      dlg.setArguments(bundle);
      return dlg;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      Dialog dialog = null;
      DIALOG[] values = DIALOG.values();
      DIALOG dialogID = values[getArguments().getInt("id")];
      switch (dialogID) {
      case LOTTERY:
        PostActivity This = ((PostActivity) getActivity());
        dialog = LotteryDialog(This);
        break;
      }
      return dialog;
    }

    /**
     * ポップアップダイアログを表示する
     */
    private Dialog LotteryDialog(final PostActivity This) {
      // ダイアログを作る
      //LayoutInflater inflate = LayoutInflater.from(this);
      LayoutInflater inflate = (LayoutInflater) This.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      //inflate.inflate(R.layout.lottery_setting_popup, null);
      final View view = inflate.inflate(R.layout.lottery_setting_popup, null);
      final SharedPreferences sharedPref = This.getSharedPreferences("lottery", MODE_PRIVATE);
      final String checkboxStr[] = {"omikuji", "meshi", "seiyu", "precure", "pokemon", "lovelive", "monhan", "imas", "aikatsu"};
      final CheckBox checkBox[] = new CheckBox[checkboxStr.length];
      for (int i = 0; i < checkboxStr.length; ++i) {
        int viewId = getResources().getIdentifier(checkboxStr[i], "id", This.getPackageName());
        checkBox[i] = (CheckBox) view.findViewById(viewId);
        checkBox[i].setChecked(sharedPref.getBoolean(checkboxStr[i], true));
      }
      // アラートダイアログを作る
      final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(This).setTitle("くじ引きコマンドの挿入").setView(view);
      //OKボタン押下後の処理を記述
      dlgBuilder.setPositiveButton(R.string.insert, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          String replaceText = "";
          SharedPreferences.Editor e = sharedPref.edit();
          for (int i = 0; i < checkboxStr.length; ++i) {
            e.putBoolean(checkboxStr[i], checkBox[i].isChecked());
            if (checkBox[i].isChecked())
              replaceText += '!' + checkboxStr[i];
          }
          e.commit();
          EditText editText = (EditText) This.findViewById(R.id.post_message);
          int start = editText.getSelectionStart();
          int end = editText.getSelectionEnd();
          Editable editable = editText.getText();
          editable.replace(Math.min(start, end), Math.max(start, end), replaceText);
        }
      });
      //OKボタン押下後の処理を記述
      dlgBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
        }
      });

      final Dialog dlg = dlgBuilder.create();
      // ダイアログの外側をタッチしても閉じない様にする
      dlg.setCanceledOnTouchOutside(false);
      return dlg;
    }
  }
}
