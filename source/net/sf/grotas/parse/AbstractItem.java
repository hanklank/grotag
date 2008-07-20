package net.sf.grotas.parse;

import java.io.File;

/**
 * Abstract item in an Amigaguide token stream.
 *
 * @author Thomas Aglassinger
 *
 */
public abstract class AbstractItem {
	private File file;
	private int line;
	private int column;

	public AbstractItem(File newFile, int newLine, int newColumn) {
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
}
