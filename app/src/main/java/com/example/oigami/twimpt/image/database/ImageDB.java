package com.example.oigami.twimpt.image.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.Drawable;

import com.example.oigami.twimpt.debug.Logger;

import java.io.File;
import java.util.Date;

/**
 * Created by oigami on 2014/10/02
 */
public class ImageDB extends SQLiteOpenHelper {
  private File mDirectoryPath;
  private static final String dbFileName = "ImageCache.db";
  //public static final String TBL_CACHE = "ImageCache";
  private static final int DB_VERSION = 3;
  private Context mContext;
  private TwimptImageTable mTwimptImageTable;
  private TwimptIconTable mTwimptIconTable;
  private SQLiteDatabase mDB;
  private static ImageDB instance;

  protected ImageDB(Context context) {
    super(context, dbFileName, null, DB_VERSION);
    mDirectoryPath = new File(context.getFilesDir(), dbFileName);
    mDirectoryPath.mkdir();
    mContext = context;
    mTwimptIconTable = new TwimptIconTable(this);
    mTwimptImageTable = new TwimptImageTable(this);
  }


  public static ImageDB getInstance(Context context) {
    if (instance == null) {
      instance = new ImageDB(context);
    }
    return instance;
  }

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
    TwimptImageTable.onCreate(db);
    TwimptIconTable.onCreate(db);
  }

  public TwimptIconTable IconTableInstance() {
    return mTwimptIconTable;
  }

  public TwimptImageTable ImageTableInstance() {
    return mTwimptImageTable;
  }

  public String getDBName() {
    return dbFileName;
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    final String TBL_CACHE = "ImageCache";
    if (oldVersion == 1 || oldVersion == 2) {
      Cursor c = db.query(TBL_CACHE, null, null, null, null, null, null);
      if (c.moveToFirst()) {
        do {
          final long id = c.getLong(c.getColumnIndex("_id"));
          mContext.deleteFile(getDBName() + '_' + String.format("%06d", id));
        } while (c.moveToNext());
      }
      db.execSQL("DROP TABLE " + TBL_CACHE + ";");
      onCreate(db);
    }
    TwimptIconTable.onUpgrade(db, oldVersion, newVersion);
    TwimptImageTable.onUpgrade(db, oldVersion, newVersion);
  }

  protected long insert(String tableName, ContentValues values) {
    return getDB().insert(tableName, null, values);
  }

  protected int update(String tableName, ContentValues values, long id, String columnId) {
    return getDB().update(tableName, values, columnId + " = ?", new String[]{String.valueOf(id)});
  }

  protected int delete(String tableName, long id, final String columnId) {
    return getDB().delete(tableName, columnId + " = ?", new String[]{String.valueOf(id)});
  }

  protected Cursor existsFile(String tableName, String url, final String columnUrl) {
    return getDB().query(tableName, null, columnUrl + " = ?", new String[]{url}, null, null, null);
  }

  protected Cursor findOlderCache(String tableName, String columnRegistDate) {
    return getDB().query(tableName,
                         null,
                         columnRegistDate + " < ?",
                         new String[]{String.valueOf(new Date().getTime() - 604800)}, // 7*24*60*60
                         null, null, null);
  }

  protected Cursor findAll(String tableName) {
    return getDB().query(tableName, null, null, null, null, null, null);
  }

  @Override
  protected void finalize() throws Throwable {
    if (mDB != null) {
      mDB.close();
    }
    this.close();
    super.finalize();
  }

  protected String getFileName(String tableName, long id) {
    return getDBName() + '_' + tableName + '_' + String.format("%06d", id);
  }

  private String getAbsoluteFileName(String tableName, long id) {
    String temp = getFileName(tableName, id);
    return mContext.getFileStreamPath(temp).getAbsolutePath();
  }

  protected Drawable getDrawable(String fileName) {
    fileName = mContext.getFileStreamPath(fileName).getAbsolutePath();
    File file = new File(fileName);
    Logger.log("file size:" + file.length() + "\nname:" + fileName);
    return Drawable.createFromPath(fileName);
  }
}


