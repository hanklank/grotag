package net.sf.grotag.parse;

import java.io.File;

/**
 * Item representing a new line of text. This is particular important for
 * paragraphs in <code>@@smartwrap</code> or <code>@@worddrap</code>.
 * @author Thomas Aglassinger
 * 
 */
public class NewLineItem extends AbstractItem {
    public NewLineItem(File newFile, int newLine, int newColumn) {
        super(newFile, newLine, newColumn);
    }

    @Override
    protected String toStringSuffix() {
        return "<newline>";
    }

    @Override
    public String toPrettyAmigaguide() {
        return "\n";
    }
}
