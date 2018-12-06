package com.yxj.mqtt.utils;

import java.util.ArrayList;
import java.util.List;

public class StrUtil {
    public static boolean startWith(CharSequence str, char c) {
        return c == str.charAt(0);
    }
    public static boolean endWith(CharSequence str, char c) {
        return c == str.charAt(str.length() - 1);
    }
    public static boolean endWith(CharSequence str, CharSequence suffix) {
        return endWith(str, suffix, false);
    }
    public static boolean endWith(CharSequence str, CharSequence suffix, boolean isIgnoreCase) {
        if (null != str && null != suffix) {
            return isIgnoreCase ? str.toString().toLowerCase().endsWith(suffix.toString().toLowerCase()) : str.toString().endsWith(suffix.toString());
        } else {
            return null == str && null == suffix;
        }
    }
    public static boolean contains(CharSequence str, char searchChar) {
        return indexOf(str, searchChar) > -1;
    }
    public static int indexOf(CharSequence str, char searchChar) {
        return indexOf(str, searchChar, 0);
    }

    public static int indexOf(CharSequence str, char searchChar, int start) {
        return str instanceof String ? ((String)str).indexOf(searchChar, start) : indexOf(str, searchChar, start, -1);
    }

    public static int indexOf(CharSequence str, char searchChar, int start, int end) {
        int len = str.length();
        if (start < 0 || start > len) {
            start = 0;
        }

        if (end > len || end < 0) {
            end = len;
        }

        for(int i = start; i < end; ++i) {
            if (str.charAt(i) == searchChar) {
                return i;
            }
        }

        return -1;
    }
    public static int count(CharSequence content, char charForSearch) {
        int count = 0;
        if (isEmpty(content)) {
            return 0;
        } else {
            int contentLength = content.length();

            for(int i = 0; i < contentLength; ++i) {
                if (charForSearch == content.charAt(i)) {
                    ++count;
                }
            }

            return count;
        }
    }
    public static int count(CharSequence content, CharSequence strForSearch) {
        if (!hasEmpty(content, strForSearch) && strForSearch.length() <= content.length()) {
            int count = 0;
            int idx = 0;
            String content2 = content.toString();

            for(String strForSearch2 = strForSearch.toString(); (idx = content2.indexOf(strForSearch2, idx)) > -1; idx += strForSearch.length()) {
                ++count;
            }

            return count;
        } else {
            return 0;
        }
    }
    public static boolean hasEmpty(CharSequence... strs) {
        if (isEmpty(strs)) {
            return true;
        } else {
            CharSequence[] arr$ = strs;
            int len$ = strs.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                CharSequence str = arr$[i$];
                if (isEmpty(str)) {
                    return true;
                }
            }

            return false;
        }
    }
    public static <T> boolean isEmpty(T... array) {
        return array == null || array.length == 0;
    }
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }
    public static List<String> split(CharSequence str, char separator) {
        return split(str, separator, 0);
    }
    public static List<String> split(CharSequence str, char separator, int limit) {
        return split(str, separator, limit, false, false);
    }
    public static List<String> split(CharSequence str, char separator, int limit, boolean isTrim, boolean ignoreEmpty) {
        return (List)(null == str ? new ArrayList(0) : split(str.toString(), separator, limit, isTrim, ignoreEmpty));
    }
    public static List<String> split(String str, char separator, int limit, boolean isTrim, boolean ignoreEmpty) {
        return split(str, separator, limit, isTrim, ignoreEmpty, false);
    }
    public static List<String> split(String str, char separator, int limit, boolean isTrim, boolean ignoreEmpty, boolean ignoreCase) {
        if (StrUtil.isEmpty(str)) {
            return new ArrayList(0);
        } else if (limit == 1) {
            return addToList(new ArrayList(1), str, isTrim, ignoreEmpty);
        } else {
            ArrayList<String> list = new ArrayList(limit > 0 ? limit : 16);
            int len = str.length();
            int start = 0;

            for(int i = 0; i < len; ++i) {
                if (equals(separator, str.charAt(i), ignoreCase)) {
                    addToList(list, str.substring(start, i), isTrim, ignoreEmpty);
                    start = i + 1;
                    if (limit > 0 && list.size() > limit - 2) {
                        break;
                    }
                }
            }

            return addToList(list, str.substring(start, len), isTrim, ignoreEmpty);
        }
    }
    private static List<String> addToList(List<String> list, String part, boolean isTrim, boolean ignoreEmpty) {
        part = part.toString();
        if (isTrim) {
            part = part.trim();
        }

        if (!ignoreEmpty || !part.isEmpty()) {
            list.add(part);
        }

        return list;
    }

    public static boolean equals(char c1, char c2, boolean ignoreCase) {
        if (ignoreCase) {
            return Character.toLowerCase(c1) == Character.toLowerCase(c2);
        } else {
            return c1 == c2;
        }
    }
    public static String removeSuffix(CharSequence str, CharSequence suffix) {
        if (!isEmpty(str) && !isEmpty(suffix)) {
            String str2 = str.toString();
            return str2.endsWith(suffix.toString()) ? subPre(str2, str2.length() - suffix.length()) : str2;
        } else {
            return str(str);
        }
    }
    public static String subPre(CharSequence string, int toIndex) {
        return sub(string, 0, toIndex);
    }
    public static String sub(CharSequence str, int fromIndex, int toIndex) {
        if (isEmpty(str)) {
            return str(str);
        } else {
            int len = str.length();
            if (fromIndex < 0) {
                fromIndex += len;
                if (fromIndex < 0) {
                    fromIndex = 0;
                }
            } else if (fromIndex > len) {
                fromIndex = len;
            }

            if (toIndex < 0) {
                toIndex += len;
                if (toIndex < 0) {
                    toIndex = len;
                }
            } else if (toIndex > len) {
                toIndex = len;
            }

            if (toIndex < fromIndex) {
                int tmp = fromIndex;
                fromIndex = toIndex;
                toIndex = tmp;
            }

            return fromIndex == toIndex ? "" : str.toString().substring(fromIndex, toIndex);
        }
    }
    public static String str(CharSequence cs) {
        return null == cs ? null : cs.toString();
    }
}

