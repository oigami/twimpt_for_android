package com.example.oigami.twimpt;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.example.oigami.twimpt.debug.Logger;

/**
 * Created by oigami on 2015/02/24
 */
public class TextLinkListener {
  public void SetTextOnTouchListener(TextView textView, Spannable spannable) {
    Listener listener = new Listener();
    listener.nowSpannable = spannable;
    textView.setText(spannable);
    textView.setOnTouchListener(listener);
  }

  public Spannable SetTextOnTouchListener(TextView textView, CharSequence spanned) {
    Listener listener = new Listener();
    Spannable spannable = Spannable.Factory.getInstance().newSpannable(spanned);
    createURLSpanEx(spannable);
    listener.nowSpannable = spannable;
    textView.setText(spannable);
    textView.setOnTouchListener(listener);
    return spannable;
  }

  class URLSpanEx extends URLSpan {
    private int mColor;
    private TextPaint mTextPaint;

    public URLSpanEx(String url, int color) {
      super(url);
      Logger.log("new URLSpanEx");
      mColor = color;
    }

    public void setColor(int color) {
      mColor = color;
      if (mTextPaint != null) mTextPaint.setColor(mColor);
    }

    @Override
    public void updateDrawState(@NonNull TextPaint textPaint) {
      mTextPaint = textPaint;
      super.updateDrawState(textPaint);
      textPaint.setColor(mColor);
    }
  }

  private static int getLine(TextView widget, Layout layout, MotionEvent event) {
    int y = (int) event.getY();
    y -= widget.getTotalPaddingTop();
    y += widget.getScrollY();
    return layout.getLineForVertical(y);
  }

  private static int getOffset(TextView widget, int x, int line) {
    Layout layout = widget.getLayout();
    return layout.getOffsetForHorizontal(line, x);
  }

  private static int getX(TextView widget, MotionEvent event) {
    int x = (int) event.getX();
    x -= widget.getTotalPaddingLeft();
    x += widget.getScrollX();
    return x;
  }

  private static URLSpan[] getURLSpan(Layout layout, Spannable spannable, int x, int line, int offset) {
    if (x < layout.getLineMax(line)) {
      return spannable.getSpans(offset, offset, URLSpan.class);
    }
    return null;
  }

  public Spannable createURLSpanEx(Spannable span) {
    for (URLSpan urlSpan : span.getSpans(0, span.length(), URLSpan.class)) {
      URLSpanEx urlSpanEx = new URLSpanEx(urlSpan.getURL(), Color.BLUE);
      setUrlSpanEx(span, urlSpan, urlSpanEx);
    }
    return span;
  }

  private static void setUrlSpanEx(Spannable spannable, URLSpan link, URLSpanEx spanEx) {
    int start = spannable.getSpanStart(link);
    int end = spannable.getSpanEnd(link);
    Logger.log("(" + start + "," + end + ")");
    spannable.removeSpan(link);
    spannable.setSpan(spanEx, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  }

  private class Listener implements View.OnTouchListener {
    private URLSpanEx nowSelectUrlSpan;
    private Spannable nowSpannable;


    @Override
    public boolean onTouch(View v, MotionEvent event) {
      TextView widget = (TextView) v;
      int action = event.getAction();
      //    Logger.log("onTouch:" + action);

      Layout layout = widget.getLayout();
      if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
        int line = getLine(widget, layout, event);
        int x = getX(widget, event);
        int offset = getOffset(widget, x, line);
        URLSpan[] link = getURLSpan(layout, nowSpannable, x, line, offset);
        if (link != null && link.length != 0) {
          if (action == MotionEvent.ACTION_UP) {
            link[0].onClick(widget);
            EndSetColorUrl();
            widget.setText(nowSpannable);
          } else if (action == MotionEvent.ACTION_DOWN) {
            BeginSetColorUrl(nowSpannable, link[0]);
            widget.setText(nowSpannable);
          }
          return true;
        } else if (action == MotionEvent.ACTION_MOVE) {
          EndSetColorUrl();
          widget.setText(nowSpannable);
          return false;
        }
      }
      if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        EndSetColorUrl();
        widget.setText(nowSpannable);
      }
      return false;
    }

    private void BeginSetColorUrl(Spannable spannable, URLSpan link) {
      Logger.log("begin");
      if (nowSelectUrlSpan != null) EndSetColorUrl();
      if (link instanceof URLSpanEx) {
        URLSpanEx span = (URLSpanEx) link;
        span.setColor(Color.YELLOW);
        nowSelectUrlSpan = span;
        return;
      }
      URLSpanEx span;

      span = new URLSpanEx(link.getURL(), Color.YELLOW);
      setUrlSpanEx(spannable, link, span);
      nowSelectUrlSpan = span;
      //    int start = mSpannable.getSpanStart(link);
      //    int end = mSpannable.getSpanEnd(link);
      //    mSpannable.removeSpan(link);
      //    URLSpan span = new URLSpanEx(link.getURL(), Color.YELLOW);
      //    mSpannable.setSpan(span, start, end, 0);
    }

    private void EndSetColorUrl() {
      if (nowSelectUrlSpan == null) return;
      nowSelectUrlSpan.setColor(Color.BLUE);
      //setUrlSpanEx(nowSelectUrlSpan,nowSelectUrlSpan);
      nowSelectUrlSpan = null;
      Logger.log("end");
    }
  }
}