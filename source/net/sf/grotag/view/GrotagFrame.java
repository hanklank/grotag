package net.sf.grotag.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
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

import net.sf.grotag.common.SwingWorker;
import net.sf.grotag.common.Tools;
import net.sf.grotag.guide.DomWriter;
import net.sf.grotag.guide.Guide;
import net.sf.grotag.guide.GuidePile;
import net.sf.grotag.guide.HtmlDomFactory;
import net.sf.grotag.guide.NodeInfo;
import net.sf.grotag.guide.Relation;

/**
 * JFrame to browse an Amigaguide documents converted to HTML.
 * 
 * @author Thomas Aglassinger
 */
public class GrotagFrame extends JFrame implements HyperlinkListener {

    /**
     * Worker to read Amigaguide document in the background while updating the
     * progress bar and status.
     * 
     * @author Thomas Aglassinger
     */
    private class ReadWorker extends SwingWorker {
        private File guideFile;

        public ReadWorker(File newGuideFile) {
            assert newGuideFile != null;
            guideFile = newGuideFile;
        }

        @Override
        public Object construct() {
            try {
                doRead(guideFile);
            } catch (IOException error) {
                showError("cannot read " + tools.sourced(guideFile), error);
            } catch (Exception error) {
                showError("cannot process " + tools.sourced(guideFile), error);
            }
            return null;
        }
    }

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

    /**
     * Action to change URL according to a relation.
     * 
     * @author Thomas Aglassinger
     */
    public class RelationAction extends AbstractAction {
        private Relation relation;

        public RelationAction(String name, Relation newRelation) {
            super(name);
            assert name != null;
            assert newRelation != null;
            relation = newRelation;
        }

        public void actionPerformed(ActionEvent event) {
            try {
                URL pageToGo = relationMap.get(relation);
                setPage(pageToGo);
            } catch (Exception error) {
                showError("cannot go to " + relation + " page", error);
            }
        }

        public Relation getRelation() {
            return relation;
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
    private Map<Relation, URL> relationMap;
    private List<JButton> relationButtons;
    private Map<URL, NodeInfo> urlToNodeMap;

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
        relationMap = new TreeMap<Relation, URL>();
        relationButtons = new LinkedList<JButton>();
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

    public void read(File guideFile) {
        ReadWorker worker = new ReadWorker(guideFile);
        worker.start();
    }

    private void doRead(File guideFile) throws IOException {
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

                urlToNodeMap = new HashMap<URL, NodeInfo>();
                int nodesWritten = 0;
                for (Guide guide : newPile.getGuides()) {
                    for (NodeInfo nodeInfo : guide.getNodeInfos()) {
                        setStatus("Reading " + guide.getDatabaseInfo().getName() + "/" + nodeInfo.getName());
                        File targetFile = factory.getTargetFileFor(guide, nodeInfo);
                        URL targetUrl = targetFile.toURL();
                        org.w3c.dom.Document htmlDocument = factory.createNodeDocument(guide, nodeInfo);
                        DomWriter htmlWriter = new DomWriter(DomWriter.Dtd.HTML);
                        htmlWriter.write(htmlDocument, targetFile);
                        urlToNodeMap.put(targetUrl, nodeInfo);
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

            if ((currentPageUrl != null) && (retraceStack.isEmpty() || !currentPageUrl.equals(retraceStack.peek()))) {
                retraceStack.push(currentPageUrl);
                setRetraceButtonEnabled();
            }
            currentPageUrl = pageUrl;

            HtmlInfo htmlInfo = new HtmlInfo(pageUrl);
            String title = htmlInfo.getTitle();

            if (title != null) {
                setTitle(title + " - Grotag");
            } else {
                setTitle("Grotag");
            }

            relationMap = htmlInfo.getRelationMap();
            for (JButton button : relationButtons) {
                Relation buttonRelation = ((RelationAction) button.getAction()).getRelation();
                boolean relationEnabled = relationMap.containsKey(buttonRelation);
                button.setEnabled(relationEnabled);
            }
        }
    }

    private JButton createRelationButton(String label, Relation relation) {
        JButton result = new JButton(new RelationAction(label, relation));
        result.setEnabled(false);
        return result;
    }

    private final void setUpButtonPane() {
        buttonPane = new JPanel();
        JButton contentsButton = createRelationButton("Contents", Relation.toc);
        JButton indexButton = createRelationButton("Index", Relation.index);
        JButton helpButton = createRelationButton("Help", Relation.help);
        JButton nextButton = createRelationButton("Next", Relation.next);
        JButton previousButton = createRelationButton("Previous", Relation.previous);

        relationButtons.add(contentsButton);
        relationButtons.add(indexButton);
        relationButtons.add(helpButton);
        relationButtons.add(nextButton);
        relationButtons.add(previousButton);

        Dimension rigidSize = new Dimension(contentsButton.getPreferredSize().height, 0);

        retraceButton = new JButton(new RetraceAction());
        retraceButton.setEnabled(false);

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
