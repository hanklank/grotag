package net.sf.grotag.parse;

import java.io.File;
import java.util.List;

import net.sf.grotag.common.Tools;

/**
 * Item representing an Amigaguide command.
 * 
 * @author Thomas Aglassinger
 */
public class CommandItem extends AbstractItem {
    private String commandName;
    private String originalCommandName;
    private boolean isInline;
    private List<AbstractItem> items;

    public CommandItem(File newFile, int newLine, int newColumn,
            String newCommandName, boolean newIsInline,
            List<AbstractItem> newItems) {
        super(newFile, newLine, newColumn);

        assert newCommandName != null;
        assert newItems != null;

        originalCommandName = newCommandName;
        commandName = newCommandName.toLowerCase();
        isInline = newIsInline;
        items = newItems;
    }

    /** Command name in all lower case for easy comparison. */
    public String getCommandName() {
        return commandName;
    }

    /** Command name using upper/lower case as originally specified in the guide. */
    public String getOriginalCommandName() {
        return originalCommandName;
    }

    /**
     * Is the command an inline command inside curly braces?
     */
    public boolean isInline() {
        return isInline;
    }

    /**
     * All options passed to this command, including white space.
     */
    public List<AbstractItem> getItems() {
        return items;
    }

    @Override
    protected String toStringSuffix() {
        String result = "<command>@";

        if (isInline()) {
            result += "{";
        }
        result += Tools.getInstance().sourced(getOriginalCommandName()) + "[";

        boolean isFirstItem = true;
        for (AbstractItem item : items) {
            if (isFirstItem) {
                isFirstItem = false;
            } else {
                result += ", ";
            }
            result += item.toString();
        }
        result += "]";
        if (isInline()) {
            result += "}";
        }
        return result;
    }

    private int getOptionItemIndex(int itemIndex) {
        return 1 + 2 * itemIndex;
    }

    private int getOptionCount() {
        int itemCount = getItems().size();
        int result = itemCount / 2;
        return result;
    }

    /**
     * Option number <code>index</code> or <code>null</code> if there are
     * not enough options.
     */
    public String getOption(int index) {
        String result;

        if (index < getOptionCount()) {
            int optionItemIndex = getOptionItemIndex(index);
            AbstractTextItem textItem = (AbstractTextItem) items
                    .get(optionItemIndex);
            result = textItem.getText();
        } else {
            result = null;
        }

        return result;
    }

    public void setOption(int index, String value) {
        int optionIndex = getOptionItemIndex(index);
        while (optionIndex >= items.size()) {
            int itemIndex = items.size() - 1;
            AbstractItem filler;

            if (itemIndex % 2 == 0) {
                filler = new SpaceItem(getFile(), getLine(), getColumn(), " ");
            } else {
                filler = new TextItem(getFile(), getLine(), getColumn(), "");
            }
            items.add(filler);
        }
        items.set(optionIndex, new StringItem(getFile(), getLine(),
                getColumn(), "\"" + value + "\""));
    }

    // TODO: Move to Tools.
    private boolean containsWhiteSpace(String some) {
        assert some != null;
        boolean result = false;
        int i = 0;
        while (!result && (i < some.length())) {
            if (Character.isWhitespace(some.charAt(i))) {
                result = true;
            } else {
                i = i + 1;
            }
        }
        return result;
    }

    @Override
    public String toPrettyAmigaguide() {
        String result = "@";
        if (isInline()) {
            result += "{";
        }
        result += getCommandName();

        for (int optionIndex = 0; optionIndex < getOptionCount(); optionIndex += 1) {
            String option = getOption(optionIndex);
            assert option != null : "getOption(" + optionIndex
                    + ") must not be null";
            boolean requiresQuotes = containsWhiteSpace(option);
            result += " ";
            if (requiresQuotes) {
                result += "\"";
            }
            result += option;
            if (requiresQuotes) {
                result += "\"";
            }
        }

        if (isInline()) {
            result += "}";
        } else {
            result += "\n";
        }
        return result;
    }
}
