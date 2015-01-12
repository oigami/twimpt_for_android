package com.example.oigami.twimpt.image;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.Drawable;

import com.example.oigami.twimpt.debug.Logger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;

/**
 * Created by oigami on 2014/10/02.
 */
public class ImageCacheDB extends SQLiteOpenHelper {

  private String dbFileName;
  public static final String TBL_CACHE = "ImageCache";
  private static final int DB_VERSION = 1;

  public interface CacheColumn {
    public static final String ID = "_id";
    /**
     * 内部のファイル名と実際のファイル名が違う 内部では id番号が入っているだけだが<br>
     * 外部ではdbFileName+"_"+idになっている
     * TODO もはやある必要がないのでそのうち削除する予定
     */
    public static final String FILENAME = "fileName";
    public static final String REGIST_DATE = "registDate";
    public static final String URL = "url";
    public static final String TYPE = "type";
  }

  private SQLiteDatabase mDB;

  public static ImageCacheDB instance;

  public String getDbFileName() {
    return dbFileName;
  }


  public ImageCacheDB(Context context, String dbFileName) {
    super(context, dbFileName, null, DB_VERSION);
    this.dbFileName = dbFileName;
  }

//  public static ImageCacheDB getInstance(Context context) {
//    if (instance == null) {
//      instance = new ImageCacheDB(context);
//    }
//    return instance;
//  }

  synchronized private SQLiteDatabase getDB() {
    if (mDB == null) {
      mDB = getWritableDatabase();
    }
    return mDB;
  }

  public void close() {
    if (mDB != null) {
      getDB().close();
    }
    super.close();
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE IF NOT EXISTS " + TBL_CACHE + " ("
                    + CacheColumn.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + CacheColumn.REGIST_DATE + " INTEGER,"
                    + CacheColumn.URL + " VARCHAR(300),"
                    + CacheColumn.TYPE + " VARCHAR(100),"
                    + CacheColumn.FILENAME + " VARCHAR(20))"
    );
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

  }

  public long insert(String url) {
    ContentValues values = new ContentValues();
    values.put(CacheColumn.REGIST_DATE, new Date().getTime());
    values.put(CacheColumn.URL, url);
    return getDB().insertOrThrow(TBL_CACHE, null, values);
  }

  public int update(long id, String filename, String type) {
    ContentValues values = new ContentValues();
    values.put(CacheColumn.TYPE, type);
    values.put(CacheColumn.FILENAME, filename);
    return getDB().update(TBL_CACHE, values, CacheColumn.ID + " = ?", new String[]{String.valueOf(id)});
  }

  public int delete(long id) {
    return getDB().delete(TBL_CACHE, CacheColumn.ID + " = ?", new String[]{String.valueOf(id)});
  }

  public Cursor exists(String url) {
    return getDB().query(TBL_CACHE, null, CacheColumn.URL + " = ?", new String[]{url}, null, null, null);
  }

  public Cursor existsFile(String url) {
    return getDB().query(TBL_CACHE, null, CacheColumn.URL + " = ? AND " + CacheColumn.FILENAME + " IS NOT NULL", new String[]{url}, null, null, null);
  }

  public Cursor findOlderCache() {
    return getDB().query(TBL_CACHE,
            null,
            CacheColumn.REGIST_DATE + " < ?",
            new String[]{String.valueOf(new Date().getTime() - 604800)}, // 7*24*60*60
            null, null, null);
  }

  public Cursor findAll() {
    return getDB().query(TBL_CACHE,
            null,
            null,
            null,
            null, null, null);
  }

  @Override
  protected void finalize() throws Throwable {
    if (mDB != null) {
      mDB.close();
    }
    this.close();
    super.finalize();
  }

  /*  画像をデータベース取得する            */
  public Drawable getDrawable(String url, Context cxt) {
    final Cursor c = existsFile(url);
    if (c.moveToFirst()) {
      final String filename = c.getString(c.getColumnIndex(ImageCacheDB.CacheColumn.FILENAME));
      final String type = c.getString(c.getColumnIndex(ImageCacheDB.CacheColumn.TYPE));
      if (type.equals("image/jpg") || type.equals("image/jpeg") || type.equals("image/png") || type.equals("image/gif")) {
        Logger.log("Drawable load:" + url);
        return Drawable.createFromPath(cxt.getFileStreamPath(getDbFileName() + "_" + filename).getAbsolutePath());
        //setImageDrawable(drawable);
        //setVisibility(RemoteImageView.VISIBLE);
      }
    }
    return null;
  }

  /**
   * 画像ファイルを保存する<br>
   * この関数ではファイルを開くだけなので書き込みを別途する必要がある
   * @param url     画像があるurl
   * @param type    画像のフォーマットタイプ
   * @param context
   * @return 保存するためのFileOutputStream
   * @throws FileNotFoundException
   */
  public FileOutputStream openFileOutput(String url, String type, Context context) throws FileNotFoundException {
    long id = insert(url);
    String filename = String.format("%06d", id);
    FileOutputStream stream = context.openFileOutput(getDbFileName() + "_" + filename, Context.MODE_PRIVATE);
    update(id, filename, type);
    return stream;
  }
}