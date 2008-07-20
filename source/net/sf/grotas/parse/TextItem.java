package net.sf.grotas.parse;

import java.io.File;

import net.sf.grotas.common.Tools;

/**
 * An item representing text.
 * 
 * @author Thomas Aglassinger
 */
public class TextItem extends AbstractItem {
    private String text;
    private Tools tools;

    /**
     * Create text item from <code>newText</code>, resolving escape sequences
     * in the text.
     */
    public TextItem(File newFile, int newLine, int newColumn, String newText) {
        super(newFile, newLine, newColumn);

        boolean afterBackslash = false;
        tools = Tools.getInstance();
        text = "";
        for (int i = 0; i < newText.length(); i += 1) {
            char ch = newText.charAt(i);
            if (afterBackslash) {
                // Tokenizer must ensure that there are only @'s and backslashes
                // at this point.
                assert (ch == '\\') || (ch == '@') : "ch=" + tools.sourced(ch);
                text += ch;
                afterBackslash = false;
            } else if (ch == '\\') {
                afterBackslash = true;
            } else {
                text += ch;
            }
        }

    }

    public String getText() {
        return text;
    }

    @Override
    protected String toStringSuffix() {
        return "<text>" + tools.sourced(getText());
    }
}
