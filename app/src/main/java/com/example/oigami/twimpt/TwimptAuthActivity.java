package com.example.oigami.twimpt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.oigami.twimpt.debug.Logger;
import com.example.oigami.twimpt.twimpt.TwimptNetwork;
import com.example.oigami.twimpt.twimpt.token.AccessTokenData;
import com.example.oigami.twimpt.twimpt.token.RequestTokenData;

import org.json.JSONObject;

import java.util.Arrays;

/**
 * Created by oigami on 2014/10/05
 */
public class TwimptAuthActivity extends ActionBarActivity {

  private boolean mNowUpdate = false;
  private boolean mustBootBrowser = false;
  private Handler mHandler = new Handler();
  private View button;
  private TextView text;
  private RequestTokenData requestTokenData;
  private AlertDialogFragment dlg;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.auth_activity);
    button = findViewById(R.id.auth_url_button);
    text = (TextView) findViewById(R.id.auth_log_edittext);
    RequestTokenData requestTokenData = TwimptToken.GetRequestToken(this);
    if (requestTokenData == null) {
      GetRequestToken();
    }

    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        GetRequestToken();
        v.setEnabled(false);
      }
    });
  }

  private void AppendLog(String string) {
    text.append(string);
  }

  private void AppendLog(int stringId) {
    AppendLog(getString(stringId));
  }

  private void AppendLogln(String string) {
    text.append(string + "\n");
  }

  private void AppendLogln(int stringId) {
    AppendLogln(getString(stringId));
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    assert false;
  }

  @Override
  public void onStart() {
    super.onStart();
    Intent intent = getIntent();
    if (Intent.ACTION_VIEW.equals(intent.getAction())) {//intentによって起動
      GetAccessToken();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (mustBootBrowser && dlg == null) {
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          AppendLog("\t...");
          AppendLog(R.string.is_stopped);
          dlg = AlertDialogFragment.CreateWarningDialog();
          dlg.show(getSupportFragmentManager(), "tag");
        }
      });
    }
  }

  @Override
  public void onPause() {
    super.onPause();
  }

  public static class AlertDialogFragment extends DialogFragment {
    public AlertDialogFragment() {
    }

    public static AlertDialogFragment CreateWarningDialog() {
      AlertDialogFragment dlg = new AlertDialogFragment();
      Bundle bundle = new Bundle();
      dlg.setArguments(bundle);
      return dlg;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      TwimptAuthActivity This = ((TwimptAuthActivity) getActivity());
      return WarningDialog(This);
    }

    private Dialog WarningDialog(final TwimptAuthActivity This) {
      final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(This)
              .setTitle(R.string.being_in_middle_of_browser_auth)
              .setMessage(R.string.want_to_continue_authentication);
      dlgBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          This.dlg = null;
          This.AppendLog("\t...");
          This.AppendLogln(R.string.resumption);
          This.GoAuthWebPage();
        }
      });
      dlgBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          This.dlg = null;
          This.AppendLog("\t...");
          This.AppendLogln(R.string.Stop);
          This.mustBootBrowser = false;
        }
      });
      final Dialog dlg = dlgBuilder.create();
      // ダイアログの外側をタッチしても閉じない様にする
      dlg.setCanceledOnTouchOutside(false);
      return dlg;
    }
  }

  public void GetRequestToken() {
    if (mNowUpdate) return;
    mNowUpdate = true;
    button.setEnabled(false);
    AppendLog(R.string.get_request_token);
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          final JSONObject json = TwimptNetwork.GetRequestToken(TwimptDeveloperData.API_KEY, TwimptDeveloperData.API_KEY_SECRET);
          Logger.log(json.toString(2));
          requestTokenData = new RequestTokenData(json);
          mHandler.post(new Runnable() {
            @Override
            public void run() {
              AppendLog("\t...");
              AppendLogln(R.string.success);
              button.setEnabled(true);
              TwimptToken.SetRequestToken(TwimptAuthActivity.this, requestTokenData);
              GoAuthWebPage();
            }
          });
        } catch (final Exception e) {
          e.printStackTrace();
          mHandler.post(new Runnable() {
            @Override
            public void run() {
              button.setEnabled(true);
              AppendLogln("\t..." + getString(R.string.Failure) + ":\n" + e.getMessage() + "\n\n" + Logger.getStackTraceString(e));
            }
          });
        }
        mNowUpdate = false;
      }
    }).start();
  }

  private void GetAccessToken() {
    if (mNowUpdate) return;
    mNowUpdate = true;
    button.setEnabled(false);
    AppendLog(R.string.get_access_token);
    new Thread(new Runnable() {
      @Override
      public void run() {
        RequestTokenData requestToken = TwimptToken.GetRequestToken(TwimptAuthActivity.this);
        try {
          final JSONObject json = TwimptNetwork.GetAccessToken(TwimptDeveloperData.API_KEY, TwimptDeveloperData.API_KEY_SECRET, requestToken.token, requestToken.secret);
          final AccessTokenData accessTokenData = new AccessTokenData(json);
          mHandler.post(new Runnable() {
            @Override
            public void run() {
              TwimptToken.SetAccessToken(TwimptAuthActivity.this, accessTokenData);
              button.setEnabled(true);
              AppendLog("\t...");
              AppendLogln(R.string.success);
              StartRoomActivity();
            }
          });
        } catch (final Exception e) {
          e.printStackTrace();
          mHandler.post(new Runnable() {
            @Override
            public void run() {
              button.setEnabled(true);
              AppendLogln("\t..." + getString(R.string.Failure) + ":\n" + e.getMessage() + "\n\n" + Arrays.toString(e.getStackTrace()));
            }
          });
        }
      }
    }).start();
  }

  private void GoAuthWebPage() {
    assert requestTokenData != null;
    AppendLog(R.string.move_in_auth_page);
    mustBootBrowser = true;
    Uri uri = Uri.parse(TwimptNetwork.GetAuthURL(TwimptDeveloperData.AUTH_ID, requestTokenData.token, requestTokenData.secret));
    Intent i = new Intent(Intent.ACTION_VIEW, uri);
    startActivity(i);
  }

  private void StartRoomActivity() {
    Toast.makeText(this, R.string.auth_success, Toast.LENGTH_LONG).show();
    Intent intent = new Intent(this, RoomActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
    finish();
  }
}
