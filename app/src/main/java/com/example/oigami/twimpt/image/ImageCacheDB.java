package com.example.oigami.twimpt.image;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.Drawable;

import com.example.oigami.twimpt.debug.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

/**
 * Created by oigami on 2014/10/02
 */
public class ImageCacheDB extends SQLiteOpenHelper {
  private File mDirectoryPath;
  private String dbFileName;
  public static final String TBL_CACHE = "ImageCache";
  private static final int DB_VERSION = 2;
  private Context mContext;

  public interface Column {
    public static final String ID = "_id";
    /**
     * 内部のファイル名と実際のファイル名が違う 内部では id番号が入っているだけだが<br>
     * 外部ではdbFileName+"_"+idになっている
     * TODO もはやある必要がないのでそのうち削除する予定
     */
    public static final String D_FILENAME = "fileName";
    public static final String REGIST_DATE = "registDate";
    public static final String URL = "url";
    public static final String TYPE = "type";
    public static final String TOTAL_BYTE_SIZE = "totalByteSize";
    public static final String IS_DOWNLOADED = "isDownloaded";
  }

  private SQLiteDatabase mDB;

  public String getDbFileName() {
    return dbFileName;
  }


  public ImageCacheDB(Context context, String dbFileName) {
    super(context, dbFileName, null, DB_VERSION);
    this.dbFileName = dbFileName;
    mDirectoryPath = new File(context.getFilesDir(), dbFileName);
    mDirectoryPath.mkdir();
    mContext = context;
  }

