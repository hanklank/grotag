package net.sf.grotag.guide;

/**
 * A node in an Amigaguide document.
 * 
 * @author Thomas Aglassinger
 */
public class Node {
    private String name;
    private String title;

    public Node(String newName, String newTitle) {
        assert newName != null;

        name = newName;
        if (newTitle != null) {
            title = newTitle;
        } else {
            title = name;
        }
    }

    /**
     * The name of the node as specified with <code>@node</code>.
     */
    public String getName() {
        return name;
    }

    /**
     * The title of the node as specified with <code>@node</code>. If no title was specified, this is same as the node name.
     */
    public String getTitle() {
        return title;
    }
}
