package net.sf.grotag.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import net.roydesign.app.Application;
import net.roydesign.ui.StandardMacAboutFrame;
import net.sf.grotag.common.Tools;
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
    private Tools tools;

    public AboutAction() {
        super(Application.getInstance().getAboutJMenuItem().getText());
        tools = Tools.getInstance();
        log = Logger.getLogger(AboutAction.class.getName());
        aboutFrameLock = new Object();
    }

    public void actionPerformed(ActionEvent actionEvent) {
        try {
            log.info("handle \"about\" event");
            synchronized (aboutFrameLock) {
                if (aboutFrame == null) {
                    Image logoImage = tools.readImageRessource("grotag.png");
                    aboutFrame = new StandardMacAboutFrame("Grotag", Version.VERSION_TAG);
                    aboutFrame.setCopyright(Version.COPYRIGHT);
                    aboutFrame.setApplicationIcon(new ImageIcon(logoImage));
                    aboutFrame.pack();
                    tools.setGrotagIcon(aboutFrame);
                }
            }
            aboutFrame.setVisible(true);
        } catch (Exception error) {
            tools.showError("cannot show about dialog", error);
        }
    }
}
