package com.example.oigami.twimpt.image;

import com.example.oigami.twimpt.debug.Logger;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by oigami on 2015/01/10.
 */

public class FileDownloadNonThread {
  private final int TIMEOUT_READ = 5000;
  private final int TIMEOUT_CONNECT = 30000;
  private final int BUFFER_SIZE = 1024;
  private Interface mInterface;
  private FileOutputStream mFileOutputStream;
  private BufferedInputStream bufferedInputStream;
  private URL mUrl;
  private URLConnection mURLConnection;
  private String mUrlString;
  /** ダウンロードするサイズ */
  private int totalByte = 0;

  /** 現在ダウンロードしたサイズ */

  public interface Interface {
    void DownloadBegin(URLConnection urlConnection);

    /**
     * ダウンロード中に定期的に呼ばれる
     * @param loadedByte 現在ダウンロードしたデータのサイズ
     */
    void Downloading(int loadedByte);

    /**
     * trueを返すとダウンロードを中止する
     * @return
     */
    boolean OnCancel();
  }

  public FileDownloadNonThread(FileOutputStream fileOutputStream, String url, Interface anInterface) {
    mFileOutputStream = fileOutputStream;
    mUrlString = url;
    mInterface = anInterface;
  }

  public boolean Download() {
    try {
      if (connect(mUrlString) == false) return true;
    } catch (IOException e) {
      Logger.log("ConnectError:" + e.toString());
    }
    int currentByte = 0;
    mInterface.DownloadBegin(mURLConnection);
    byte[] buffer = new byte[BUFFER_SIZE];
    try {
      int len;
      while ((len = bufferedInputStream.read(buffer)) != -1) {
        mFileOutputStream.write(buffer, 0, len);
        currentByte += len;
        mInterface.Downloading(currentByte);
        if (mInterface.OnCancel()) {
          break;
        }
      }
    } catch (IOException e) {
      Logger.log(e.toString());
      return false;
    }
    try {
      close();
    } catch (IOException e) {
      Logger.log("CloseError:" + e.toString());
    }
    return true;
  }


  private boolean connect(String sUrl) throws IOException {
    mUrl = new URL(sUrl);
    mURLConnection = mUrl.openConnection();
    mURLConnection.setReadTimeout(TIMEOUT_READ);
    mURLConnection.setConnectTimeout(TIMEOUT_CONNECT);
    InputStream inputStream = mURLConnection.getInputStream();
    bufferedInputStream = new BufferedInputStream(inputStream, BUFFER_SIZE);
    if (mFileOutputStream == null) return false;
    totalByte = mURLConnection.getContentLength();
    Logger.log("downloading: total_byte" + totalByte);
    return true;
  }

  private void close() throws IOException {
    mFileOutputStream.flush();
    mFileOutputStream.close();
    bufferedInputStream.close();
  }
}