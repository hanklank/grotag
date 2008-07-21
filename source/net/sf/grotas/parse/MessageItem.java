package net.sf.grotas.parse;

import java.io.File;

/**
 * A message generated during parsing.
 * 
 * @author Thomas Aglassinger
 */
public class MessageItem implements Comparable<MessageItem> {
    private File file;
    private int line;
    private int column;
    private String text;

    public MessageItem(File newFile, int newLine, int newColumn, String newText) {
        assert newFile != null;
        assert newLine >= 0;
        assert newColumn >= 0;
        assert newText != null;
        assert newText.length() > 0;

        file = newFile;
        line = newLine;
        column = newColumn;
        text = newText;
    }

    public File getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return getFile().getName() + "[" + getLine() + ":" + getColumn()
                + "]: " + getText();
    }

    public int compareTo(MessageItem other) {
        int result;
        if (other == null) {
            result = -1;
        } else {
            result = getFile().getAbsolutePath().compareTo(
                    other.getFile().getAbsolutePath());
            if (result == 0) {
                result = getLine() - other.getLine();
                if (result == 0) {
                    result = getColumn() - other.getColumn();
                }
            }
        }
        return result;
    }
}
