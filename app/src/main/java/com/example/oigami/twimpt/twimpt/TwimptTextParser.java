package com.example.oigami.twimpt.twimpt;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;

import com.example.oigami.twimpt.debug.Logger;
import com.example.oigami.twimpt.util.HtmlEx;

import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by oigami on 2015/02/17
 */
public class TwimptTextParser {
  private static final String TWIMPT_IMAGE_URL = TwimptUtil.TWIMPT_IMAGE_URL;

  static class ParsedTextData {
    Spannable textSpan;
    /**
     * 投稿された画像urlを保持
     * nullの場合は画像なし
     * date/hash.ext の形式
     */
    String[] postedImageUrl;
  }

  private Pattern[] mPatterns;
  private Pattern[] mImagePatterns;

  private static String[] RegexFormat(String str, String rep) {
    return new String[]{"\\[" + str + "\\]([\\s\\S]*?)\\[\\/" + str + "\\]", rep};
  }

  private static String[] RegexFormat(char str, String rep) {
    return new String[]{"\\[" + str + "\\]([\\s\\S]*?)\\[\\/" + str + "\\]", rep};
  }

  private static String[] RegexFormat(String str) {
    return RegexFormat(str, "<" + str + ">$1</" + str + ">");
  }

  private static String[] RegexFormat(char str) {
    return RegexFormat(str, "<" + str + ">$1</" + str + ">");
  }

  /**
   * Twimptの画像のパスを作成する
   * @param date 日付
   * @param ext  拡張子
   * @param hash ハッシュ
   * @return date/hash.ext
   */
  private static String createImagePath(String date, String ext, String hash) {
    return date + "/" + hash + "." + ext;
  }

  private static final String[][] imageRegex = {
          {
                  "\\[img:([0-9]+?):([a-zA-Z0-9]+?)\\]([a-zA-Z0-9]+?)\\[/img\\]",
                  "<a href='" + TWIMPT_IMAGE_URL + createImagePath("$1", "$2", "$3") + "'>$3</a>"
          }
  };
  private static final String[][] decorationRegex = {
          {"((?:p|tp|ttp|http|^|\\s)(s?://[-_.!~*'()a-zA-Z0-9;/?:@&=+\\$,%#]+))", "<a href='http$2' target='_blank'>$1</a>"},
          {"\n", "<br>"},
          RegexFormat('b'),
          RegexFormat('i'),
          RegexFormat('s'),
          RegexFormat('u'),
          RegexFormat("aa", "$1"),
          RegexFormat('o', "$1"),
          RegexFormat("sub"),
          RegexFormat("sup"),
          RegexFormat("big"),
          RegexFormat("small"),
          RegexFormat("blockquote"),
          {"\\[color:(#?[a-fA-F0-9]+?)\\]([\\s\\S]*?)\\[/color\\]", "<font color=$1>$2</font>"},
          {"\\[bgcolor:(#?[a-fA-F0-9]+?)\\]([\\s\\S]*?)\\[/bgcolor\\]", "<bg color=$1>$2</bg>"},

          //          [/\[border\:#?([a-fA-F0-9]+?)\]([\s\S]*?)\[\/border\]/g,'$2'],
          //          [/\[pre\]([\s\S]*?)\[\/pre\]/g,'$1'],
          {"\\[censored\\]", "<font color=#ff0000>禁則事項です</font>"},

          //          [/\!do/g,'┣゛'],
          //          [/\!xxx/g,'&hearts;&hearts;&hearts;'],

          //          [/\[quote\](.*?)\[\/quote\]/g,'[引用]'],
          //          [/\!link\:account/g,'アカウント'],
          //          [/\!link\:profile/g,'プロフィール'],
          //          [/\!link\:roomlist/g,'ルーム'],
          //          [/\!link\:threadlist/g,'スレッド'],
          //          [/\!link\:timeline/g,'タイムライン'],
          //          [/\!link\:public /g,'パブリック'],
          //          [/\!link\:favorite/g,'お気に入り'],
          //          [/\!link\:design/g,'デザイン'],
          //          [/\!link\:option/g,'オプション'],
          //          [/\!link\:help/g,'ヘルプ'],
          //          [/\!link\:twitter/g,'ツイッター'],
          //          [/\!link\:croudia/g,'クローディア'],
          //          [/\<.*?\>/g,'']
  };

