package net.sf.grotag.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;

import net.sf.grotag.common.Tools;

public class GrotagFrame extends JFrame implements HyperlinkListener {
    private JLabel statusLabel;
    private JTextPane htmlPane;
    private JScrollPane htmlScrollPane;
    private JPanel statusPane;
    private JPanel buttonPane;
    private Logger log;
    private Tools tools;

    public GrotagFrame() {
        super("Grotag");

        log = Logger.getLogger(GrotagFrame.class.getName());
        tools = Tools.getInstance();

        setLayout(new BorderLayout());
        setUpButtonPane();
        setUpHtmlPane();
        setUpStatusPane();
        pack();
    }

    public void setPage(File pageFile) throws MalformedURLException, IOException {
        assert pageFile != null;
        log.info("set page to: " + tools.sourced(pageFile));
        htmlPane.setPage(pageFile.toURL());
        String title = String.valueOf(htmlPane.getDocument().getProperty(Document.TitleProperty));
        setTitle(title + " - Grotag");
    }

    private final void setUpButtonPane() {
        buttonPane = new JPanel();
        JButton contentsButton = new JButton("Contents");
        JButton indexButton = new JButton("Next");
        JButton helpButton = new JButton("Next");
        JButton backButton = new JButton("Retrace");
        JButton nextButton = new JButton("Next");
        JButton previousButton = new JButton("Previous");

        contentsButton.setEnabled(false);
        indexButton.setEnabled(false);
        helpButton.setEnabled(false);
        backButton.setEnabled(false);
        nextButton.setEnabled(false);
        previousButton.setEnabled(false);

        buttonPane.add(contentsButton);
        buttonPane.add(indexButton);
        buttonPane.add(helpButton);
        buttonPane.add(backButton);
        buttonPane.add(nextButton);
        buttonPane.add(previousButton);

        add(buttonPane, BorderLayout.PAGE_START);
    }

    private final void setUpHtmlPane() {
        htmlPane = new JTextPane();
        htmlPane.addHyperlinkListener(this);
        htmlPane.setPreferredSize(new Dimension(640, 512));
        htmlPane.setEditable(false);
        htmlScrollPane = new JScrollPane(htmlPane);
        add(htmlScrollPane, BorderLayout.CENTER);
    }

    private final void setUpStatusPane() {
        statusPane = new JPanel();
        statusLabel = new JLabel("...");
        statusPane.add(statusLabel);
        add(statusPane, BorderLayout.PAGE_END);
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            URL urlToOpen = e.getURL();
            try {
                htmlPane.setPage(urlToOpen);
            } catch (IOException error) {
                log.log(Level.WARNING, "cannot open URL: " + tools.sourced(urlToOpen.toExternalForm()), error);
            }
        }
    }

    @Override
    public void dispose() {
        if (htmlPane != null) {
            htmlPane.removeHyperlinkListener(this);
        }
        super.dispose();
    }

}
