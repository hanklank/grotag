package net.sf.grotag.parse;

import java.io.File;

/**
 * Abstract item in an Amigaguide token stream.
 * 
 * @author Thomas Aglassinger
 */
public abstract class AbstractItem {
    private File file;
    private int line;
    private int column;

    protected AbstractItem(File newFile, int newLine, int newColumn) {
        assert newFile != null;
        assert newLine >= 0;
        assert newColumn >= 0;

        file = newFile;
        line = newLine;
        column = newColumn;
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

    @Override
    public String toString() {
        String result = file.getName();

        result += " [" + getLine() + ":" + getColumn() + "]";
        result += toStringSuffix();
        return result;
    }

    /**
     * Pretty printed Amigaguide source code representation of the item.
     */
    abstract public String toPrettyAmigaguide();

    abstract protected String toStringSuffix();
}
