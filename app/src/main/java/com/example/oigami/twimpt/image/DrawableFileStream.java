package com.example.oigami.twimpt.image;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;

public class DrawableFileStream extends FilterOutputStream {
  public interface StreamCloseListener {
    public void Close();
  }
  StreamCloseListener mListener;

  public DrawableFileStream(FileOutputStream stream, StreamCloseListener listener) throws FileNotFoundException {
    super(stream);
    mListener = listener;
  }

  @Override
  public void close() throws IOException {
    super.close();
    mListener.Close();
  }
}
