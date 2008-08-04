package net.sf.grotag.common;

import java.io.File;

/**
 * Amiga related tools.
 * 
 * @author Thomas Aglassinger
 */
public class AmigaTools {
    /**
     * The character encoding used by text files under AmigaOS.
     */
    public static final String ENCODING = "ISO-8859-1";

    private static AmigaTools instance;

    private Tools tools;

    private AmigaTools() {
        tools = Tools.getInstance();
    }

    public static final synchronized AmigaTools getInstance() {
        if (instance == null) {
            instance = new AmigaTools();
        }
        return instance;
    }

    public File getFileFor(String amigaPath) {
        String currentFolderPath = System.getProperty("user.dir");
        return getFileFor(amigaPath, new File(currentFolderPath));
    }

    public File getFileFor(String amigaPath, File currentFolder) {
        String result = "";
        int charIndex = 0;

        int colonIndex = amigaPath.indexOf(':');
        if (colonIndex >= 0) {
            // FIXME: Implement absolute Amiga paths.
            charIndex = colonIndex + 1;
            assert false : "cannot resolve absolute Amiga path: " + tools.sourced(amigaPath);
        } else {
            charIndex = 0;
        }

        if (result.length() == 0) {
            result = currentFolder.getAbsolutePath();
        } else {
            result = new File(result).getAbsolutePath();
        }
        result += File.separator;

        // Resolve leading slashes.
        while ((charIndex < amigaPath.length()) && (amigaPath.charAt(charIndex) == '/')) {
            result = new File(result).getParent();
            charIndex += 1;
        }

        // Resolve double slashes within path.
        boolean lastWasSlash = false;
        while (charIndex < amigaPath.length()) {
            char ch = amigaPath.charAt(charIndex);
            if (ch == '/') {
                if (lastWasSlash) {
                    result = new File(result).getParent();
                } else {
                    lastWasSlash = true;
                }
            } else {
                if (lastWasSlash) {
                    result += File.separator;
                    lastWasSlash = false;
                }
                result += ch;
            }
            charIndex += 1;
        }

        // Amiga path terminates with slash.
        if (lastWasSlash) {
            result = new File(result).getParent();
        }

        // TODO: Adjust upper/lower case according to folders and files in local
        // file system.
        return new File(result);
    }

    public String escapedForAmigaguide(String some) {
        assert some != null;
        String result = "";
        for (int charIndex = 0; charIndex < some.length(); charIndex += 1) {
            char ch = some.charAt(charIndex);
            if (ch == '\\') {
                result += "\\\\";
            } else if (ch == '@') {
                result += "\\@";
            } else {
                result += ch;
            }
        }
        return result;
    }

}
