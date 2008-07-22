package net.sf.grotag.guide;


/**
 * An Amigaguide macro. For example the macro definition
 * <code>@@macro buddies "hugo, sepp, resi"</code> yields a macro with the name
 *         "buddies" and the value "hugo, sepp, resi". Every time
 *         <code>@@{buddies}<code> shows up in the  document, it will be replaced replaced
 *         with "hugo, sepp, resi".
 * @author Thomas Aglassinger
 */
public class Macro {
    private String name;
    private String text;
    
    public Macro(String newName, String newText) {
        assert newName != null;
        name = newName;
        text = newText;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }
}
