package net.sf.grotag.common;

import java.util.Map;
import java.util.TreeMap;

public class Tools {
    private static final String DEFAULT_TOKEN_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";

    /**
     * Size of slack in buffer escaped characters get encoded to.
     */
    private static final int ESCAPE_SLACK_COUNT = 16;
    private static final int UNICODE_HEX_DIGIT_COUNT = 4;

    private Map<Character, String> escapeMap;

    private static Tools instance;

    public static final synchronized Tools getInstance() {
        if (instance == null) {
            instance = new Tools();
        }
        return instance;
    }

    private Tools() {
        escapeMap = new TreeMap<Character, String>();
        escapeMap.put(new Character('\"'), "\\\"");
        escapeMap.put(new Character('\''), "\\\'");
        escapeMap.put(new Character('\\'), "\\\\");
        escapeMap.put(new Character('\b'), "\\b");
        escapeMap.put(new Character('\f'), "\\f");
        escapeMap.put(new Character('\n'), "\\n");
        escapeMap.put(new Character('\r'), "\\r");
        escapeMap.put(new Character('\t'), "\\t");
    }

    private boolean isEscapable(Character some) {
        return escapeMap.containsKey(some);
    }

    /** Copy of <code>some</code> with any trailing white space removed. */
    public String withoutTrailingWhiteSpace(String some) {
        assert some != null;

        int i = some.length() - 1;
        while ((i >= 0) && (Character.isWhitespace(some.charAt(i)))) {
            i -= 1;
        }
        return some.substring(0, i + 1);
    }

    /**
     * Source code version of <code>some</code> that can be pasted into a Java
     * source. The result is embedded in two quotes, escape characters are
     * rendered where possible. Invisible characters are rendered as unicode
     * escape. The value <code>null</code> results in the the text "null"
     * (without quotes).
     */
    public/* @ pure @ */String sourced(/* @ nullable @ */String some) {
        String result;

        if (some == null) {
            result = "null";
        } else {
            StringBuffer buffer = new StringBuffer(some.length()
                    + ESCAPE_SLACK_COUNT);

            buffer.append('\"');
            for (int i = 0; i < some.length(); i += 1) {
                char c = some.charAt(i);
                Character cAsCharacter = new Character(c);
                String escaped = null;

                if (isEscapable(cAsCharacter)) {
                    escaped = escapeMap.get(cAsCharacter);
                } else if (c < ' ') {
                    escaped = hexString(c, UNICODE_HEX_DIGIT_COUNT, "\\u");
                }
                if (escaped == null) {
                    buffer.append(c);
                } else {
                    buffer.append(escaped);
                }
            }
            buffer.append('\"');
            result = buffer.toString();
        }
        return result;
    }

    /**
     * Source code version of character <code>some</code> that can be pasted
     * into a Java source. The result is embedded in two quotes, escape
     * characters are rendered where possible. Invisible characters are rendered
     * as unicode escape.
     */
    public/* @ pure @ */String sourced(char some) {
        return sourced(Character.toString(some));
    }

    /**
     * Hex representation of <code>value</code>.
     * 
     * @param digits
     *                the number of digits the result should have at least; if
     *                necessary, leading "0"s are prepended
     * @param prefix
     *                the text to be used as the fist few characters of the
     *                result; "0x" if null.
     */
    // @ requires digits > 0;
    // @ requires digits <= MAX_HEX_DIGIT_COUNT;
    public/* @ pure @ */String hexString(long value, int digits, /*
                                                                     * @
                                                                     * nullable @
                                                                     */
    String prefix) {
        String result = Long.toHexString(value);
        String actualPrefix;

        if (prefix == null) {
            actualPrefix = "0x";
        } else {
            actualPrefix = prefix;
        }

        if (result.length() < digits) {
            String zeros = "000000000000000";

            // @ assert zeros.length() == (MAX_HEX_DIGIT_COUNT - 1);
            result = zeros.substring(result.length() - 1, digits - 1) + result;
        }
        result = actualPrefix + result;
        return result;
    }

    /**
     * Hex representation of <code>value</code>, prefixed with "0x".
     * 
     * @param digits
     *                the number of digits the result should have at least; if
     *                necessary, leading "0"s are prepended
     */
    // @ requires digits > 0;
    // @ ensures \result.length() >= (2 + digits);
    public/* @ pure @ */String hexString(long value, int digits) {
        return hexString(value, digits, null);
    }

    public String[] getToken(String line, int startColumn) {
        return getToken(line, startColumn, null);
    }

    public String[] getToken(String line, int startColumn,
            String continuingChars) {
        assert line != null;
        assert startColumn <= line.length();

        String[] result = null;
        String tokenChars = continuingChars;
        String space = null;
        String token = null;
        int column = startColumn;

        while ((column < line.length()) && (result == null)) {
            // Skip white space
            space = "";
            while ((column < line.length())
                    && Character.isWhitespace(line.charAt(column))) {
                space += line.charAt(column);
                column += 1;
            }

            if (tokenChars == null) {
                tokenChars = DEFAULT_TOKEN_CHARS;
            }
            
            if (column < line.length()) {
                char firstChar = line.charAt(column);
                
                token = "" + firstChar;
                if (tokenChars.indexOf(firstChar) >= 0) {
                    column += 1;
                    while ((column < line.length())
                            && (tokenChars.indexOf(line.charAt(column)) >= 0)) {
                        token += line.charAt(column);
                        column += 1;
                    }
                }
                result = new String[] { space, token };
            }
        }
        return result;
    }
}
