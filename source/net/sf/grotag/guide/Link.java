package net.sf.grotag.guide;

import java.io.File;

import net.sf.grotag.parse.CommandItem;

public class Link {
    /**
     * Enumerator for the different states a link can have.
     * <ul>
     * <li>BROKEN - A link to a non-existent document.
     * <li>VALID - A link to an existing Amigaguide document and node.
     * <li>VALID_GUIDE_BROKEN_NODE - A link to an existing Amigaguide document
     * but a non-existent node.
     * <li>VALID_GUIDE_UNCHECKED_NODE - A link to an existing Amigaguide
     * document but a non-existent node.
     * <li>VALID_OTHER_FILE - A link to an existing file that is not an
     * Amigaguide document.
     * <li>UNCHECKED - Link has not been checked yet (the initial state).
     * <li>UNSUPPORTED - A link of a kind that is not supported by Grotag, for
     * example "rx".
     * </ul>
     * 
     * @author Thomas Aglassinger
     */
    public enum State {
        BROKEN, VALID, VALID_GUIDE_BROKEN_NODE, VALID_GUIDE_UNCHECKED_NODE, VALID_OTHER_FILE, UNCHECKED, UNSUPPORTED
    }

    public static final int NO_LINE = -1;

    private int line;
    private String target;
    private String type;
    private String label;
    private State state;
    private File targetFile;
    private String targetNode;
    private CommandItem sourceItem;

    /**
     * Create a new link. This has package visibility only, use
     * <code>Guide.createLink</code> to create a link from a class outside of
     * this package.
     */
    Link(CommandItem newSource, String newType, String newTarget, int newLineNumber) {
        assert newSource != null;
        assert newSource.getCommandName().startsWith("\"");
        assert newSource.getCommandName().endsWith("\"");
        assert newSource.getOption(0) != null;
        assert newType != null;
        assert newType.length() > 0;
        assert newTarget != null;
        assert newTarget.length() > 0;
        assert (newLineNumber > 0) || (newLineNumber == NO_LINE);

        sourceItem = newSource;
        label = newSource.getOriginalCommandName();
        label = label.substring(1, label.length() - 1);
        state = State.UNCHECKED;
        type = newType;
        target = newTarget;
        line = newLineNumber;
    }

    public int getLine() {
        return line;
    }

    /**
     * The target as optional Amiga file path with a slash "/" and the required
     * node name. Example: "Help:MyApplication/Manual.guide/Main".
     */
    public String getTarget() {
        return target;
    }

    /**
     * The type of the link, for example "link".
     */
    public String getType() {
        return type;
    }

    /**
     * The label the link will be presented with towards the user. For example,
     * <code>@{" Introduction " link "intro"}</code> would yield " Introduction "
     *     (including the blanks).
     */
    public String getLabel() {
        return label;
    }

    private boolean isLinkType() {
        return (getType().equals("link"));
    }

    /**
     * The actual file on the local file system which contains the Amigaguide
     * document where the target node is defined.
     */
    public File getTargetFile() {
        assert isLinkType() : "type=" + getType();
        return targetFile;
    }

    /**
     * The name of the node in the target file.
     */
    public String getTargetNode() {
        assert isLinkType() : "type=" + getType();
        return targetNode;
    }

    public void setResolvedTarget(File newTargetFile, String newTargetNode) {
        assert newTargetFile != null;
        assert newTargetNode != null;
        assert newTargetNode.length() > 0;
        assert isLinkType() : "type=" + getType();
        targetFile = newTargetFile;
        targetNode = newTargetNode;
    }

    /**
     * The command from which this link originates.
     */
    public CommandItem getSourceItem() {
        return sourceItem;
    }

    public State getState() {
        return state;
    }

    public void setState(State newState) {
        state = newState;
    }
}
