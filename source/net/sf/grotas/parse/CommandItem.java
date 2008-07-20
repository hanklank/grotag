package net.sf.grotas.parse;

import java.io.File;
import java.util.List;

import net.sf.grotas.common.Tools;

public class CommandItem extends AbstractItem {
    private String commandName;
    private String originalCommandName;
    private boolean isInline;
    private List<AbstractItem> items;

    public CommandItem(File newFile, int newLine, int newColumn, String newCommandName, boolean newIsInline, List<AbstractItem> newItems) {
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
        for (AbstractItem item: items) {
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
}
