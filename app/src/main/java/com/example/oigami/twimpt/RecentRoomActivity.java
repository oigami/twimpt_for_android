package com.example.oigami.twimpt;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.oigami.twimpt.twimpt.TwimptJson;
import com.example.oigami.twimpt.twimpt.TwimptNetwork;
import com.example.oigami.twimpt.util.Network;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by oigami on 2014/10/03
 */
public class RecentRoomActivity extends ActionBarActivity {
  public static final String INTENT_ROOM_RECENT_NAME = "RECENT_ROOM_NAME";
  private DataApplication globals;
  private int mNowPage = 1;
  private String mRecentUrl;
  private boolean mNowUpdate = false;
  private SwipeRefreshLayout mSwipeRefreshWidget;
  private ArrayList<String> mArrayListHash = new ArrayList<>();
  private RecentRoomsListAdapter mAdapter = new RecentRoomsListAdapter();
  Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      RefreshEnd();
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
    mSwipeRefreshWidget = (SwipeRefreshLayout) findViewById(R.id.recent_room_refresh_layout);
    mSwipeRefreshWidget.setColorSchemeResources(
            R.color.blue_bright,
            R.color.green_light,
            R.color.orange_light,
            R.color.red_light);
    mSwipeRefreshWidget.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {
        GetRecentRoomData();
      }
    });
    //new TwimptAsyncTask(globals.twimptRooms.get(globals.now_room_hash))
    GetRecentRoomData();
  }
  private boolean RefreshStart() {
    //すでにアップデート中の場合
    if (mNowUpdate) return false;

    mNowUpdate = true;
    mSwipeRefreshWidget.setRefreshing(true);
    mSwipeRefreshWidget.setEnabled(false);
    return true;
  }

  private void RefreshEnd(){
    mNowUpdate = false;
    mSwipeRefreshWidget.setRefreshing(false);
    mSwipeRefreshWidget.setEnabled(true);
    mAdapter.notifyDataSetChanged();
  }

  private boolean CanRequest() {
    if (!Network.isConnected(this)) {
      RefreshEnd();
      Toast.makeText(this, R.string.update_network_error, Toast.LENGTH_LONG).show();
      return false;
    }
    if (!RefreshStart()) {
      RefreshEnd();
      Toast.makeText(this, R.string.updating, Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  public void GetRecentRoomData() {
    if (!CanRequest()) return;
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          final JSONObject json = TwimptNetwork.GetRecentRoom(mRecentUrl, mNowPage);
          if (mNowPage == 1)
            mArrayListHash.clear();
          TwimptJson.RecentRoomListParse(globals.twimptRooms, json, mArrayListHash);
        } catch (IOException | JSONException e) {
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
      ViewHolder holder = null;
      if (v == null) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.recent_list_view, null);
        holder = new ViewHolder();
        holder.name = (TextView) v.findViewById(R.id.recent_name);
        v.setTag(holder);
      } else {
        holder = (ViewHolder) v.getTag();
      }
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
