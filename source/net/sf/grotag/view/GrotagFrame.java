package net.sf.grotag.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;

import net.sf.grotag.common.Tools;
import net.sf.grotag.guide.DomWriter;
import net.sf.grotag.guide.Guide;
import net.sf.grotag.guide.GuidePile;
import net.sf.grotag.guide.HtmlDomFactory;
import net.sf.grotag.guide.NodeInfo;

/**
 * JFrame to browse an Amigaguide documents converted to HTML.
 * 
 * @author Thomas Aglassinger
 */
public class GrotagFrame extends JFrame implements HyperlinkListener {

    /**
     * Action to process "retrace" command.
     * 
     * @author Thomas Aglassinger
     */
    public class RetraceAction extends AbstractAction {
        private RetraceAction() {
            super("Retrace");
        }

        public void actionPerformed(ActionEvent event) {
            try {
                synchronized (pageLock) {
                    log.info("action: retrace");
                    URL previousHtmlFile = retraceStack.pop();
                    setPage(previousHtmlFile);
                    setRetraceButtonEnabled();
                }
            } catch (Throwable error) {
                showError("cannot retrace", error);
            }
        }
    }

    private JLabel statusLabel;
    private JTextPane htmlPane;
    private JScrollPane htmlScrollPane;
    private JPanel statusPane;
    private JPanel buttonPane;
    private Logger log;
    private Tools tools;
    private Stack<URL> retraceStack;
    private URL currentPageUrl;
    private JProgressBar progressBar;
    private File tempFolder;
    private GuidePile pile;

    /**
     * Lock to synchronize on for page or file operations.
     */
    private Object pageLock;
    private JButton retraceButton;

    public GrotagFrame() {
        super("Grotag");

        log = Logger.getLogger(GrotagFrame.class.getName());
        tools = Tools.getInstance();

        retraceStack = new Stack<URL>();
        pageLock = "pageLock";
        setLayout(new BorderLayout());
        setUpButtonPane();
        setUpHtmlPane();
        setUpStatusPane();
        clearStatus();
        pack();
        progressBar.setVisible(false);
    }