  public ImageCacheDB(Context context, String dbFileName, File directoryPath) {
    super(context, dbFileName, null, DB_VERSION);
    this.dbFileName = dbFileName;
    mDirectoryPath = directoryPath;
    mDirectoryPath.mkdir();
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
                    + Column.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Column.REGIST_DATE + " INTEGER,"
                    + Column.URL + " VARCHAR(300) UNIQUE,"
                    + Column.TYPE + " VARCHAR(50),"
                    + Column.TOTAL_BYTE_SIZE + " INTEGER,"
                    + Column.IS_DOWNLOADED + " INTEGER)"
    );
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion == 1) {
      Cursor c = db.query(TBL_CACHE, null, null, null, null, null, null);
      if (c.moveToFirst()) {
        do {
          final long id = c.getLong(c.getColumnIndex(Column.ID));

          mContext.deleteFile(getFileName(id));
        } while (c.moveToNext());
      }
      db.execSQL("DROP TABLE " + TBL_CACHE + ";");
      onCreate(db);
    }
  }

  public long insert(String url) {
    ContentValues values = new ContentValues();
    values.put(Column.REGIST_DATE, new Date().getTime());
    values.put(Column.URL, url);
    return getDB().insertOrThrow(TBL_CACHE, null, values);
  }

  public long insert(long id, String type, String date, long totalByteSize, String url, boolean isDownloaded) {
    ContentValues values = new ContentValues();
    values.put(Column.ID, id);
    values.put(Column.TYPE, type);
    values.put(Column.REGIST_DATE, date);
    values.put(Column.TOTAL_BYTE_SIZE, totalByteSize);
    values.put(Column.URL, url);
    values.put(Column.IS_DOWNLOADED, isDownloaded ? 1 : 0);
    return getDB().insertOrThrow(TBL_CACHE, null, values);
  }

  public long insert(ContentValues values) {
    return getDB().insertOrThrow(TBL_CACHE, null, values);
  }

  public long insert(String url, String type, Long totalByteSize, boolean isDownloaded) {
    ContentValues values = new ContentValues();
    values.put(Column.REGIST_DATE, new Date().getTime());
    values.put(Column.URL, url);
    values.put(Column.TYPE, type);
    values.put(Column.TOTAL_BYTE_SIZE, totalByteSize);
    values.put(Column.IS_DOWNLOADED, isDownloaded ? 1 : 0);
    return insert(values);
  }

  public int update(long id, String type, Long totalByteSize, Boolean isDownloaded) {
    ContentValues values = new ContentValues();
    if (type != null) values.put(Column.TYPE, type);
    if (totalByteSize != null) values.put(Column.TOTAL_BYTE_SIZE, totalByteSize);
    if (isDownloaded != null) values.put(Column.IS_DOWNLOADED, isDownloaded ? 1 : 0);
    return getDB().update(TBL_CACHE, values, Column.ID + " = ?", new String[]{String.valueOf(id)});
  }

  public int delete(long id) {
    return getDB().delete(TBL_CACHE, Column.ID + " = ?", new String[]{String.valueOf(id)});
  }

  public Cursor exists(String url) {
    return getDB().query(TBL_CACHE, null, Column.URL + " = ?", new String[]{url}, null, null, null);
  }

  public Cursor existsFile(String url) {
    return getDB().query(TBL_CACHE, null, Column.URL + " = ?", new String[]{url}, null, null, null);
  }

  public Cursor findOlderCache() {
    return getDB().query(TBL_CACHE,
            null,
            Column.REGIST_DATE + " < ?",
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

  public String getFileName(long id) {
    return getDbFileName() + '_' + String.format("%06d", id);
  }

  public String getFileName(long id, Context cxt) {
    return cxt.getFileStreamPath(getFileName(id)).getAbsolutePath();
  }

  public String getFileName(String url, Context cxt) {
    final Cursor c = existsFile(url);
    if (!c.moveToFirst()) return null;
    final long id = c.getLong(c.getColumnIndex(Column.ID));
    return getFileName(id, cxt);
  }

  /*  画像をデータベース取得する            */
  public Drawable getDrawable(String url, Context cxt) {
    final Cursor c = existsFile(url);
    if (!c.moveToFirst()) return null;
    final boolean isDownloaded = c.getLong(c.getColumnIndex(Column.IS_DOWNLOADED)) != 0;
    if (!isDownloaded) return null;
    final String type = c.getString(c.getColumnIndex(Column.TYPE));
    final long id = c.getLong(c.getColumnIndex(Column.ID));
    if (type == null) return null;
    if (type.equals("image/jpg") || type.equals("image/jpeg") || type.equals("image/png") || type.equals("image/gif")) {
      String fileName = cxt.getFileStreamPath(getFileName(id)).getAbsolutePath();
      File file = new File(fileName);
      Logger.log("file size:" + file.length());
      Drawable drawable = Drawable.createFromPath(fileName);
      if (drawable == null) {
        delete(id);
        Logger.log("Drawable delete:" + url);
      } else {
        Logger.log("Drawable load:" + url);
      }
      return drawable;
      //setImageDrawable(drawable);
      //setVisibility(RemoteImageView.VISIBLE);
    }
    return null;
  }

  /**
   * 画像ファイルを保存する<br>
   * この関数ではファイルを開くだけなので書き込みを別途する必要がある
   * @param url     画像があるurl
   * @param type    画像のフォーマットタイプ
   * @param context Activity
   * @return 保存するためのFileOutputStream
   * @throws FileNotFoundException
   */
  public OutputStream openFileOutput(final String url, final String type, long totalByteSize,
                                     Context context) throws FileNotFoundException {
    long id = insert(url, type, totalByteSize, false);
    String filename = String.format("%06d", id);
    FileOutputStream s = context.openFileOutput(getDbFileName() + "_" + filename, Context.MODE_PRIVATE);
    return new DrawableFileStream(this, id, s);
  }

  static class DrawableFileStream extends FilterOutputStream {
    ImageCacheDB mDatabase;
    long mId;

    public DrawableFileStream(ImageCacheDB database, long id, FileOutputStream stream) throws FileNotFoundException {
      super(stream);
      mDatabase = database;
      mId = id;
    }

    @Override
    public void close() throws IOException {
      super.close();
      mDatabase.update(mId, null, null, true);
    }
  }
}