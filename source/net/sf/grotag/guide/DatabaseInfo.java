package net.sf.grotag.guide;

/**
 * Information about an Amigaguide <code>@database</code>.
 * 
 * @author Thomas Aglassinger
 */
public class DatabaseInfo extends AbstractInfo {
    private static final String GUIDE_SUFFIX = ".guide";

    private String author;
    private String copyright;
    private String version;

    protected DatabaseInfo(String newName) {
        super(newName);
        if (getName().toLowerCase().endsWith(GUIDE_SUFFIX)) {
            setName(getName().substring(0, getName().length() - GUIDE_SUFFIX.length()));
        }
        setWrap(Wrap.NONE);
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String newAuthor) {
        author = newAuthor;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String newCopyright) {
        copyright = newCopyright;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String newVersion) {
        version = newVersion;
    }

    @Override
    public String toString() {
        String result = "DatabaseInfo: " + getName();
        return result;
    }
}
