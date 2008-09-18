package net.sf.grotag;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import net.roydesign.app.Application;
import net.roydesign.event.ApplicationEvent;
import net.roydesign.ui.StandardMacAboutFrame;
import net.sf.grotag.common.Tools;
import net.sf.grotag.view.GrotagFrame;

/**
 * Mac OS X application to open an empty Grotag window and handle Application
 * events.
 * 
 * @author Thomas Aglassinger
 */
public class MacGrotag implements ActionListener {
    private Application application;
    private GrotagFrame frame;
    private Logger log;
    private StandardMacAboutFrame aboutFrame;
    private String aboutFrameLock;

    public MacGrotag() {
        // Setup logging.
        Tools.getInstance();

        log = Logger.getLogger(MacGrotag.class.getName());
        frame = new GrotagFrame();
        frame.setVisible(true);
        aboutFrameLock = "x";
        application = Application.getInstance();
        application.addOpenDocumentListener(this);
        application.getAboutJMenuItem().addActionListener(this);
    }

    public static void main(String[] arguments) {
        new MacGrotag();
    }

    public void actionPerformed(ActionEvent actionEvent) {
        try {
            ApplicationEvent event = (ApplicationEvent) actionEvent;
            int type = event.getType();
            if (type == ApplicationEvent.ABOUT) {
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
            } else if (type == ApplicationEvent.OPEN_DOCUMENT) {
                log.info("handle \"open document\" event");
                frame.read(event.getFile());
            } else {
                log.warning("ignored application event of type=" + type);
            }
        } catch (Exception error) {
            // FIXME: Show error dialog.
            error.printStackTrace();
        }
    }
}
