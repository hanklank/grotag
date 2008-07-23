package net.sf.grotag.parse;

import java.io.File;

import net.sf.grotag.common.Tools;

/**
 * Item that holds a string, which originally was embedded in quotes though the
 * internal representation does remove these quotes.
 * 
 * @author Thomas Aglassinger
 */
public class StringItem extends AbstractTextItem {

    public StringItem(File newFile, int newLine, int newColumn, String newString) {
        super(newFile, newLine, newColumn);

        // Assert that the text actually is a string embedded between quotes.
        // Ensuring this is the responsibility of LineTokenizer.
        assert newString != null;
        assert newString.length() >= 2;
        assert newString.charAt(0) == '"';
        assert newString.charAt(newString.length() - 1) == '"';

        setText(newString.substring(1, newString.length() - 2));
    }

    @Override
    protected String toStringSuffix() {
        return "<string>" + Tools.getInstance().sourced(getText());
    }
}
