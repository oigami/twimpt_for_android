package com.example.oigami.twimpt.image.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;

import com.example.oigami.twimpt.debug.Logger;
import com.example.oigami.twimpt.image.DrawableFileStream;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;

public class TwimptIconTable {

  private static final String TABLE_NAME = "TwimptIconCache";

  public static void onCreate(SQLiteDatabase db) {
    db.execSQL(CREATE_SQL);
  }

  private static final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                                           + Column.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                                           + Column.REGIST_DATE + " INTEGER,"
                                           + Column.HASH + " VARCHAR(64) UNIQUE,"
                                           + Column.TOTAL_BYTE_SIZE + " INTEGER,"
                                           + Column.IS_DOWNLOADED + " INTEGER)";

  public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

  }

  private interface Column {
    public static final String ID = "_id";
    public static final String REGIST_DATE = "registDate";
    public static final String HASH = "hash";
    public static final String TOTAL_BYTE_SIZE = "totalByteSize";
    public static final String IS_DOWNLOADED = "isDownloaded";
  }

  private ImageCacheDB mImageCacheDB;

  protected TwimptIconTable(ImageCacheDB database) {
    mImageCacheDB = database;
  }

  public long insert(String hash, long totalByteSize, boolean isDownloaded) {
    ContentValues values = new ContentValues();
    values.put(Column.REGIST_DATE, new Date().getTime());
    values.put(Column.TOTAL_BYTE_SIZE, totalByteSize);
    values.put(Column.HASH, hash);
    values.put(Column.IS_DOWNLOADED, isDownloaded ? 1 : 0);
    return mImageCacheDB.insert(TABLE_NAME, values);
  }

  public int update(long id, boolean isDownloaded) {
    ContentValues values = new ContentValues();
    values.put(Column.IS_DOWNLOADED, isDownloaded ? 1 : 0);
    return mImageCacheDB.update(TABLE_NAME, values, id, Column.ID);
  }

  public int delete(long id) {
    return mImageCacheDB.delete(TABLE_NAME, id, Column.ID);
  }

  public Drawable getDrawable(String url) {
    final Cursor c = mImageCacheDB.existsFile(TABLE_NAME, url, Column.HASH);
    if (!c.moveToFirst()) return null;
    final boolean isDownloaded = c.getLong(c.getColumnIndex(Column.IS_DOWNLOADED)) != 0;
    if (!isDownloaded) return null;
    final long id = c.getLong(c.getColumnIndex(Column.ID));
    String fileName = mImageCacheDB.getFileName(TABLE_NAME, id);
    Drawable drawable = mImageCacheDB.getDrawable(fileName);
    if (drawable == null) delete(id);
    return drawable;
  }

  /**
   * 画像ファイルを保存する<br>
   * この関数ではファイルを開くだけなので書き込みを別途する必要がある
   * @param hash    twimpt画像のhash部分（？）
   * @param context Activity
   * @return 保存するためのFileOutputStream
   * @throws java.io.FileNotFoundException
   */
  public OutputStream openFileOutput(final String hash, long totalByteSize, Context context) throws FileNotFoundException {
    final long id = insert(hash, totalByteSize, false);
    final String fileName = mImageCacheDB.getFileName(TABLE_NAME, id);
    Logger.log(fileName);
    FileOutputStream s = context.openFileOutput(fileName, Context.MODE_PRIVATE);
    DrawableFileStream.StreamCloseListener listener = new DrawableFileStream.StreamCloseListener() {
      @Override
      public void Close() {
        update(id, true);
      }
    };
    return new DrawableFileStream(s, listener);
  }
}
