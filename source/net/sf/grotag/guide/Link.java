package net.sf.grotag.guide;

import java.io.File;
import java.util.logging.Logger;

import net.sf.grotag.common.AmigaTools;
import net.sf.grotag.common.Tools;
import net.sf.grotag.parse.CommandItem;
import net.sf.grotag.parse.FileSource;

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

    // TODO: public enum Type.

    public static final int NO_LINE = -1;

    private int line;
    private String target;
    private String type;
    private String label;
    private State state;
    private File targetFile;
    private String targetNode;
    private CommandItem linkCommand;

    /**
     * Create a new link.
     */
    public Link(CommandItem newLinkCommand) {
        assert newLinkCommand != null;
        assert newLinkCommand.isLink();
        assert newLinkCommand.getOption(0) != null;

        Tools tools = Tools.getInstance();
        AmigaTools amigaTools = AmigaTools.getInstance();
        Logger log = Logger.getLogger(Link.class.getName());

        linkCommand = newLinkCommand;
        label = newLinkCommand.getOriginalCommandName();
        label = label.substring(1, label.length() - 1);
        state = State.UNCHECKED;
        type = linkCommand.getOption(0).toLowerCase();
        target = linkCommand.getOption(1);
        String lineText = linkCommand.getOption(2);
        if (lineText != null) {
            line = Integer.parseInt(lineText);
        } else {
            line = NO_LINE;
        }
        if (type.equals("link")) {
            // FIXME: Handle non-FileSource properly by using original file.
            // (For example from macros expanding to links.)
            assert newLinkCommand.getFile() instanceof FileSource;
            File guideFile = ((FileSource) newLinkCommand.getFile()).getFile();
            int slashIndex = target.lastIndexOf('/');
            if (slashIndex >= 0) {
                String linkAmigaPath = target.substring(0, slashIndex);
                File baseFolder = guideFile.getParentFile();
                targetFile = amigaTools.getFileFor(linkAmigaPath, baseFolder);
                targetNode = target.substring(slashIndex + 1);
                log.fine("mapping link: " + tools.sourced(linkAmigaPath) + " -> " + tools.sourced(targetFile) + ", "
                        + tools.sourced(targetNode));
            } else {
                // Link to node in same file.
                targetFile = guideFile;
                targetNode = target;
            }
            targetNode = targetNode.toLowerCase();
        }
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

    /**
     * The command from which this link originates.
     */
    public CommandItem getLinkCommand() {
        return linkCommand;
    }

    public State getState() {
        return state;
    }

    public void setState(State newState) {
        state = newState;
    }
}
