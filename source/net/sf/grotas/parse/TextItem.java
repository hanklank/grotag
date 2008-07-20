package net.sf.grotas.parse;

import java.io.File;


/** An item representing text. */
public class TextItem extends AbstractItem {
	private String text;

	public TextItem(File newFile, int newLine, int newColumn, String newText) {
		super(newFile, newLine, newColumn);
		text = newText;
	}

	public String getText() {
		return text;
	}
}
