package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.functions.Func1;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Misc. utils. All methods don't use Android specific stuff to use these methods in plain JUnit tests.
 */
public final class TextUtils {

    /**
     * a Collator instance appropriate for comparing strings using the default locale while ignoring the casing
     */
    public static final Collator COLLATOR = getCollator();

    private static final Pattern PATTERN_REMOVE_NONPRINTABLE = Pattern.compile("\\p{Cntrl}");

    private static final Pattern PATTERN_REMOVE_SPECIAL = Pattern.compile("[^a-z0-9]");


    /**
     * Internal cache for created Patterns (avoids parsing them unnecessarily often)
     */
    private static final Map<String, Pattern> PATTERN_CACHE = Collections.synchronizedMap(new HashMap<>());

    private TextUtils() {
        // utility class
    }

    public static List<String> sortListLocaleAware(final List<String> listToSort) {
        return sortListLocaleAware(listToSort, s -> s);
    }

    public static <T> List<T> sortListLocaleAware(final List<T> listToSort, final Func1<T, String> sortStringAccessor) {
        Collections.sort(listToSort, (e1, e2) -> COLLATOR.compare(sortStringAccessor.call(e1), sortStringAccessor.call(e2)));
        return listToSort;
    }

    /**
     * Searches for the pattern pattern in the data. If the pattern is not found defaultValue is returned
     *
     * @param data
     *            Data to search in
     * @param pattern
     *            Pattern to search for
     * @param trim
     *            Set to true if the group found should be trim'ed
     * @param group
     *            Number of the group to return if found
     * @param defaultValue
     *            Value to return if the pattern is not found
     * @param last
     *            Find the last occurring value
     * @return defaultValue or the n-th group if the pattern matches (trimmed if wanted)
     */
    @SuppressWarnings("RedundantStringConstructorCall")
    @SuppressFBWarnings("DM_STRING_CTOR")
    public static String getMatch(@Nullable final String data, final Pattern pattern, final boolean trim, final int group, final String defaultValue, final boolean last) {
        if (data != null) {
            final Matcher matcher = pattern.matcher(data);
            if (matcher.find()) {
                String result = matcher.group(group);
                while (last && matcher.find()) {
                    result = matcher.group(group);
                }

                if (result != null) {
                    final Matcher remover = PATTERN_REMOVE_NONPRINTABLE.matcher(result);
                    final String untrimmed = remover.replaceAll(" ");

                    // Some versions of Java copy the whole page String, when matching with regular expressions
                    // later this would block the garbage collector, as we only need tiny parts of the page
                    // see http://developer.android.com/reference/java/lang/String.html#backing_array
                    // Thus the creation of a new String via String constructor is voluntary here!!
                    // And BTW: You cannot even see that effect in the debugger, but must use a separate memory profiler!
                    return trim ? new String(untrimmed).trim() : new String(untrimmed);
                }
            }
        }

        return defaultValue;
    }

    /**
     * Searches for the pattern pattern in the data. If the pattern is not found defaultValue is returned
     *
     * @param data
     *            Data to search in
     * @param pattern
     *            Pattern to search for
     * @param trim
     *            Set to true if the group found should be trim'ed
     * @param defaultValue
     *            Value to return if the pattern is not found
     * @return defaultValue or the first group if the pattern matches (trimmed if wanted)
     */
    public static String getMatch(final String data, final Pattern pattern, final boolean trim, final String defaultValue) {
        return getMatch(data, pattern, trim, 1, defaultValue, false);
    }

    /**
     * Searches for the pattern pattern in the data. If the pattern is not found defaultValue is returned
     *
     * @param data
     *            Data to search in
     * @param pattern
     *            Pattern to search for
     * @param defaultValue
     *            Value to return if the pattern is not found
     * @return defaultValue or the first group if the pattern matches (trimmed)
     */
    public static String getMatch(@Nullable final String data, final Pattern pattern, final String defaultValue) {
        return getMatch(data, pattern, true, 1, defaultValue, false);
    }

    /**
     * Searches for the pattern pattern in the data.
     *
     * @return true if data contains the pattern pattern
     */
    public static boolean matches(final String data, final Pattern pattern) {
        // matcher is faster than String.contains() and more flexible - it takes patterns instead of fixed texts
        return data != null && pattern.matcher(data).find();

    }

