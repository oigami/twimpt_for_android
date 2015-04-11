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

/**
 * Created by oigami on 2015/03/13
 */
public class TwimptImageTable {

  private static final String TABLE_NAME = "TwimptImageCache";

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

  protected TwimptImageTable(ImageCacheDB database) {
    mImageCacheDB = database;
  }

  public String getFileName(String hash) {
    Cursor c = mImageCacheDB.existsFile(TABLE_NAME, hash, Column.HASH);
    if (!c.moveToFirst()) return null;
    long id = c.getLong(c.getColumnIndex(Column.ID));
    return mImageCacheDB.getFileName(TABLE_NAME, id);
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

  public Drawable getDrawable(String url, Context context) {
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

  private long insertOrGet(String hash, long totalByteSize) {
    long id = insert(hash, totalByteSize, false);
    if (id == -1) {
      final Cursor c = mImageCacheDB.existsFile(TABLE_NAME, hash, Column.HASH);
      if (c.moveToFirst()) id = c.getLong(c.getColumnIndex(Column.ID));
    }
    return id;
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
    if (getDrawable(hash, context) != null) return null;
    final long id = insertOrGet(hash, totalByteSize);
    if (id == -1) return null;
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