  public TwimptTextParser() {
    int length = decorationRegex.length;
    mPatterns = new Pattern[length];
    for (int i = length - 1; i >= 0; i--) {
      mPatterns[i] = Pattern.compile(decorationRegex[i][0]);
    }

    int imageLength = imageRegex.length;
    mImagePatterns = new Pattern[imageLength];
    for (int i = imageLength - 1; i >= 0; i--) {
      mImagePatterns[i] = Pattern.compile(imageRegex[i][0]);
    }
  }

  public ParsedTextData Parse(String rawText) {
    //    rawText="[color:#ffff00]color:#ffff00[/color][sub]sub[/sub][sup]sup[/sup]" +
    //                    "[big]big[/big][small]small[/small][o]o[/o][u]u[/u][s]s[/s]" +
    //                    "[bgcolor:#8000ff]bgcolor:#8000ff[/bgcolor][i]i[/i][b]b[/b]";

    //    data.rawText = "http://www.google.co.jp/tesuto\n";
    //    data.rawText += "ttp://www.google.co.jp/テスト\n";
    //    data.rawText += "ttps://www.google.co.jp/ ";
    //    data.rawText += "https://www.google.co.jp/\n";
    //    data.rawText += "ps://www.google.co.jp/\n";
    //    data.rawText += "://www.google.co.jp/\n";
    ParsedTextData parsedText = new ParsedTextData();
    String tempStr = TextDecorationParse(rawText);
    Spanned tempSpanned = HtmlEx.fromHtml(ImageParse(parsedText, tempStr), null, new HtmlEx.TagHandler() {
      @Override
      public void handleTag(boolean opening, String tag, Editable output, Attributes attributes, XMLReader xmlReader) {
        if (tag.equals("bg")) {
          int len = output.length();
          if (opening) {
            String color = attributes.getValue("color");
            output.setSpan(new BackgroundColorSpan(Color.parseColor(color)), len, len, Spannable.SPAN_MARK_MARK);
          } else {
            BackgroundColorSpan obj = (BackgroundColorSpan) getLast(output, BackgroundColorSpan.class);
            int where = output.getSpanStart(obj);
            output.removeSpan(obj);
            if (where != len) {
              output.setSpan(obj, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
          }
        }
      }

      private Object getLast(Editable text, Class kind) {
        Object[] obj = text.getSpans(0, text.length(), kind);
        if (obj.length == 0) {
          return null;
        } else {
          for (int i = obj.length; i > 0; i--) {
            if (text.getSpanFlags(obj[i - 1]) == Spannable.SPAN_MARK_MARK) {
              return obj[i - 1];
            }
          }
          return null;
        }
      }
    });
    parsedText.textSpan = Spannable.Factory.getInstance().newSpannable(tempSpanned);
    return parsedText;
  }

  public String TextDecorationParse(String text) {
    int cnt = 0;
    for (Pattern pattern : mPatterns) {
      Matcher m = pattern.matcher(text);
      if (m.find())
        text = m.replaceAll(decorationRegex[cnt][1]);
      cnt++;
    }
    //最初がタグの場合はそれがrootとして扱われてしまうのでrootを入れておく
    return "<root>" + text + "</root>";
  }

  public String ImageParse(ParsedTextData parsedText, String text) {
    int cnt = 0;
    ArrayList<String> url = new ArrayList<>();
    for (Pattern pattern : mImagePatterns) {
      Matcher m = pattern.matcher(text);
      if (m.find()) {
        do {
          String imagePath = createImagePath(m.group(1), m.group(2), m.group(3));
          Logger.log(imagePath);
          url.add(imagePath);
          //Logger.log(m.group(1));
        } while (m.find());
        text = m.replaceAll(imageRegex[cnt][1]);
      }
      cnt++;
    }
    if (url.size() != 0)
      parsedText.postedImageUrl = url.toArray(new String[url.size()]);
    return text;
  }
}
