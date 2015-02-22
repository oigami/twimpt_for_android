package com.example.oigami.twimpt.twimpt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by oigami on 2015/02/17
 */
public class TwimptLogParser {
  private Pattern[] mPatterns;
  private static final String[] templateRegex = {
          "\\[{0}\\]([\\s\\S]*?)\\[\\/{0}\\]"
  };

  private static String[] RegexFormat(String rep) {
    return new String[]{"\\[" + rep + "\\]([\\s\\S]*?)\\[\\/" + rep + "\\]", "<" + rep + ">$1</" + rep + ">"};
  }

  private static String[] RegexFormat(char rep) {
    return new String[]{"\\[" + rep + "\\]([\\s\\S]*?)\\[\\/" + rep + "\\]", "<" + rep + ">$1</" + rep + ">"};
  }

  private static final String[][] regex = {
          {"\n", "<br>"},
          RegexFormat('b'),
          RegexFormat('i'),
          RegexFormat('s'),
          RegexFormat('u'),
          {String.format(templateRegex[0], "aa"), "$1"},
          {String.format(templateRegex[0], 'o'), "$1"},
          RegexFormat("sub"),
          RegexFormat("sup"),
          RegexFormat("big"),
          RegexFormat("small"),
          RegexFormat("blockquote"),
          {"\\[color:#?([a-fA-F0-9]+?)\\]([\\s\\S]*?)\\[/color\\]", "<font color=$1>$2</font>"},

          //          [/\[bgcolor\:#?([a-fA-F0-9]+?)\]([\s\S]*?)\[\/bgcolor\]/g,'$2'],
          //          [/\[border\:#?([a-fA-F0-9]+?)\]([\s\S]*?)\[\/border\]/g,'$2'],
          //          [/\[pre\]([\s\S]*?)\[\/pre\]/g,'$1'],
          {"\\[censored\\]", "<font color=#ff0000>禁則事項です</font>"},

          //          [/\!do/g,'┣゛'],
          //          [/\!xxx/g,'&hearts;&hearts;&hearts;'],

          //          [/\[img\:([0-9]+?)\:([a-zA-Z0-9]+?)\]([a-zA-Z0-9]+?)\[\/img\]/g,'【画像】'],
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

  public TwimptLogParser() {
    mPatterns = new Pattern[regex.length];
    int cnt = 0;
    for (Pattern pattern : mPatterns) {
      pattern = Pattern.compile(regex[cnt][0]);
    }
  }

  public String Parse(String text) {
    int cnt = 0;
    for (Pattern pattern : mPatterns) {
      Matcher m = pattern.matcher(text);
      if (m.find())
        text = m.replaceAll(regex[cnt++][1]);
    }
    return text;
  }
}
