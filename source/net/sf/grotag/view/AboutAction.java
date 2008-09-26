package net.sf.grotag.view;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.AbstractAction;

import net.roydesign.app.Application;
import net.roydesign.ui.StandardMacAboutFrame;
import net.sf.grotag.common.Version;

/**
 * Action to show About window.
 * 
 * @author Thomas Aglassinger
 */
public class AboutAction extends AbstractAction {
    private Object aboutFrameLock;
    private StandardMacAboutFrame aboutFrame;
    private Logger log;

    public AboutAction() {
        super(Application.getInstance().getAboutJMenuItem().getText());
        log = Logger.getLogger(AboutAction.class.getName());
        aboutFrameLock = new Object();
    }

    public void actionPerformed(ActionEvent actionEvent) {
        try {
            log.info("handle \"about\" event");
            synchronized (aboutFrameLock) {
                if (aboutFrame == null) {
                    aboutFrame = new StandardMacAboutFrame("Grotag", Version.VERSION_TAG);
                    aboutFrame.setCopyright(Version.COPYRIGHT);
                    aboutFrame.pack();
                }
                aboutFrame.setVisible(true);
            }
        } catch (Exception error) {
            // FIXME: Show error dialog.
            error.printStackTrace();
        }
    }
}
