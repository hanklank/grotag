package net.sf.grotag.guide;

/**
 * Information about a <code>@database</code>.
 * 
 * @author Thomas Aglassinger
 */
public class DatabaseInfo extends AbstractInfo {
    private String author;
    private String copyright;
    private String version;

    protected DatabaseInfo(String newName) {
        super(newName);
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
