package net.sf.grotas.parse;

import net.sf.grotas.common.Tools;

/** Tokenizer for a single line of an AmigaGuide document */
public class LineTokenizer {
	public static final char TYPE_SPACE = '_';
	public static final char TYPE_TEXT = 'a';
	public static final char TYPE_STRING = '$';
	public static final char TYPE_COMMAND = '@';
	public static final char TYPE_OPEN_BRACE = '{';
	public static final char TYPE_CLOSE_BRACE = '}';

	private static final char IN_TEXT = 't';
	private static final char IN_COMMAND = '@';
	private static final char IN_COMMAND_BRACE = '{';
	private static final int NO_COLUMN = -1;

	/**
	 * Token type when the line is empty.
	 */
	private static final char TYPE_INVALID = 'x';

	private char parserState;
	private String text;
	private int lineNumber;
	private int column;
	private int columnOpenBrace;
	private String token;
	private char type;
	private Tools tools;
	private boolean insertCloseBrace;

	/**
	 * Create a new LineTokenizer for the line <code>newText</code> read from
	 * the input at <code>newLineNumber</code>.
	 * 
	 * The tokenizer already performs some corrections on the input to guarantee
	 * a certain syntactical correctness. In particular, it ensures:
	 * <ul>
	 * <li>Strings in commands have a terminating quote.
	 * <li>Command have a terminating curly brace.
	 * <li>All escape sequences are proper.
	 * </ul>
	 * 
	 * @param newLineNumber
	 *            the number of the line when read from the input file, starting
	 *            with 0
	 */
	public LineTokenizer(String newText, int newLineNumber) {
		assert newText != null;
		assert newLineNumber >= 0;

		tools = Tools.getInstance();
		text = withoutTrailingWhiteSpace(newText);
		lineNumber = newLineNumber;
		column = 0;
		columnOpenBrace = NO_COLUMN;
		parserState = IN_TEXT;
		type = TYPE_INVALID;
	}

	/**
	 * The type of the current token.
	 * 
	 * @return one of TYPE_*
	 */
	public char getType() {
		if (type == TYPE_INVALID) {
			throw new IllegalStateException(
					"getType() must be called only when there is a token available");
		}
		return type;
	}

	public String withoutTrailingWhiteSpace(String some) {
		assert some != null;

		int i = some.length() - 1;
		while ((i > 0) && (Character.isWhitespace(some.charAt(i)))) {
			i -= 1;
		}
		return some.substring(0, i + 1);
	}

	private void fireWarning(String message) {
		assert message != null;
		fireWarning(message, getColumn());
	}

	private void fireWarning(String message, int messageColumn) {
		assert message != null;
		assert messageColumn >= 0;
		System.err.println("(" + getLine() + ":" + messageColumn + ") - "
				+ message);
	}

	public void advance() {
		if (!hasNext()) {
			throw new IllegalStateException(
					"cannot advance past end of line number " + getLine());
		}

		char some;

		if (insertCloseBrace) {
			assert parserState == IN_COMMAND_BRACE : "parserState must be "
					+ IN_COMMAND_BRACE + " but is " + parserState;
			insertCloseBrace = false;
			some = '}';
		} else {
			some = text.charAt(column);
			column += 1;
		}
		token = "" + some;
		type = TYPE_INVALID;
		if (Character.isWhitespace(some)) {
			// Parse sequence of white spaces.
			while (hasChars() && Character.isWhitespace(text.charAt(column))) {
				token += text.charAt(column);
				column += 1;
			}
			type = TYPE_SPACE;
		} else if (some == '@') {
			// Parse @ indicating a command.
			parserState = IN_COMMAND;
			type = TYPE_COMMAND;
		} else if ((parserState == IN_COMMAND) && (some == '{')) {
			// Parse opening curly brace indicating a command.
			columnOpenBrace = column;
			parserState = IN_COMMAND_BRACE;
			type = TYPE_OPEN_BRACE;
		} else if (((parserState == IN_COMMAND) || (parserState == IN_COMMAND_BRACE))
				&& (some == '"')) {
			// Parse quoted text within a command.
			int quoteColumn = column;

			do {
				token += text.charAt(column);
				column += 1;
			} while (hasChars() && (text.charAt(column) != '"'));

			if (!token.endsWith("\"") || token.equals("\"")) {
				token += "\"";
				fireWarning("appended missing trailing quote", quoteColumn);
			}
			type = TYPE_STRING;
		} else if ((parserState == IN_COMMAND_BRACE) && (some == '}')) {
			// Parse "}" within a command to indicate end of command.
			assert columnOpenBrace != NO_COLUMN : "columnOpenBrace must have been set earlier";
			token = "" + some;
			columnOpenBrace = NO_COLUMN;
			parserState = IN_TEXT;
			type = TYPE_CLOSE_BRACE;
		} else {
			// Parse normal text.
			boolean afterBackslash = (some == '\\');

			while (hasChars()
					&& !Character.isWhitespace(text.charAt(column))
					&& !((parserState == IN_COMMAND_BRACE) && (text
							.charAt(column) == '}'))
					&& !((parserState == IN_TEXT)
							&& (text.charAt(column) == '@') && !afterBackslash)) {
				some = text.charAt(column);
				if (afterBackslash) {
					if ((some != '\\') && (some != '@')) {
						fireWarning("inserted missing backslash before dangling backslash with "
								+ tools.sourced(some)
								+ " instead of \"\\\" or \"@\"");
						token += '\\';
					}
					token += some;
					afterBackslash = false;
				} else if (some == '\\') {
					token += some;
					afterBackslash = true;
				} else {
					token += some;
				}
				column += 1;
			}

			if (afterBackslash) {
				fireWarning("appended missing backslash after dangling baskslash at end of token");
				token += '\\';
			}
			type = TYPE_TEXT;
		}
		if (!hasNext() && (parserState == IN_COMMAND_BRACE)) {
			insertCloseBrace = true;
		}
		assert type == TYPE_INVALID : "token type must be set";
	}

	private boolean hasChars() {
		return (column < text.length());
	}

	public boolean hasNext() {
		return hasChars() || insertCloseBrace;
	}

	public int getLine() {
		return lineNumber;
	}

	public int getColumn() {
		return column;
	}

	public String getToken() {
		return token;
	}
}