    /**
     * Replaces every \n, \r and \t with a single space. Afterwards multiple spaces
     * are merged into a single space. Finally leading spaces are deleted.
     *
     * This method must be fast, but may not lead to the shortest replacement String.
     *
     * You are only allowed to change this code if you can prove it became faster on a device.
     * see cgeo.geocaching.test.WhiteSpaceTest#replaceWhitespaceManually in the test project.
     *
     * @param data
     *            complete HTML page
     * @return the HTML page as a very long single "line"
     */
    public static String replaceWhitespace(final String data) {
        final int length = data.length();
        final char[] chars = new char[length];
        data.getChars(0, length, chars, 0);
        int resultSize = 0;
        boolean lastWasWhitespace = true;
        for (final char c : chars) {
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                if (!lastWasWhitespace) {
                    chars[resultSize++] = ' ';
                }
                lastWasWhitespace = true;
            } else {
                chars[resultSize++] = c;
                lastWasWhitespace = false;
            }
        }
        return String.valueOf(chars, 0, resultSize);
    }

    /**
     * @param str input string
     *            As of performance reasons we non't use a REGEX here. Don't use this function for strings which could contain new-line characters like "\r\n" or "\r"
     * @return normalized String Length like it is counted at the gc website (count UNIX new-line character "\n" as two characters)
     */
    public static int getNormalizedStringLength (@NonNull final String str) {
        final String newStr = str.trim();
        return StringUtils.countMatches(newStr, "\n") + newStr.length();
    }

    /**
     * Quick and naive check for possible rich HTML content in a string.
     *
     * @param str
     *            A string containing HTML code.
     * @return <tt>true</tt> if <tt>str</tt> contains HTML code that needs to go through a HTML renderer before
     *         being displayed, <tt>false</tt> if it can be displayed as-is without any loss
     */
    public static boolean containsHtml(final String str) {
        return str.indexOf('<') != -1 || str.indexOf('&') != -1;
    }

    /**
     * Remove all control characters (which are not valid in XML or HTML), as those should not appear in cache texts
     * anyway
     *
     */
    public static String removeControlCharacters(final String input) {
        final Matcher remover = PATTERN_REMOVE_NONPRINTABLE.matcher(input);
        return remover.replaceAll(" ").trim();
    }

    /**
     * Calculate a simple checksum for change-checking (not usable for security/cryptography!)
     *
     * @param input
     *            String to check
     * @return resulting checksum
     */
    public static long checksum(final String input) {
        final CRC32 checksum = new CRC32();
        checksum.update(input.getBytes(StandardCharsets.UTF_8));
        return checksum.getValue();
    }

    /**
     * Build a Collator instance appropriate for comparing strings using the default locale while ignoring the casing.
     *
     * @return a collator
     */
    private static Collator getCollator() {
        final Collator collator = Collator.getInstance();
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        collator.setStrength(Collator.TERTIARY);
        return collator;
    }

    /**
     * When converting html to text using {@link HtmlCompat#fromHtml(String, int)} then the result often contains unwanted trailing
     * linebreaks (from the conversion of paragraph tags). This method removes those.
     */
    public static CharSequence trimSpanned(final Spanned source) {
        final int length = source.length();
        int i = length;

        // loop back to the first non-whitespace character
        //noinspection StatementWithEmptyBody
        while (--i >= 0 && Character.isWhitespace(source.charAt(i))) {
        }

        if (i < length - 1) {
            return source.subSequence(0, i + 1);
        }
        return source;
    }

    /**
     * Convert a potentially HTML string into a plain-text one. If the string does not contain HTML markup,
     * it is returned unchanged.
     *
     * @param html a string containing either HTML or plain text
     * @return a string without any HTML markup
     */
    public static String stripHtml(final String html) {
        return containsHtml(html) ? trimSpanned(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)).toString() : html;
    }

    public static SpannableString coloredCacheText(@NonNull final Geocache cache, @NonNull final String text) {
        final SpannableString span = new SpannableString(text);
        if (cache.isDisabled() || cache.isArchived()) { // strike
            span.setSpan(new StrikethroughSpan(), 0, span.toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (cache.isArchived()) {
            span.setSpan(new ForegroundColorSpan(ContextCompat.getColor(CgeoApplication.getInstance(), R.color.archived_cache_color)), 0, span.toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return span;
    }

    @NonNull
    public static String getTextBeforeIndexUntil(final String text, final int idx, final String startToken) {
        return getTextBeforeIndexUntil(text, idx, startToken, -1);
    }

    @NonNull
    public static String getTextAfterIndexUntil(final String text, final int idx, final String endToken) {
        return getTextAfterIndexUntil(text, idx, endToken, -1);
    }

    /**
     * Returns substring of text before (and excluding) given 'idx' until one of given conditions is met
     *
     * @param text       text to work on
     * @param idx        idx to start checking
     * @param startToken if not null, return text until (and excluding) this token is found
     * @param maxLength  if >=0, text is truncated to given max length
     * @return found text or empty string. Never null.
     */
    @NonNull
    public static String getTextBeforeIndexUntil(final String text, final int idx, final String startToken, final int maxLength) {
        if (StringUtils.isEmpty(text) || idx <= 0) {
            return "";
        }
        String before = text.substring(0, Math.min(idx, text.length()));

        if (StringUtils.isNotEmpty(startToken)) {
            final int tokenIdx = before.lastIndexOf(startToken);
            if (tokenIdx >= 0) {
                before = before.substring(tokenIdx + startToken.length());
            }
        }
        return (maxLength >= 0 && before.length() > maxLength) ? before.substring(before.length() - maxLength) : before;
    }

    /**
     * Returns substring of text after (and excluding) given 'idx' until one of given conditions is met
     *
     * @param text      text to work on
     * @param idx       idx to start checking
     * @param endToken  if not null, return text until (and excluding) this token is found
     * @param maxLength if >=0, text is truncated to given max length
     * @return found text or empty string. Never null.
     */
    @NonNull
    public static String getTextAfterIndexUntil(final String text, final int idx, final String endToken, final int maxLength) {
        if (StringUtils.isEmpty(text) || idx >= text.length() - 1) {
            return "";
        }
        String after = text.substring(idx < 0 ? 0 : idx + 1);
        if (StringUtils.isNotEmpty(endToken)) {
            final int tokenIdx = after.indexOf(endToken);
            if (tokenIdx >= 0) {
                after = after.substring(0, tokenIdx);
            }
        }
        return (maxLength >= 0 && after.length() > maxLength) ? after.substring(0, maxLength) : after;
    }

    /**
     * Tries to find the next value in 'text' which is delimited (beginning and end) with given ÄdelimiterChar and
     * returns text inside these delimiters. In this text, occurences of both 'escapedChar' or 'delimiterChar' can
     * be escaped with 'escapedChar' to get them into the indelimited text.
     * Method returns null if no delimited value is found.
     * This is the 'inverse' function to {@link #createDelimitedValue(String, char, char)}
     */
    public static String parseNextDelimitedValue(@NonNull final String text, final char delimiterChar, final char escapeChar) {
        final String quotedDelim = "\\" + delimiterChar;
        final String quotedEsc = "\\" + escapeChar;

        final Pattern findNextPattern =
                compilePattern("(?s)" + quotedDelim +
                        "((?:" + quotedEsc + ".|" +
                        "[^" + quotedEsc + quotedDelim + "]++)*)" + quotedDelim);

        final Matcher m = findNextPattern.matcher(text);
        if (m.find()) {
            return compilePattern(quotedEsc + "(.)").matcher(m.group(1)).replaceAll("$1");
        }
        return null;
    }

    /**
     * Returns a delimited version of given 'text' using given 'delimiterChar'. Occurences of 'delimiterChar' are
     * escaped with given 'escapeChar'. Occurences of 'escapedChar' are also escaped with 'escapedChar'.
     * This is the 'inverse' function to {@link #parseNextDelimitedValue(String, char, char)}
     */
    @NonNull
    public static String createDelimitedValue(@NonNull final String text, final char delimiterChar, final char escapeChar) {
        final String quotedDelim = "\\" + delimiterChar;
        final String quotedEsc = "\\" + escapeChar;
        return delimiterChar + compilePattern("([" + quotedDelim + quotedEsc + "])").matcher(text).replaceAll(quotedEsc + "$1") + delimiterChar;
    }

    /**
     * Returns text split into its single words (a word being a continuous group of non-whitespace-characters).
     * Leadig/trailing whitespaces are omitted. blank string (or null string) results in empty array.
     *
     * @param text text to split
     * @return splited into words
     */
    @NonNull
    public static String[] getWords(final String text) {
        final String theText = text == null ? "" : text.trim();
        if (theText.isEmpty()) {
            return new String[0];
        }
        return compilePattern("\\s+").split(theText);
    }

    /**
     * Replaces all occurences of texts starting with 'startToken' and ending with 'endTOken' with given 'replacement'.
     * Note that 'replacement' is interpreted as regex as defined in {@link String#replaceAll(String, String)}}.
     * In replacmenent, '$1' may be used to reference the replaced text inside the tokens
     * it is assured that for same parameters, matches are always the same as in {@link #getAll(String, String, String)}.
     *
     * @param text        text to do replacement in
     * @param startToken  starttoken. if blank then "starttoken" is assumed to be start of text
     * @param endToken    starttoken. if blank then "endtoken" is assumed to be end of text
     * @param replacement replacements
     * @return text with replacements
     */
    @NonNull
    public static String replaceAll(final String text, final String startToken, final String endToken, final String replacement) {
        if (text == null) {
            return "";
        }
        return getTokenSearchPattern(startToken, endToken).matcher(text).replaceAll(replacement);
    }

    /**
     * Gets all text occurences starting with 'startToken' and ending with 'endToken'.
     * it is assured that for same parameters, matches are always the same as in {@link #replaceAll(String, String, String, String)}.
     *
     * @param text       text to search in
     * @param startToken starttoken. if blank then "starttoken" is assumed to be start of text
     * @param endToken   starttoken. if blank then "endtoken" is assumed to be end of text
     * @return array of found matches
     */
    @NonNull
    public static List<String> getAll(final String text, final String startToken, final String endToken) {
        if (text == null) {
            return Collections.emptyList();
        }
        final Matcher m = getTokenSearchPattern(startToken, endToken).matcher(text);
        final List<String> result = new ArrayList<>();
        while (m.find()) {
            result.add(m.group(1));
        }
        return result;
    }

    /**
     * Shortens a given text to a maximum given number of characters. In case the text is too long it
     * is shortened according to a given begin-end-distribution-value. Deleted text part is marked with '...'
     * @param text text to shorten. If text is shorter than maxLength it remains unchanged
     * @param maxLength maxLength to shorten text to.
     * @param beginEndDistribution begin-end-distribution to obey on shortening. If >=1 then text is shortened at the end.
     *      If <=0 then text is shortened at the beginning. If between 0-1 then text is shortened at beginning and end in relation to this value.
     * @return the shortened text
     */
    @NonNull
    public static String shortenText(final String text, final int maxLength, final float beginEndDistribution) {
        final String separator = "...";
        if (StringUtils.isBlank(text) || maxLength < 0) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        if (maxLength < separator.length()) {
            return text.substring(0, maxLength);
        }
        final int charsAtBegin = Math.max(0, Math.min(maxLength - separator.length(), (int) ((maxLength - separator.length()) * beginEndDistribution)));
        final int charsAtEnd = maxLength - separator.length() - charsAtBegin;

        return text.substring(0, charsAtBegin) + separator + text.substring(text.length() - charsAtEnd);
    }

    public static boolean isLetterOrDigit(final char ch, final boolean useUpper) {
        boolean returnValue = CharUtils.isAsciiAlphanumeric(ch);
        if (useUpper) {
            returnValue &= CharUtils.isAsciiAlphaUpper(ch);
        } else {
            returnValue &= CharUtils.isAsciiAlphaLower(ch);
        }

        return returnValue;
    }

    public static boolean isEqualIgnoreCaseAndSpecialChars(final String s1, final String s2) {
        if (Objects.equals(s1, s2)) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        return toComparableStringIgnoreCaseAndSpecialChars(s1).equals(toComparableStringIgnoreCaseAndSpecialChars(s2));
    }

    public static String toComparableStringIgnoreCaseAndSpecialChars(final String value) {
        if (value == null) {
            return null;
        }
        return PATTERN_REMOVE_SPECIAL.matcher(value.toLowerCase(Locale.US)).replaceAll("");
    }

    public static <E extends Enum<E>> E getEnumIgnoreCaseAndSpecialChars(final Class<E> enumClass, final String enumName, final E defaultEnum) {
        if (enumName == null || !enumClass.isEnum()) {
            return defaultEnum;
        }
        for (final E each : enumClass.getEnumConstants()) {
            if (isEqualIgnoreCaseAndSpecialChars(each.name(), enumName)) {
                return each;
            }
        }
        return defaultEnum;
    }

    private static Pattern getTokenSearchPattern(final String startToken, final String endToken) {
        return compilePattern("(?s)" + (StringUtils.isEmpty(startToken) ? "^" : Pattern.quote(startToken)) + "(.*?)" +
                (StringUtils.isEmpty(endToken) ? "$" : Pattern.quote(endToken)));
    }

    private static Pattern compilePattern(final String patternString) {

        Pattern pattern = PATTERN_CACHE.get(patternString);
        if (pattern == null) {
            pattern = Pattern.compile(patternString);
            PATTERN_CACHE.put(patternString, pattern);
        }
        return pattern;
    }

}
