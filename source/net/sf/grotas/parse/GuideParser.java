package net.sf.grotas.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class GuideParser {
	private static final char BEFORE_DATABASE = 'b';
	private LineTokenizer tokenizer;
	private File guideFile;
	private char state;

	public GuideParser(File newGuideFile) {
		assert newGuideFile != null;
		guideFile = newGuideFile;
	}

	public void read() throws IOException {
		InputStream guideStream = new FileInputStream(guideFile);
		BufferedReader guideReader = new BufferedReader(new InputStreamReader(
				guideStream, "ISO-8859-1"));
		int lineNumber = 0;
		String line;

		state = BEFORE_DATABASE;
		try {
			do {
				line = guideReader.readLine();
				if (line != null) {
					tokenizer = new LineTokenizer(line, lineNumber);
				}
				lineNumber += 1;
			} while (line != null);
		} finally {
			guideReader.close();
		}
	}
}
