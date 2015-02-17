package com.example.oigami.twimpt.image;

import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;

/**
 * Created by oigami on 2015/02/11
 */
public class FileDownloadThread extends FileDownloader {
  ExecutorService mExec;

  public FileDownloadThread(ExecutorService exec, String url, OnDownloadBeginListener listener) {
    super(url, listener);
    mExec = exec;
  }

  @Override
  public boolean Download() {
    mExec.submit(new Runnable() {
      @Override
      public void run() {
        SuperDownload();
      }
    });
    return true;
  }

  private void SuperDownload() {
    super.Download();
  }
}
