package net.sf.grotag.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Tools {
    private static final String LOGGING_PROPERTIES = "logging.properties";
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
        // Attempt to setup logging.
        String userDir = System.getProperty("user.dir");
        String userHome = System.getProperty("user.home");
        File[] possibleLoggingSetupFiles = new File[] { new File(userDir, LOGGING_PROPERTIES),
                new File(new File(userHome, ".grotag"), LOGGING_PROPERTIES) };
        boolean loggingSetup = false;
        int fileIndex = 0;

        while (!loggingSetup && (fileIndex < possibleLoggingSetupFiles.length)) {
            File loggingSetupFilePath = possibleLoggingSetupFiles[fileIndex];
            try {
                FileInputStream in = new FileInputStream(loggingSetupFilePath);
                try {
                    LogManager.getLogManager().readConfiguration(in);
                    Logger.getLogger(Tools.class.getName())
                            .info("setup loggers from: \"" + loggingSetupFilePath + "\"");
                } finally {
                    in.close();
                }
            } catch (FileNotFoundException errorToIgnore) {
                // Ignore that optional logging setup file could not be found.
            } catch (IOException error) {
                Logger.getLogger(Tools.class.getName()).severe(
                        "cannot read \"" + loggingSetupFilePath + "" + "\": " + error.getMessage());
            }
            fileIndex += 1;
        }

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

    /**
     * Copy of <code>some</code> with any trailing white space removed.
     */
    public String withoutTrailingWhiteSpace(String some) {
        assert some != null;

        int i = some.length() - 1;
        while ((i >= 0) && (Character.isWhitespace(some.charAt(i)))) {
            i -= 1;
        }
        return some.substring(0, i + 1);
    }

    /**
     * Copy of <code>some</code> with any whitespace removed.
     */
    public String withoutWhiteSpace(String some) {
        assert some != null;
        String result = "";

        for (char ch : some.toCharArray()) {
            if (!Character.isWhitespace(ch)) {
                result += ch;
            }
        }
        return result;
    }

    /**
     * Same as <code>sourced(String)</code>, but uses absolute file path.
     * 
     * @see #sourced(String)
     */
    public/* @ pure @ */String sourced(/* @ nullable @ */File some) {
        String result;

        if (some == null) {
            result = sourced((String) null);
        } else {
            result = sourced(some.getAbsolutePath());
        }
        return result;
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
            StringBuffer buffer = new StringBuffer(some.length() + ESCAPE_SLACK_COUNT);

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

    public String[] getToken(String line, int startColumn, String continuingChars) {
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
            while ((column < line.length()) && Character.isWhitespace(line.charAt(column))) {
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
                    while ((column < line.length()) && (tokenChars.indexOf(line.charAt(column)) >= 0)) {
                        token += line.charAt(column);
                        column += 1;
                    }
                }
                result = new String[] { space, token };
            }
        }
        return result;
    }

    public String getRelativePath(File baseDir, File fileInBaseDir) {
        String basePath = baseDir.getAbsolutePath();
        String filePath = fileInBaseDir.getAbsolutePath();

        assert filePath.startsWith(basePath) : "file " + sourced(filePath) + " must start with " + sourced(baseDir);
        String result = filePath.substring(basePath.length() + 1);

        return result;
    }

    public String[] getRelativePaths(File baseDir, File[] filesInBaseDir) {
        String[] result = new String[filesInBaseDir.length];

        for (int i = 0; i < filesInBaseDir.length; i += 1) {
            result[i] = getRelativePath(baseDir, filesInBaseDir[i]);
        }
        return result;
    }

    /**
     * Get the (lower case) last suffix of name (without the "."), for example:
     * "hugo.tar.gz" yields "gz".
     */
    public String getSuffix(File file) {
        assert file != null;
        return getSuffix(file.getName());
    }

    /**
     * Get the (lower case) last suffix of name (without the "."), for example:
     * "hugo.tar.gz" yields "gz".
     */
    public String getSuffix(String name) {
        assert name != null;
        String result;
        int lastDotIndex = name.lastIndexOf('.');
        int lastSeparatorIndex = name.lastIndexOf(File.separator);

        if ((lastDotIndex < lastSeparatorIndex) || (lastDotIndex == -1)) {
            result = "";
        } else {
            result = name.substring(lastDotIndex + 1).toLowerCase();
        }
        return result;
    }

    public String getWithoutLastSuffix(String fileName) {
        assert fileName != null;
        String result;
        String suffix = getSuffix(fileName);
        int length = fileName.length();

        if (suffix.length() == 0) {
            if ((length > 0) && (fileName.charAt(length - 1) == '.')) {
                result = fileName.substring(0, length - 1);
            } else {
                result = fileName;
            }
        } else {
            result = fileName.substring(0, length - suffix.length() - 1);
        }
        return result;
    }

    private char separatorChar(String possibleSeparatedText) {
        assert possibleSeparatedText != null;
        char result;
        if (possibleSeparatedText.indexOf(';') >= 0) {
            result = ';';
        } else if (possibleSeparatedText.indexOf(',') >= 0) {
            result = ',';
        } else {
            result = 0;
        }
        return result;
    }

    public String cutOffAt(String text, char charToCutOffAt) {
        assert text != null;
        String result;
        int cutOffIndex = text.indexOf(charToCutOffAt);
        if (cutOffIndex >= 0) {
            result = text.substring(0, cutOffIndex);
        } else {
            result = text;
        }
        return result;
    }

    public String[] separated(String possibleSeparatedText) {
        String[] result;
        if (possibleSeparatedText != null) {
            char separator = separatorChar(possibleSeparatedText);
            if (separator != 0) {
                result = possibleSeparatedText.split("" + separator);
            } else {
                result = new String[] { possibleSeparatedText };
            }
            for (int i = 0; i < result.length; i += 1) {
                result[i] = result[i].trim();
                // TODO: Remove empty items.
            }
            // Detect if result actually is empty.
            if (result.length == 0) {
                result = null;
            } else if ((result.length == 1) && (result[0].length() == 0)) {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }
}
