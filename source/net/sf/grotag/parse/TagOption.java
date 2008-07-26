package net.sf.grotag.parse;

import java.text.ParseException;
import java.util.Arrays;

import net.sf.grotag.common.Tools;

/**
 * Definition of an option that can be passed to a Tag.
 * 
 * @see Tag
 * @see Type
 * @author Thomas Aglassinger
 */
public class TagOption {
    /**
     * Possible types for tag options:
     * <ul>
     * <li>ANY - any number of options, including none
     * <li>COLOR - one of the following values: "back", "background", "fill",
     * "filltext", "highlight", "shadow", "shine" and "text"
     * <li>FILE - an existing file
     * <li>FILENODE - an existing amigaguide file + "/" + an existing node
     * <li>NODE - an existing node within the current file
     * <li>NUMBER - an integer number
     * <li>SOME - any number of options but at least 1
     * <li>TEXT - a text </li>
     * 
     * @author Thomas Aglassinger
     * 
     */
    public enum Type {
        ANY, COLOR, FILE, FILENODE, NODE, NUMBER, SOME, TEXT
    }

    private enum State {
        AT_TYPE, AT_EQUAL_SIGN, AT_DEFAULT_VALUE, DONE;
    }

    /**
     * Valid colors for <code>@{bg}</code> and <code>@{fg}</code>. This array must be sorted because it is binary searched.
     */
    private static final String[] VALID_COLORS = new String[] { "back", "background", "fill", "filltext", "highlight",
            "shadow", "shine", "text" };

    private Tools tools;
    private int column;
    private String line;
    private Type type;
    private String defaultValue;

    /**
     * Create a new TagOption from a definition description. The description is
     * of the form <code>type["="default]</code> (without any blanks).
     * 
     * @throws ParseException
     *                 if the line does not conform to the syntax specified
     *                 above
     */
    public TagOption(String defintion, int startColumn) throws ParseException {
        assert defintion != null;

        tools = Tools.getInstance();
        line = defintion;
        column = startColumn;

        State state = State.AT_TYPE;
        String[] spaceAndToken;

        do {
            spaceAndToken = tools.getToken(line, column);
            if (spaceAndToken != null) {
                int spaceLength = spaceAndToken[0].length();
                String token = spaceAndToken[1];

                column += spaceLength;
                if (state == State.AT_TYPE) {
                    try {
                        type = Enum.valueOf(Type.class, token.toUpperCase());
                        state = State.AT_EQUAL_SIGN;
                    } catch (IllegalArgumentException error) {
                        throw new ParseException("tag option is " + tools.sourced(token) + " but must be one of: "
                                + Type.values(), column);
                    }
                } else if (state == State.AT_EQUAL_SIGN) {
                    if (!token.equals("=")) {
                        throw new ParseException("token type must be followed by " + tools.sourced("="), column);
                    }
                    state = State.AT_DEFAULT_VALUE;
                } else if (state == State.AT_DEFAULT_VALUE) {
                    defaultValue = token;
                    state = State.DONE;
                } else {
                    throw new ParseException("tag option must have ended", column);
                }
                column += token.length();
            }
        } while (spaceAndToken != null);
        if (state == State.AT_DEFAULT_VALUE) {
            throw new ParseException("default value expected", column);
        }
    }

    /**
     * Error that occurred when validating <code>textToValidate</code> against
     * the specification, or <code>null</code> if text is valid. This only
     * takes into account simple syntactical errors, more complex errors such a
     * a <code>link</code> option not pointing to an existing node are not
     * considered here.
     */
    public String validationError(String textToValidate) {
        String result = null;

        if ((getType() != Type.ANY) && (textToValidate == null)) {
            result = "option must be specified";
        } else if (getType() == Type.COLOR) {
            if (Arrays.binarySearch(VALID_COLORS, textToValidate) < 0) {
                // FIXME: Figure out why tools would be null without explicit
                // assignment here.
                tools = Tools.getInstance();
                // TODO: Add "...instead one of: ..." to error message.
                result = "color is " + tools.sourced(textToValidate);
            }
        } else if (getType() == Type.NUMBER) {
            try {
                Integer.parseInt(textToValidate);
            } catch (NumberFormatException error) {
                result = tools.sourced(textToValidate) + " must be a number";
            }
        }
        return result;
    }

    public TagOption(Type newType, String newDefaultValue) {
        type = newType;
        defaultValue = newDefaultValue;
    }

    public TagOption(Type newType) {
        this(newType, null);
    }

    public Type getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
