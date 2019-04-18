/*  Journal App for Android
 *  Copyright (C) 2019 John Wiley & Sons, Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wiley.android.journalApp.html;

import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import com.wiley.android.journalApp.utils.AssetsUtils;
import com.wiley.wol.client.android.log.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.XMLReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Created by Andrey Rylov on 12/05/14.
 */
public final class HtmlUtils {
    private HtmlUtils() {
    }

    public static String getAssetsHtmlUrl(String path) {
        return "file:///android_asset/" + getAssetsHtmlFilename(path);
    }

    public static boolean hasAssetsHtmlUrl(Context context, String path) {
        String filename = getAssetsHtmlFilename(path);
        return AssetsUtils.existsFileInAssets(context, filename);
    }

    public static String getAssetsHtmlFilename(String path) {
        return "html/" + path;
    }

    public static String getAssetsImgUrl(String img) {
        return getAssetsHtmlUrl("Graphics/Shared/" + img);
    }

    public static String stripHtml(String source) {
        return source.replaceAll("<[^>]+>", "").replaceAll("\\\\s*\\n\\\\s*\\n\\\\s*", "\n");
    }

    /**
     * @see "https://bitbucket.org/Kuitsi/android-textview-html-list/"
     */
    public static class TagHandlerListSupport implements Html.TagHandler {
        /**
         * Keeps track of lists (ol, ul). On bottom of Stack is the outermost list
         * and on top of Stack is the most nested list
         */
        Stack<String> lists = new Stack<>();
        /**
         * Tracks indexes of ordered lists so that after a nested list ends
         * we can continue with correct index of outer list
         */
        Stack<Integer> olNextIndex = new Stack<>();
        /**
         * List indentation in pixels. Nested lists use multiple of this.
         */
        private static final int indent = 10;
        private static final int listItemIndent = indent * 2;
        private static final BulletSpan bullet = new BulletSpan(indent);

        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if ("ul".equalsIgnoreCase(tag)) {
                if (opening) {
                    lists.push(tag);
                } else {
                    lists.pop();
                }
            } else if ("ol".equalsIgnoreCase(tag)) {
                if (opening) {
                    lists.push(tag);
                    olNextIndex.push(1);//TODO: add support for lists starting other index than 1
                } else {
                    lists.pop();
                    olNextIndex.pop();
                }
            } else if ("li".equalsIgnoreCase(tag)) {
                if (opening) {
                    if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                        output.append("\n");
                    }
                    String parentList = lists.peek();
                    if ("ol".equalsIgnoreCase(parentList)) {
                        start(output, new Ol());
                        output.append(olNextIndex.peek().toString()).append(". ");
                        olNextIndex.push(olNextIndex.pop() + 1);
                    } else if ("ul".equalsIgnoreCase(parentList)) {
                        start(output, new Ul());
                    }
                } else {
                    if ("ul".equalsIgnoreCase(lists.peek())) {
                        if (output.charAt(output.length() - 1) != '\n') {
                            output.append("\n");
                        }
                        // Nested BulletSpans increases distance between bullet and text, so we must prevent it.
                        int bulletMargin = indent;
                        if (lists.size() > 1) {
                            bulletMargin = indent - bullet.getLeadingMargin(true);
                            if (lists.size() > 2) {
                                // This get's more complicated when we add a LeadingMarginSpan into the same line:
                                // we have also counter it's effect to BulletSpan
                                bulletMargin -= (lists.size() - 2) * listItemIndent;
                            }
                        }
                        BulletSpan newBullet = new BulletSpan(bulletMargin);
                        end(output,
                                Ul.class,
                                new LeadingMarginSpan.Standard(listItemIndent * (lists.size() - 1)),
                                newBullet);
                    } else if ("ol".equalsIgnoreCase(lists.peek())) {
                        if (output.charAt(output.length() - 1) != '\n') {
                            output.append("\n");
                        }
                        int numberMargin = listItemIndent * (lists.size() - 1);
                        if (lists.size() > 2) {
                            // Same as in ordered lists: counter the effect of nested Spans
                            numberMargin -= (lists.size() - 2) * listItemIndent;
                        }
                        end(output,
                                Ol.class,
                                new LeadingMarginSpan.Standard(numberMargin));
                    }
                }
            } else {
                if (opening) {
                    Logger.d("TagHandler", "Found an unsupported tag " + tag);
                }
            }
        }

        private static void start(Editable text, Object mark) {
            int len = text.length();
            text.setSpan(mark, len, len, Spanned.SPAN_MARK_MARK);
        }

        private static void end(Editable text, Class<?> kind, Object... replaces) {
            int len = text.length();
            Object obj = getLast(text, kind);
            int where = text.getSpanStart(obj);
            text.removeSpan(obj);
            if (where != len) {
                for (Object replace : replaces) {
                    text.setSpan(replace, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        private static Object getLast(Spanned text, Class<?> kind) {
        /*
		 * This knows that the last returned object from getSpans()
		 * will be the most recently added.
		 */
            Object[] objs = text.getSpans(0, text.length(), kind);
            if (objs.length == 0) {
                return null;
            }
            return objs[objs.length - 1];
        }

        private static class Ul {
        }

        private static class Ol {
        }

    }

    public static String detectLinks(final String html) {
        final Document doc = Jsoup.parse(html);
        detectLinks(doc.children());

        return doc.outerHtml();
    }

    private static void detectLinks(final Elements allElements) {
        for (Element element : allElements) {
            if ("a".equals(element.tagName())) {
                continue;
            }

            final Map<String, String> urlsMap = new HashMap<>();
            final String text = element.ownText();
            final Spannable sp = new SpannableString(Html.fromHtml(text));

            Linkify.addLinks(sp, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

            final URLSpan[] spans = sp.getSpans(0, sp.length(), URLSpan.class);
            for (URLSpan urlSpan : spans) {
                final String url = urlSpan.getURL().replace("mailto:", "");
                final String urlToReplace = "<a href=" + urlSpan.getURL() + ">" + url + "</a>";
                urlsMap.put(url, urlToReplace);
            }

            for (Map.Entry<String, String> entry : urlsMap.entrySet()) {
                element.html(element.html().replaceAll(entry.getKey(), entry.getValue()));
            }

            if (element.children().size() > 0) {
                detectLinks(element.children());
            }
        }
    }

}
