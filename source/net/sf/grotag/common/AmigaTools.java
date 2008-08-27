package net.sf.grotag.common;

import java.io.File;

import net.sf.grotag.common.AmigaPathList.AmigaPathFilePair;

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

    private AmigaTools() {
        super();
    }

    public static final synchronized AmigaTools getInstance() {
        if (instance == null) {
            instance = new AmigaTools();
        }
        return instance;
    }

    public File getFileFor(String amigaPath, AmigaPathList amigaPaths) {
        assert amigaPath != null;
        assert amigaPaths != null;
        String currentFolderPath = System.getProperty("user.dir");
        return getFileFor(amigaPath, new File(currentFolderPath), amigaPaths);
    }

    public File getFileFor(String amigaPath, File currentFolder, AmigaPathList amigaPaths) {
        assert amigaPath != null;
        assert currentFolder != null;
        assert amigaPaths != null;

        String result = "";
        int charIndex = 0;

        int colonIndex = amigaPath.indexOf(':');
        if (colonIndex >= 0) {
            // Resolve absolute Amiga path.
            String lowerAmigaPath = amigaPath.toLowerCase();
            int amigaPathIndex = 0;
            boolean pathFound = false;
            while (!pathFound && (amigaPathIndex < amigaPaths.items().size())) {
                AmigaPathFilePair pair = amigaPaths.items().get(amigaPathIndex);
                if (lowerAmigaPath.startsWith(pair.getAmigaPath())) {
                    pathFound = true;
                } else {
                    amigaPathIndex += 1;
                }
            }
            if (pathFound) {
                AmigaPathFilePair pairFound = amigaPaths.items().get(amigaPathIndex);
                File localFolder = pairFound.getLocalFolder();
                if (localFolder != null) {
                    result = localFolder.getAbsolutePath();
                    charIndex = pairFound.getAmigaPath().length();
                } else {
                    pathFound = false;
                    amigaPaths.addUndefined(amigaPath.substring(0, colonIndex));
                }
            }
            if (!pathFound) {
                // Assign unknown Amiga paths to the temporary folder.
                result = System.getProperty("java.io.tmpdir");
                charIndex = colonIndex + 1;
            }
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
