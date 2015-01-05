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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


/**
 * Created by oigami on 2014/10/01.
 */
public class PostActivity extends ActionBarActivity {
  private enum DIALOG {
    MENU,
    LOTTERY,
  }

  static public String INTENT_POST_HASH = "POST_HASH";
  static public String INTENT_NOW_HASH = "NOW_HASH";
  DataApplication globals;
  boolean mNowUpdate = false;
  private String mNowRoomHash, mPostRoomHash;
  private TwimptRoom mTwimptRoom;
  EditText mPostMessageEdit;
  private Handler mHandler = new Handler() {
    //メッセージ受信
    public void handleMessage(Message message) {
      mNowUpdate = false;
      finish();
      //メッセージの表示
      //textView.setText((String) message.obj);//
      //adapter.notifyDataSetChanged();
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

    mTwimptRoom = globals.twimptRooms.get(mPostRoomHash);
    getSupportActionBar().setTitle(mTwimptRoom.name);

    mPostMessageEdit = (EditText) findViewById(R.id.post_message);
    //
    mPostMessageEdit.showContextMenu();
    registerForContextMenu(mPostMessageEdit);
//    mPostMessageEdit.setOnLongClickListener(new View.OnLongClickListener() {
//      @Override
//      public boolean onLongClick(View view) {
//          openContextMenu(view);
//        return true;
//      }
//    });
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
    //AdapterView.AdapterContextMenuInfo adapterInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
    //TwimptLogData twimptLogData = mGlobals.twimptRooms.get(mGlobals.mNowRoomHash).dataList.get(mListViewClickedNum);
    menu.clear();

    menu.setHeaderTitle(null);
    menu.add(0, CONTEXT_MENU.OMIKUJI.ordinal(), 0, R.string.insert_lottery_command);
    //menu.add(0, CONTEXT_MENU.DEFAULT.ordinal(), 0, "テキストの選択");
    menu.add(0, CONTEXT_MENU.CANCEL.ordinal(), 0, R.string.cancel);

    //String roomName = twimptLogData.roomHash != null ? mGlobals.twimptRooms.get(twimptLogData.roomHash).name : "ひとりごと";

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
        //Intent intent = new Intent(this, LotterySettingActivity.class);
        //startActivity(intent);
        dlg = new AlertDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("id", DIALOG.LOTTERY.ordinal());
        dlg.setArguments(bundle);
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
    if (mNowUpdate == false) {
      mNowUpdate = true;
      final TwimptRoom nowTwimptRoom = globals.twimptRooms.get(mNowRoomHash);
      final TwimptRoom postTwimptRoom = globals.twimptRooms.get(mPostRoomHash);
      final TextView message = (TextView) findViewById(R.id.post_message);
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            SharedPreferences sharedPref;
            sharedPref = getSharedPreferences("token", MODE_PRIVATE);
            String accessToken = sharedPref.getString("access_token", "");
            String accessTokenSecret = sharedPref.getString("access_token_secret", "");
            JSONObject json = Twimpt.PostRequest(accessToken, accessTokenSecret,
                    postTwimptRoom.type, postTwimptRoom.hash, message.getText().toString(),
                    nowTwimptRoom.type, nowTwimptRoom.hash, nowTwimptRoom.getLatestLogHash(), nowTwimptRoom.LatestModifyHash);
            final TwimptJson.ParsedData parsedData = TwimptJson.UpdateParse(json);
            mHandler.post(new Runnable() {
              @Override
              public void run() {
                RoomActivity.PushData(PostActivity.this, globals.twimptRooms, nowTwimptRoom, parsedData, true);
              }
            });
          } catch (JSONException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          }
          mHandler.sendEmptyMessage(0);
        }
      }).start();
    }
  }

  /**
   * ポップアップダイアログを表示する
   */
  public Dialog PopupDialog() {
    // ダイアログを作る
    //LayoutInflater inflate = LayoutInflater.from(this);
    LayoutInflater inflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    //inflate.inflate(R.layout.lottery_setting_popup, null);
    final View view = inflate.inflate(R.layout.lottery_setting_popup, null);
    final SharedPreferences sharedPref = getSharedPreferences("lottery", MODE_PRIVATE);
    final String checkbox_str[] = {"omikuji", "meshi", "seiyu", "precure", "pokemon", "lovelive"};
    final CheckBox checkBox[] = new CheckBox[checkbox_str.length];
    for (int i = 0; i < checkbox_str.length; ++i) {
      int viewId = getResources().getIdentifier(checkbox_str[i], "id", getPackageName());
      checkBox[i] = (CheckBox) view.findViewById(viewId);
      checkBox[i].setChecked(sharedPref.getBoolean(checkbox_str[i], true));
    }
    // アラートダイアログを作る
    final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this).setTitle("くじ引きコマンドの挿入").setView(view);
    //OKボタン押下後の処理を記述
    dlgBuilder.setPositiveButton(R.string.insert, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        //dlg.dismiss();
        String replaceText = new String();
        SharedPreferences.Editor e = sharedPref.edit();
        for (int i = 0; i < checkbox_str.length; ++i) {
          //int viewId = getResources().getIdentifier(checkbox_str[i], "id", getPackageName());
          //CheckBox checkbox = (CheckBox) view.findViewById(viewId);
          e.putBoolean(checkbox_str[i], checkBox[i].isChecked());
          if (checkBox[i].isChecked()) {
            replaceText += '!' + checkbox_str[i];
          }
        }
        e.commit();
        EditText editText = (EditText) findViewById(R.id.post_message);
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
    /*String ret_command=new String();
    for(int i=0; i<checkBox.length; ++i) {
      if (checkBox[i].isChecked()) {
        ret_command += '!' + checkbox_str[i];
      }
    }
    return ret_command;*/
  }

  /**
   * アラートダイアログ
   * setArguments()で"id"を使い表示するダイアログを制御
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
        case LOTTERY:
          PostActivity This = ((PostActivity) getActivity());
          dialog = This.PopupDialog();
          break;
        default:
          dialog = null;
      }
      return dialog;
    }
  }
}
