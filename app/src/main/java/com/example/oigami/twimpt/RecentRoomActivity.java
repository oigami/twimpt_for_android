package com.example.oigami.twimpt;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.oigami.twimpt.twimpt.TwimptJson;
import com.example.oigami.twimpt.twimpt.TwimptNetwork;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by oigami on 2014/10/03
 */
public class RecentRoomActivity extends ActionBarActivity {
  static public String INTENT_ROOM_RECENT_NAME = "RECENT_ROOM_NAME";
  DataApplication globals;
  int mNowPage = 1;
  String mRecentUrl;
  boolean mNowUpdate = false;
  ArrayList<String> mArrayListHash = new ArrayList<String>();
  private RecentRoomsListAdapter mAdapter = new RecentRoomsListAdapter();
  Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      mNowPage++;
      mNowUpdate = false;
      mAdapter.notifyDataSetChanged();
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.recent_room_activity);

    globals = (DataApplication) this.getApplication();

    final Intent intent = getIntent();
    String recentName = intent.getStringExtra(RecentRoomActivity.INTENT_ROOM_RECENT_NAME);

    getSupportActionBar().setTitle(recentName);

    Resources res = getResources();

    if (recentName.equals(res.getString(R.string.recent_created_name))) {
      mRecentUrl = "created";
    } else if (recentName.equals(res.getString(R.string.recent_posted_name))) {
      mRecentUrl = "posted";
    } else if (recentName.equals(res.getString(R.string.recent_opened_name))) {
      mRecentUrl = "opened";
    }
    ListView listView = (ListView) findViewById(R.id.recent_room_list);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent1 = new Intent(RecentRoomActivity.this, RoomActivity.class);
        intent1.putExtra(RoomActivity.INTENT_ROOM_NAME_HASH, mArrayListHash.get(i));
        intent1.putExtra(RoomActivity.INTENT_NAME_TYPE, "room");

        startActivity(intent1);
      }
    });
    listView.setAdapter(mAdapter);

    //new TwimptAsyncTask(globals.twimptRooms.get(globals.now_room_hash))
    GetRecentRoomData();
  }

  public void GetRecentRoomData() {
    if (!mNowUpdate) return;
    mNowUpdate = true;
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          final JSONObject json = TwimptNetwork.GetRecentRoom(mRecentUrl, mNowPage);
          if (mNowPage == 1)
            mArrayListHash.clear();
          TwimptJson.RecentRoomListParse(globals.twimptRooms, json, mArrayListHash);
        } catch (IOException e) {
          e.printStackTrace();
        } catch (JSONException e) {
          e.printStackTrace();
        }
        mHandler.sendEmptyMessage(0);
      }
    }).start();
  }

  private class RecentRoomsListAdapter extends BaseAdapter {
    @Override
    public boolean isEnabled(int position) {
      return true;
    }

    @Override
    public int getCount() {
      return mArrayListHash.size();
    }

    public String getItem(int position) {
      return mArrayListHash.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    class ViewHolder {
      TextView name;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;
      // ビューホルダー
      ViewHolder holder = null;
      // 無い場合だけ作る
      if (v == null) {
        // XMLからレイアウトを作る
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.recent_list_view, null);
        // ビューホルダーを作る
        holder = new ViewHolder();
        // テキストビューを取り出す
        holder.name = (TextView) v.findViewById(R.id.recent_name);
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
      String hash = getItem(position);
      if (hash != null) {//おそらくnullはないが念のため
        holder.name.setText(globals.twimptRooms.get(hash).name);
      } else {
        holder.name.setText(getResources().getString(R.string.monologue_name));
      }
      return v;
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
