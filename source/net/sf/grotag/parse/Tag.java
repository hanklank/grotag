package net.sf.grotag.parse;

/**
 * Amigaguide tag
 * 
 * @author Thomas Aglassinger
 */
public class Tag implements Comparable<Tag> {
    /**
     * Possible scopes for Amigaguide tags:
     * <ul>
     * <li>GLOBAL - takes effect for the whole document</li>
     * <li>INLINE - only available within curly braces</li>
     * <li>LINK - a link tag, for example
     * <code>@@{"hugo" link "hugo.guide"}</code></li>
     *           <li>NODE - takes effect within a node</li>
     *           </ul>
     * 
     * @author Thomas Aglassinger
     */
    public enum Scope {
        GLOBAL, INLINE, LINK, NODE
    }

    /**
     * Possible versions of Amigaguide
     * <ul>
     * <li>V34 - Workbench 1.3 and 2.x</li>
     * <li>V39 - Workbench 3.0</li>
     * <li>V40 - Workbench 3.1</li>
     * </ul>
     * 
     * @author Thomas Aglassinger
     */
    public enum Version {
        V34, V39, V40
    }

    private String name;
    private TagOption[] options;
    private Scope scope;
    private boolean isMacro;
    private boolean isObsolete;
    private boolean isUnique;
    private boolean isUnused;
    private AbstractTextItem macroTextItem;
    private Version version;

    public Tag(String newName, Version newVersion, Scope newScope, TagOption[] newOptions) {
        this(newName, newVersion, newScope, false, newOptions);
    }

    public Tag(String newName, Version newVersion, Scope newScope) {
        this(newName, newVersion, newScope, false, (TagOption[]) null);
    }

    public Tag(String newName, Version newVersion, Scope newScope, TagOption newOption) {
        this(newName, newVersion, newScope, false, newOption);
    }

    public Tag(String newName, Version newVersion, Scope newScope, boolean newUnique) {
        this(newName, newVersion, newScope, newUnique, (TagOption[]) null);
    }

    public Tag(String newName, Version newVersion, Scope newScope, boolean newUnique, TagOption newOption) {
        this(newName, newVersion, newScope, newUnique, new TagOption[] { newOption });
    }

    /**
     * Create a macro with the name <code>newName</code> and
     * <code>newTextItem</code> which defines the text calls to the macro
     * should be replaced with.
     */
    public static Tag createMacro(String newName, AbstractTextItem newTextItem) {
        assert newName != null;
        assert newName.equals(newName.toLowerCase());
        assert newName.length() > 0;
        assert newTextItem != null;
        assert (newTextItem instanceof TextItem) || (newTextItem instanceof StringItem) : "newTextItem="
                + newTextItem.getClass().getName();

        // V34 because macros will be expanded anyway. @macro nevertheless is
        // V40, and so are all V40 tags used within a macro.
        Tag result = new Tag(newName, Version.V34, Scope.INLINE);
        result.isMacro = true;
        result.macroTextItem = newTextItem;
        return result;
    }

    public static Tag createLink(String newName, Version newVersion) {
        return createLink(newName, newVersion, (TagOption[]) null);
    }

    public static Tag createLink(String newName, Version newVersion, TagOption newOption) {
        Tag result;
        TagOption[] options;
        if (newOption != null) {
            options = new TagOption[] { newOption };
        } else {
            options = null;
        }
        result = new Tag(newName, newVersion, Scope.LINK, false, options);
        return result;
    }

    public static Tag createLink(String newName, Version newVersion, TagOption[] newOptions) {
        return new Tag(newName, newVersion, Scope.LINK, false, newOptions);
    }

    public Tag(String newName, Version newVersion, Scope newScope, boolean newUnique, TagOption[] newOptions) {
        assert newName != null;
        assert newName.equals(newName.toLowerCase());
        assert newName.length() > 0;
        assert !newUnique || (newScope != Scope.INLINE);

        name = newName;
        version = newVersion;
        scope = newScope;
        isUnique = newUnique;
        options = newOptions;
    }

    public String getName() {
        return name;
    }

    public TagOption[] getOptions() {
        return options;
    }

    public Scope getScope() {
        return scope;
    }

    /**
     * First Amigaguide version the tag was available for.
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Compare with <code>other</code> concerning scope and name and yield -1,
     * 0 or 1.
     */
    public int compareTo(Tag other) {
        int result = getScope().ordinal() - other.getScope().ordinal();
        if (result == 0) {
            result = getName().compareToIgnoreCase(other.getName());
        }
        return result;
    }

    /** Is the tag unique within its scope? */
    public boolean isUnique() {
        return isUnique;
    }

    /**
     * Is the tag a macro defined with <code>@@macro</code>?
     */
    public boolean isMacro() {
        return isMacro;
    }

    /**
     * The <code>TextItem</code> containing the text that calls to the macro
     * should be replaced with.
     */
    public AbstractTextItem getMacroTextItem() {
        assert isMacro;
        return macroTextItem;
    }

    public boolean isObsolete() {
        return isObsolete;
    }

    public void setObsolete(boolean newObsolete) {
        this.isObsolete = newObsolete;
    }

    public boolean isUnused() {
        return isUnused;
    }

    public void setUnused(boolean newUnused) {
        this.isUnused = newUnused;
    }
}
