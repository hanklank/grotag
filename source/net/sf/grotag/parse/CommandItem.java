package net.sf.grotag.parse;

import java.util.ArrayList;
import java.util.List;

import net.sf.grotag.common.AmigaTools;
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

    public CommandItem(AbstractSource newSource, int newLine, int newColumn, String newCommandName,
            boolean newIsInline, List<AbstractItem> newItems) {
        super(newSource, newLine, newColumn);

        assert newCommandName != null;
        assert newItems != null;

        originalCommandName = newCommandName;
        commandName = newCommandName.toLowerCase();
        isInline = newIsInline;
        if (isInline) {
            // Strip trailing "}".
            items = new ArrayList<AbstractItem>(newItems.subList(0, Math.max(0, newItems.size() - 1)));
        } else {
            items = newItems;
        }
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
        boolean result = getCommandName().startsWith("\"");
        assert !result || getCommandName().endsWith("\"");
        return result;
    }

    /**
     * Interpret the command as a link label and yield it without the enclosing
     * quotes.
     */
    public String getLinkLabel() {
        assert isLink();
        String result = getOriginalCommandName();
        result = result.substring(1, result.length() - 1);
        return result;
    }

    /**
     * All options passed to this command, including white space.
     */
    public List<AbstractItem> getItems() {
        return items;
    }

    /**
     * Yield a <code>TextItem</code> that contains the text of this link
     * command.
     */
    public TextItem toTextItem() {
        assert isLink();
        TextItem result;
        AmigaTools amigaTools = AmigaTools.getInstance();
        String linkLabel = getOriginalCommandName();
        assert linkLabel.startsWith("\"");
        assert linkLabel.endsWith("\"");
        assert linkLabel.length() >= 2;
        linkLabel = linkLabel.substring(1, linkLabel.length() - 1);
        linkLabel = amigaTools.escapedForAmigaguide(linkLabel);
        result = new TextItem(getFile(), getLine(), getColumn() + 2, linkLabel);
        return result;
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
        // FIXME: Skip SpaceItems properly.
        return 1 + 2 * itemIndex;
    }

    int getOptionCount() {
        int result = 0;
        for (AbstractItem item : getItems()) {
            if (!(item instanceof SpaceItem)) {
                result += 1;
                assert item instanceof AbstractTextItem;
            }
        }
        return result;
    }

    /**
     * Option number <code>optionIndex</code> or <code>null</code> if there
     * are not enough options.
     */
    public AbstractTextItem getOptionItem(int optionIndex) {
        AbstractTextItem result = null;
        int itemIndex = 0;
        int optionsPassed = 0;

        while ((result == null) && (itemIndex < getItems().size())) {
            AbstractItem item = items.get(itemIndex);
            if (!(item instanceof SpaceItem)) {
                assert item instanceof AbstractTextItem;
                if (optionsPassed == optionIndex) {
                    result = (AbstractTextItem) item;
                } else {
                    optionsPassed += 1;
                }
            }
            itemIndex += 1;
        }

        return result;
    }

    /**
     * Option number <code>optionIndex</code> or <code>null</code> if there
     * are not enough options.
     */
    public String getOption(int optionIndex) {
        String result;

        AbstractTextItem optionItem = getOptionItem(optionIndex);
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
