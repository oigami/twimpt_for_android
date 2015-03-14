package com.example.oigami.twimpt.file;

import com.example.oigami.twimpt.debug.Logger;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by oigami on 2015/01/10
 */

public class FileDownloader {

  private final int TIMEOUT_READ = 5000;
  private final int TIMEOUT_CONNECT = 30000;
  private final int BUFFER_SIZE = 1024;
  private OnDownloadBeginListener mDownloadBegin;
  private OutputStream mFileOutputStream;
  private BufferedInputStream bufferedInputStream;
  private String mUrlString;
  private OnDownloadingListener mDownloading;
  private OnDownloadedListener mDownloaded;
  private OnCancelListener mCancel;

  public interface OnCancelListener {
    /**
     * trueを返すとダウンロードを中止する
     * @return true(終了) or false(続行)
     */
    boolean OnCancel();
  }

  /** 現在ダウンロードしたサイズ */
  public interface OnDownloadingListener {
    /**
     * ダウンロード中に定期的に呼ばれる
     * @param nowLoadedByte 現在ダウンロードしたデータのサイズ
     */
    public void OnDownloading(int nowLoadedByte);
  }

  public interface OnDownloadedListener {
    /**
     * ダウンロード終了時
     * @param isSuccess 成功した場合はtrue
     */
    public void OnDownloaded(String url, boolean isSuccess);
  }

  private void OnDownloaded(boolean isSuccess) {
    if (mDownloaded != null) mDownloaded.OnDownloaded(mUrlString, isSuccess);
  }

  public interface OnDownloadBeginListener {
    OutputStream DownloadBegin(URLConnection urlConnection);
  }

  public boolean OnCancel() {
    if (mCancel != null) return mCancel.OnCancel();
    return false;
  }

  public FileDownloader(String url, OnDownloadBeginListener listener) {
    mUrlString = url;
    mDownloadBegin = listener;
  }

  public FileDownloader(String url, FileOutputStream stream) {
    mUrlString = url;
    mFileOutputStream = stream;
  }

  /**
   * ダウンロード中に通知するリスナー
   * @param listener 通知するリスナー
   */
  public void SetOnDownloadingListener(OnDownloadingListener listener) {
    mDownloading = listener;
  }

  public void SetOnDownloadedListener(OnDownloadedListener listener) {
    mDownloaded = listener;
  }
  public void SetOnCancelListener(OnCancelListener listener) {
    mCancel = listener;
  }

  public boolean Download() {
    Logger.log("download:" + mUrlString);
    try {
      if (!connect(mUrlString)) {
        Logger.log("ConnectError");
        return false;
      }
    } catch (IOException e) {
      Logger.log("ConnectError:" + e.getMessage());
      Logger.log("ConnectError:" + e.toString());
      return false;
    }
    int currentByte = 0;

    byte[] buffer = new byte[BUFFER_SIZE];
    try {
      int len;
      while ((len = bufferedInputStream.read(buffer)) != -1) {
        mFileOutputStream.write(buffer, 0, len);
        currentByte += len;
        if (mDownloading != null) mDownloading.OnDownloading(currentByte);
        if (OnCancel()) break;
      }
    } catch (IOException e) {
      Logger.log(e.toString());
      OnDownloaded(false);
      return false;
    } catch (NullPointerException e) {
      Logger.log(e.toString());
      OnDownloaded(false);
      return false;
    }
    try {
      close();
      OnDownloaded(true);
    } catch (IOException e) {
      OnDownloaded(false);
      Logger.log("CloseError:" + e.toString());
    }
    return true;
  }


  private boolean connect(String sUrl) throws IOException {

    URL url = new URL(sUrl);
    URLConnection URLConnection = url.openConnection();
    URLConnection.setReadTimeout(TIMEOUT_READ);
    URLConnection.setConnectTimeout(TIMEOUT_CONNECT);
    InputStream inputStream = new FlushedInputStream(URLConnection.getInputStream());
    bufferedInputStream = new BufferedInputStream(inputStream, BUFFER_SIZE);
    if (bufferedInputStream == null) return false;
    /* ダウンロードするサイズ */
    int totalByte = URLConnection.getContentLength();
    Logger.log("downloading: total_byte" + totalByte);
    if (mDownloadBegin != null)
      mFileOutputStream = mDownloadBegin.DownloadBegin(URLConnection);
    if (mFileOutputStream == null) return false;
    return true;
  }

  private void close() throws IOException {
    mFileOutputStream.flush();
    mFileOutputStream.close();
    bufferedInputStream.close();
  }

  static class FlushedInputStream extends FilterInputStream {
    public FlushedInputStream(InputStream inputStream) {
      super(inputStream);
    }

    @Override
    public long skip(long n) throws IOException {
      long totalBytesSkipped = 0L;
      while (totalBytesSkipped < n) {
        long bytesSkipped = in.skip(n - totalBytesSkipped);
        if (bytesSkipped == 0L) {
          int c = read();
          if (c < 0) {
            break;  // we reached EOF
          } else {
            bytesSkipped = 1; // we read one byte
          }
        }
        totalBytesSkipped += bytesSkipped;
      }
      return totalBytesSkipped;
    }
  }

}