    public void clearStatus() {
        setStatus(" ");
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    private File createTempFolder() throws IOException {
        File result = File.createTempFile("grotag-view-", null);
        result.delete();
        result.mkdirs();
        return result;
    }

    private void showError(String message, Throwable details) {
        assert message != null;
        assert details != null;
        // TODO: Add dialog to show error message.
        log.log(Level.SEVERE, message, details);
        // TODO: Print Stack trace to log.
        details.printStackTrace();
    }

    public void read(File guideFile) throws IOException {
        File newTempFolder = createTempFolder();
        GuidePile newPile = null;

        synchronized (pageLock) {
            progressBar.setValue(0);
            progressBar.setIndeterminate(true);
            progressBar.setVisible(true);
            try {
                setStatus("Reading " + guideFile);
                newPile = GuidePile.createGuidePile(guideFile);
                HtmlDomFactory factory = new HtmlDomFactory(newPile, newTempFolder);
                
                factory.copyStyleFile();

                // Compute number of nodes in pile to show progress.
                // TODO: Use number of items as base for progress.
                int nodeCount = 0;
                for (Guide guide : newPile.getGuides()) {
                    nodeCount += guide.getNodeInfos().size();
                }

                progressBar.setMinimum(0);
                progressBar.setMaximum(nodeCount);
                progressBar.setIndeterminate(false);
                setStatus("Creating pages");

                int nodesWritten = 0;
                for (Guide guide : newPile.getGuides()) {
                    for (NodeInfo nodeInfo : guide.getNodeInfos()) {
                        File targetFile = factory.getTargetFileFor(guide, nodeInfo);
                        org.w3c.dom.Document htmlDocument = factory.createNodeDocument(guide, nodeInfo);
                        DomWriter htmlWriter = new DomWriter(DomWriter.Dtd.HTML);
                        htmlWriter.write(htmlDocument, targetFile);
                        nodesWritten += 1;
                        progressBar.setValue(nodesWritten);
                    }
                }
            } catch (Throwable error) {
                showError("cannot read " + tools.sourced(guideFile), error);
            } finally {
                if (newPile != null) {
                    // Start showing new guide.
                    if (tempFolder != null) {
                        tools.attemptToDeleteAll(tempFolder);
                    }
                    tempFolder = newTempFolder;
                    pile = newPile;
                    retraceStack.clear();
                    setPage(pile.getFirstHtmlFile(tempFolder));
                } else {
                    // Error while preparing new guide; keep the old one.
                    tools.attemptToDeleteAll(newTempFolder);
                }
                clearStatus();
                progressBar.setVisible(false);
            }
        }
    }

    public void setPage(File pageFile) throws MalformedURLException, IOException {
        assert pageFile != null;
        URL pageUrl = pageFile.toURL();
        setPage(pageUrl);
    }

    private void setRetraceButtonEnabled() {
        retraceButton.setEnabled(retraceStack.size() > 0);
    }

    public void setPage(URL pageUrl) throws IOException {
        assert pageUrl != null;
        synchronized (pageLock) {
            log.info("set page to: " + tools.sourced(pageUrl.toString()));
            htmlPane.setPage(pageUrl);
            String title = String.valueOf(htmlPane.getDocument().getProperty(Document.TitleProperty));
            setTitle(title + " - Grotag");
            if ((currentPageUrl != null) && (retraceStack.isEmpty() || !currentPageUrl.equals(retraceStack.peek()))) {
                retraceStack.push(currentPageUrl);
                setRetraceButtonEnabled();
            }
            currentPageUrl = pageUrl;
        }
    }

    private final void setUpButtonPane() {
        buttonPane = new JPanel();
        JButton contentsButton = new JButton("Contents");
        JButton indexButton = new JButton("Index");
        JButton helpButton = new JButton("Help");
        JButton nextButton = new JButton("Next");
        JButton previousButton = new JButton("Previous");
        Dimension rigidSize = new Dimension(contentsButton.getPreferredSize().height, 0);

        retraceButton = new JButton(new RetraceAction());

        contentsButton.setEnabled(false);
        indexButton.setEnabled(false);
        helpButton.setEnabled(false);
        retraceButton.setEnabled(false);
        nextButton.setEnabled(false);
        previousButton.setEnabled(false);

        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.add(contentsButton);
        buttonPane.add(indexButton);
        buttonPane.add(helpButton);
        buttonPane.add(Box.createRigidArea(rigidSize));
        buttonPane.add(retraceButton);
        buttonPane.add(nextButton);
        buttonPane.add(previousButton);
        buttonPane.add(Box.createHorizontalGlue());

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
        statusPane.setLayout(new BoxLayout(statusPane, BoxLayout.LINE_AXIS));
        statusLabel = new JLabel("Initialising");
        int rigidSize = statusLabel.getPreferredSize().height;
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(100, progressBar.getPreferredSize().height));
        progressBar.setMaximumSize(progressBar.getPreferredSize());

        statusPane.add(statusLabel);
        statusPane.add(Box.createHorizontalGlue());
        statusPane.add(progressBar);
        statusPane.add(Box.createRigidArea(new Dimension(rigidSize, 0)));
        add(statusPane, BorderLayout.PAGE_END);
    }

    public void hyperlinkUpdate(HyperlinkEvent linkEvent) {
        try {
            if (linkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                URL urlToOpen = linkEvent.getURL();
                try {
                    setPage(urlToOpen);
                } catch (IOException error) {
                    showError("cannot open URL: " + tools.sourced(urlToOpen.toExternalForm()), error);
                }
            } else if (linkEvent.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                setStatus("Go to " + linkEvent.getURL().toExternalForm());
            } else if (linkEvent.getEventType() == HyperlinkEvent.EventType.EXITED) {
                clearStatus();
            } else {
                log.fine("ignored hyperlink event: " + linkEvent.getEventType());
            }
        } catch (Throwable error) {
            showError("cannot process hyperlink event", error);
        }
    }

    @Override
    public void dispose() {
        synchronized (pageLock) {
            if (htmlPane != null) {
                htmlPane.removeHyperlinkListener(this);
            }
            pile = null;
            if (tempFolder != null) {
                tools.attemptToDeleteAll(tempFolder);
                tempFolder = null;
            }
            super.dispose();
        }
    }
}
