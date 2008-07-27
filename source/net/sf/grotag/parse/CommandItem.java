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

    public CommandItem(File newFile, int newLine, int newColumn, String newCommandName, boolean newIsInline,
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
     * Is the command an inline command inside curly braces, for example
     * <code>@{b}</code>?
     */
    public boolean isInline() {
        return isInline;
    }

    /**
     * Is the command a link command, for example
     * <code>@{"Overview" LINK overview}</code>?
     */
    public boolean isLink() {
        return getCommandName().startsWith("\"");
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
    public AbstractTextItem getOptionItem(int index) {
        AbstractTextItem result;

        if (index < getOptionCount()) {
            int optionItemIndex = getOptionItemIndex(index);
            result = (AbstractTextItem) items.get(optionItemIndex);
        } else {
            result = null;
        }

        return result;
    }

    /**
     * Option number <code>index</code> or <code>null</code> if there are
     * not enough options.
     */
    public String getOption(int index) {
        String result;

        AbstractTextItem optionItem = getOptionItem(index);
        if (optionItem != null) {
            result = optionItem.getText();
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
        items.set(optionIndex, new StringItem(getFile(), getLine(), getColumn(), "\"" + value + "\""));
    }

    private boolean requiresQuotes(String some) {
        assert some != null;
        boolean result = false;
        int i = 0;
        while (!result && (i < some.length())) {
            char ch = some.charAt(i);
            if (Character.isWhitespace(ch) || (ch == '}')) {
                result = true;
            } else {
                i = i + 1;
            }
        }
        return result;
    }

    /**
     * Short Amigaguide snipplet to refer to the command in error messages,
     * excluding any options and link descriptions.
     */
    public String toShortAmigaguide() {
        String result = "@";
        if (isInline()) {
            result += "{";
        }
        if (isLink()) {
            String linkType = getOption(0);
            result += "\"...\"";
            if (linkType != null) {
                result += " " + linkType.toLowerCase();
            }
        } else {
            result += getCommandName();
        }
        if (isInline()) {
            result += "}";
        }
        return result;
    }

    @Override
    public String toPrettyAmigaguide() {
        String result = "@";
        if (isInline()) {
            result += "{";
        }
        if (isLink()) {
            result += getOriginalCommandName();
        } else {
            String name = getCommandName();

            // Make sure $VER: is rendered upper case so the AmigaOS version
            // command can find it.
            if (name.equals("$ver:")) {
                name = name.toUpperCase();
            }
            result += name;
        }

        for (int optionIndex = 0; optionIndex < getOptionCount(); optionIndex += 1) {
            String option = getOption(optionIndex);
            assert option != null : "getOption(" + optionIndex + ") must not be null";
            boolean requiresQuotes = requiresQuotes(option);
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

    /**
     * A single string containing all white space and options passed to the
     * command except for leading and trailing white space.
     */
    public String getAllOptionsText() {
        String result = "";
        for (AbstractItem item : getItems()) {
            if (item instanceof AbstractTextItem) {
                result += ((AbstractTextItem) item).getText();
            } else if (item instanceof SpaceItem) {
                result += ((SpaceItem) item).getSpace();
            } else {
                assert false : "cannot append item: " + item;
            }
        }
        result = result.trim();
        return result;
    }
}
