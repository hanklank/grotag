package net.sf.grotag.view;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.AbstractAction;

import net.roydesign.app.Application;
import net.roydesign.ui.StandardMacAboutFrame;

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
        aboutFrameLock = "x";
    }

    public void actionPerformed(ActionEvent actionEvent) {
        try {
            log.info("handle \"about\" event");
            synchronized (aboutFrameLock) {
                if (aboutFrame == null) {
                    // FIXME: Use Version class.
                    aboutFrame = new StandardMacAboutFrame("Grotag", "0.1.0");
                    aboutFrame.setCopyright("Copyright 2008 Thomas Aglassinger.");
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
