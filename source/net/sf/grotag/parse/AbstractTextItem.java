package net.sf.grotag.parse;

import java.io.File;

public abstract class AbstractTextItem extends AbstractItem {
    private String text;

    protected AbstractTextItem(File newFile, int newLine, int newColumn) {
        super(newFile, newLine, newColumn);
    }

    protected final void setText(String newText) {
        text = newText;
    }

    public String getText() {
        return text;
    }
}
