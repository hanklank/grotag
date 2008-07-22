package net.sf.grotag.parse;

import java.io.File;

import net.sf.grotag.common.Tools;

public class StringItem extends AbstractItem {
    private String string;

    public StringItem(File newFile, int newLine, int newColumn, String newString) {
        super(newFile, newLine, newColumn);
        
        // Assert that the text actually is a string embedded between quotes.
        // Ensuring this is the responsibility of LineTokenizer. 
        assert newString != null;
        assert newString.length() >= 2;
        assert newString.charAt(0) == '"';
        assert newString.charAt(newString.length() - 1) == '"';
        
        string = newString;
    }

    /** The string text without enclosing quotes. */
    public String getString() {
        return string;
    }

    @Override
    protected String toStringSuffix() {
        return "<string>" + Tools.getInstance().sourced(getString());
    }
}
