package com.example.oigami.twimpt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.oigami.twimpt.twimpt.TwimptJson;
import com.example.oigami.twimpt.twimpt.TwimptNetwork;
import com.example.oigami.twimpt.twimpt.TwimptRoom;
import com.example.oigami.twimpt.twimpt.token.AccessTokenData;

import org.json.JSONObject;


/**
 * Created by oigami on 2014/10/01
 */
public class PostActivity extends ActionBarActivity {
  private enum DIALOG {
    MENU,
    LOTTERY,
  }

  static public String INTENT_POST_HASH = "POST_HASH";
  static public String INTENT_NOW_HASH = "NOW_HASH";
  private DataApplication globals;
  boolean mNowUpdate = false;
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
    Intent i = getIntent();
    mNowRoomHash = i.getStringExtra(INTENT_NOW_HASH);
    mPostRoomHash = i.getStringExtra(INTENT_POST_HASH);

    TwimptRoom twimptRoom = globals.twimptRooms.get(mPostRoomHash);
    getSupportActionBar().setTitle(twimptRoom.name);
    EditText mPostMessageEdit = (EditText) findViewById(R.id.post_message);

    //mPostMessageEdit.showContextMenu();
    registerForContextMenu(mPostMessageEdit);

    View button2 = findViewById(R.id.post_button);
    button2.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        PostRequest();

      }
    });
  }

  private enum CONTEXT_MENU {
    CANCEL,
    OMIKUJI,
    DEFAULT
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    // コンテキストメニューを作る
    menu.clear();
    menu.setHeaderTitle(null);
    menu.add(0, CONTEXT_MENU.OMIKUJI.ordinal(), 0, R.string.insert_lottery_command);
    //menu.add(0, CONTEXT_MENU.DEFAULT.ordinal(), 0, "テキストの選択");
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
        // unregisterForContextMenu(mPostMessageEdit);
        //mPostMessageEdit.selectAll();
        //mPostMessageEdit.performLongClick();
        //registerForContextMenu(mPostMessageEdit);
        return true;
      default:
        Toast.makeText(this, "エラー", Toast.LENGTH_LONG).show();
        return super.onContextItemSelected(item);
    }
  }

  void PostRequest() {
    if (mNowUpdate)
      return;
    mNowUpdate = true;
    final TwimptRoom nowTwimptRoom = globals.twimptRooms.get(mNowRoomHash);
    final TwimptRoom postTwimptRoom = globals.twimptRooms.get(mPostRoomHash);
    final TextView message = (TextView) findViewById(R.id.post_message);
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          AccessTokenData accessToken = TwimptToken.GetAccessToken(PostActivity.this);
          JSONObject json = TwimptNetwork.PostRequest(accessToken.token, accessToken.secret,
                  postTwimptRoom.type, postTwimptRoom.hash, message.getText().toString(),
                  nowTwimptRoom.type, nowTwimptRoom.hash, nowTwimptRoom.getLatestLogHash(), nowTwimptRoom.LatestModifyHash);
          final TwimptJson.ParsedData parsedData = TwimptJson.UpdateParse(json);
          mHandler.post(new Runnable() {
            @Override
            public void run() {
              RoomActivity.PushData(PostActivity.this, globals.twimptRooms, nowTwimptRoom, parsedData, true);
            }
          });
        } catch (Exception e) {
          e.printStackTrace();
        }
        mHandler.sendEmptyMessage(0);
      }
    }).start();
  }


  /**
   * アラートダイアログ
   * setArguments()で"id"を使い表示するダイアログを制御
   */
  public static class AlertDialogFragment extends DialogFragment {
    public AlertDialogFragment() { }

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
      final String checkboxStr[] = {"omikuji", "meshi", "seiyu", "precure", "pokemon", "lovelive"};
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
        public void onClick(DialogInterface dialog, int which) { }
      });

      final Dialog dlg = dlgBuilder.create();
      // ダイアログの外側をタッチしても閉じない様にする
      dlg.setCanceledOnTouchOutside(false);
      return dlg;
    }
  }
}
