package com.example.oigami.twimpt;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.support.v7.app.ActionBarActivity;

import java.util.Map;

/**
 * Created by oigami on 2014/10/02.
 */
public class RoomListActivity extends ActionBarActivity {
  DataApplication globals;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.room_list_activity);

    globals = (DataApplication) this.getApplication();


    Resources res = getResources();

    //monologueは例外扱い
    TwimptRoom monologueRoom = new TwimptRoom();
    monologueRoom.type = "monologue";
    monologueRoom.name = res.getString(R.string.monologue_name);
    globals.twimptRooms.put(monologueRoom.type, monologueRoom);

    //publicは例外扱い
    TwimptRoom publicRoom = new TwimptRoom();
    publicRoom.type = "public";
    publicRoom.name = res.getString(R.string.public_name);
    globals.twimptRooms.put(publicRoom.type, publicRoom);

    getSupportActionBar().setTitle(res.getString(R.string.room_list_name));
    final String[] members = {
            res.getString(R.string.public_name),
            res.getString(R.string.monologue_name),
            res.getString(R.string.recent_created_name),
            res.getString(R.string.recent_opened_name),
            res.getString(R.string.recent_posted_name)
    };
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, members);
    final ListView listView = (ListView) findViewById(R.id.room_list);
    // アダプターを設定します
    listView.setAdapter(adapter);
    // リストビューのアイテムがクリックされた時に呼び出されるコールバックリスナーを登録します
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListView listView = (ListView) parent;
        // クリックされたアイテムを取得します
        String item = (String) listView.getItemAtPosition(position);
        //Toast.makeText(RoomListActivity.this, item, Toast.LENGTH_LONG).show();
        String[] hash = {"public", "monologue"};
        String roomHash;
        switch (position) {
          case 0:
          case 1: {
            roomHash = hash[position];
            Map<String, TwimptRoom> twimptRoomMap = globals.twimptRooms;
            TwimptRoom twimptRoom = twimptRoomMap.get(roomHash);
            Intent intent = new Intent(RoomListActivity.this, RoomActivity.class);
            intent.putExtra(RoomActivity.INTENT_ROOM_NAME_HASH, roomHash);
            //intent.putExtra("keyword", globals.twimptRooms.get(now_room));
            startActivity(intent);
            return;
          }
          case 2:
          case 3:
          case 4:
            Intent intent = new Intent(RoomListActivity.this, RecentRoomActivity.class);
            intent.putExtra(RecentRoomActivity.INTENT_ROOM_RECENT_NAME, members[position]);
            //intent.putExtra("keyword", globals.twimptRooms.get(now_room));
            startActivity(intent);
            return;
          default:
            //TODO
            Toast.makeText(RoomListActivity.this, "未実装", Toast.LENGTH_LONG).show();
        }
      }
    });

    // リストビューのアイテムが選択された時に呼び出されるコールバックリスナーを登録します
    listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ListView listView = (ListView) parent;
        // 選択されたアイテムを取得します
        String item = (String) listView.getSelectedItem();
        Toast.makeText(RoomListActivity.this, item, Toast.LENGTH_LONG).show();
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }

    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.room_list, menu);
    // メニューの要素を追加

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    SharedPreferences sharedPref;
    String accessToken, accessTokenSecret;
    switch (id) {
      case R.id.action_auth:
        sharedPref = getSharedPreferences("token", MODE_PRIVATE);
        accessToken = sharedPref.getString("access_token", "");
        accessTokenSecret = sharedPref.getString("access_token_secret", "");
        if (accessToken.equals("") || accessTokenSecret.equals("")) {
          Intent intent = new Intent(RoomListActivity.this, TwimptAuthActivity.class);
          startActivity(intent);
        } else {
          Toast.makeText(this, "すでに認証しています", Toast.LENGTH_LONG).show();
        }
        break;
      case R.id.action_deauthentication: {
        sharedPref = getSharedPreferences("token", MODE_PRIVATE);
        accessToken = sharedPref.getString("access_token", "");
        accessTokenSecret = sharedPref.getString("access_token_secret", "");
        if (accessToken.equals("") || accessTokenSecret.equals("")) {
          Toast.makeText(this, "認証されていません", Toast.LENGTH_LONG).show();
        } else {
          SharedPreferences.Editor e = sharedPref.edit();
          e.clear();
          e.commit();
          Toast.makeText(this, "認証を解除しました", Toast.LENGTH_LONG).show();
        }
        break;
      }
    }
    return super.onOptionsItemSelected(item);
  }
}
