package net.sf.grotas.parse;

import java.io.File;

/**
 * Abstract item in an Amigaguide token stream.
 * 
 * @author Thomas Aglassinger
 */
abstract class AbstractItem {
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

    abstract protected String toStringSuffix();
}
