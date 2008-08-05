package net.sf.grotag.guide;

import net.sf.grotag.parse.CommandItem;

/**
 * Information about an <code>@node</code>.
 * 
 * @author Thomas Aglassinger
 */
public class NodeInfo extends AbstractInfo {
    private DatabaseInfo databaseInfo;
    private CommandItem startNode;
    private CommandItem endNode;
    private String title;

    public NodeInfo(DatabaseInfo newDatabaseInfo, String newName, String newTitle) {
        super(newName.toLowerCase());
        assert newDatabaseInfo != null;

        databaseInfo = newDatabaseInfo;
        if (newTitle != null) {
            title = newTitle;
        } else {
            title = newName;
        }
    }

    public String getTitle() {
        return title;
    }

    public CommandItem getStartNode() {
        return startNode;
    }

    public void setStartAndEndNode(CommandItem newStartNode, CommandItem newEndNode) {
        assert newStartNode != null;
        assert newStartNode.getCommandName().equals("node");
        assert newEndNode != null : "@endnode must exist for: " + newStartNode;
        assert newEndNode.getCommandName().equals("endnode") : "name of @node and @endnode must match: @node="
                + newStartNode + ", @endnode=" + newEndNode;
        startNode = newStartNode;
        endNode = newEndNode;
    }

    public CommandItem getEndNode() {
        return endNode;
    }

    @Override
    public String getFontName() {
        String result = super.getFontName();
        if (result == null) {
            result = databaseInfo.getFontName();
        }
        return result;
    }

    @Override
    public Wrap getWrap() {
        Wrap result = super.getWrap();
        if (result == Wrap.DEFAULT) {
            result = databaseInfo.getWrap();
        }
        return result;
    }

    @Override
    public int getFontSize() {
        int result = super.getFontSize();
        if (result == 0) {
            result = databaseInfo.getFontSize();
        }
        return result;
    }

    @Override
    public String toString() {
        String result = "NodeInfo " + getName() + ": start=" + getStartNode() + ", end=" + getEndNode();
        return result;
    }
}
