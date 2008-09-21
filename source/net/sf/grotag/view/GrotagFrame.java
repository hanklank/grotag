package net.sf.grotag.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
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

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import net.sf.grotag.common.AmigaPathList;
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
     * Menu bar to interact with the Grotag viewer.
     * 
     * @author Thomas Aglassinger
     */
    private class GrotagMenuBar extends JMenuBar {
        private int commandMask;

        public GrotagMenuBar() {
            super();
            if (tools.isMacOsX()) {
                commandMask = ActionEvent.META_MASK;
            } else {
                commandMask = ActionEvent.CTRL_MASK;
            }
            add(createFileMenu());
            add(createEditMenu());
            add(createGoMenu());
        }

        private void setAccelerator(JMenuItem item, int code) {
            item.setAccelerator(KeyStroke.getKeyStroke(code, commandMask));
        }

        private final JMenu createFileMenu() {
            JMenu result = new JMenu("File");
            JMenuItem openItem = new JMenuItem(new OpenAction());
            openItem.setMnemonic(KeyEvent.VK_O);
            setAccelerator(openItem, KeyEvent.VK_O);
            result.add(openItem);
            if (!tools.isMacOsX()) {
                JMenuItem exitItem = new JMenuItem(new ExitAction());
                exitItem.setMnemonic(KeyEvent.VK_X);
                setAccelerator(exitItem, KeyEvent.VK_X);
                result.add(exitItem);
            }
            result.setMnemonic(KeyEvent.VK_F);
            return result;
        }

        private final JMenu createEditMenu() {
            JMenu result = new JMenu("Edit");
            Action copyAction = getActionByName(DefaultEditorKit.copyAction);
            Action selectAllAction = getActionByName(DefaultEditorKit.selectAllAction);
            JMenuItem copyItem = new JMenuItem(copyAction);
            JMenuItem selectAllItem = new JMenuItem(selectAllAction);
            copyItem.setAction(copyAction);
            copyItem.setText("Copy");
            copyItem.setMnemonic(KeyEvent.VK_C);
            setAccelerator(copyItem, KeyEvent.VK_C);
            selectAllItem.setText("Select all");
            selectAllItem.setMnemonic(KeyEvent.VK_A);
            setAccelerator(selectAllItem, KeyEvent.VK_A);
            result.add(copyItem);
            result.add(selectAllItem);
            result.setMnemonic(KeyEvent.VK_E);
            return result;
        }

        private final JMenu createGoMenu() {
            JMenu result = new JMenu("Go");
            JMenuItem nextItem = new JMenuItem(new RelationAction("Next", Relation.next));
            nextItem.setMnemonic(KeyEvent.VK_N);
            setAccelerator(nextItem, KeyEvent.VK_RIGHT);
            JMenuItem previousItem = new JMenuItem(new RelationAction("Previous", Relation.previous));
            previousItem.setMnemonic(KeyEvent.VK_P);
            setAccelerator(previousItem, KeyEvent.VK_LEFT);
            JMenuItem tocItem = new JMenuItem(new RelationAction("Contents", Relation.toc));
            tocItem.setMnemonic(KeyEvent.VK_C);
            setAccelerator(tocItem, KeyEvent.VK_T);
            JMenuItem indexItem = new JMenuItem(new RelationAction("Index", Relation.index));
            indexItem.setMnemonic(KeyEvent.VK_I);
            setAccelerator(indexItem, KeyEvent.VK_N);
            result.add(nextItem);
            result.add(previousItem);
            result.add(tocItem);
            result.add(indexItem);
            result.setMnemonic(KeyEvent.VK_G);
            return result;
        }
    }

    /**
     * Action to close the window and exit.
     * 
     * @author Thomas Aglassinger
     */
    private class ExitAction extends AbstractAction {
        public ExitAction() {
            super("Exit");
        }

        public void actionPerformed(ActionEvent e) {
            dispose();
            System.exit(0);
        }
    }

    /**
     * Action to open a new guide using a dialog.
     * 
     * @author Thomas Aglassinger
     */
    private class OpenAction extends AbstractAction {
        public OpenAction() {
            super("Open...");
        }

        public void actionPerformed(ActionEvent event) {
            try {
                int userAction = openChooser.showOpenDialog(getGrotagFrame());
                if (userAction == JFileChooser.APPROVE_OPTION) {
                    // FIXME: Read grotag.xml.
                    read(openChooser.getSelectedFile(), new AmigaPathList());
                }
            } catch (Exception error) {
                showError("cannot open file", error);
            }
        }
    }

    /**
     * Worker to read Amigaguide document in the background while updating the
     * progress bar and status.
     * 
     * @author Thomas Aglassinger
     */
    private class ReadWorker extends SwingWorker {
        private File guideFile;
        private AmigaPathList amigaPaths;

        public ReadWorker(File newGuideFile, AmigaPathList newAmigaPaths) {
            assert newGuideFile != null;
            assert newAmigaPaths != null;
            guideFile = newGuideFile;
            amigaPaths = newAmigaPaths;
        }

        @Override
        public Object construct() {
            try {
                doRead(guideFile, amigaPaths);
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

    private static final String DEFAULT_TITLE = "Grotag";

    private JLabel statusLabel;
    private JTextPane htmlPane;
    private JScrollPane htmlScrollPane;
    private JPanel statusPane;
    private JPanel buttonPane;
    private JSplitPane splitPane;
    private JScrollPane messagePane;
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
    private Map<Object, Action> editorKitActionMap;
    private JFileChooser openChooser;
    private MessageItemTableModel messageModel;

    /**
     * Lock to synchronize on for page or file operations.
     */
    private Object pageLock;
    private JButton retraceButton;

    private JTable messageTable;

    public GrotagFrame() {
        super(DEFAULT_TITLE);

        log = Logger.getLogger(GrotagFrame.class.getName());
        tools = Tools.getInstance();

        retraceStack = new Stack<URL>();
        relationMap = new TreeMap<Relation, URL>();
        relationButtons = new LinkedList<JButton>();
        pageLock = "pageLock";
        openChooser = new JFileChooser();
        openChooser.addChoosableFileFilter(new GuideFileFilter());
        openChooser.setAcceptAllFileFilterUsed(false);
        setLayout(new BorderLayout());
        setUpButtonPane();
        setUpHtmlPane();
        setUpEditorActionTable(htmlPane);
        setUpMessagePane();
        setUpSplitPane();
        setUpStatusPane();
        clearStatus();
        add(buttonPane, BorderLayout.PAGE_START);
        add(splitPane, BorderLayout.CENTER);
        add(statusPane, BorderLayout.PAGE_END);
        setJMenuBar(new GrotagMenuBar());
        pack();
        progressBar.setVisible(false);
    }

    private void setUpMessagePane() {
        messageModel = new MessageItemTableModel();
        messageTable = new JTable(messageModel);
        messageTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        tools.initColumnWidths(messageTable);
        messagePane = new JScrollPane(messageTable);
    }

    private void setUpSplitPane() {
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, htmlScrollPane, messagePane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.90);
    }

    /**
     * Build a map to find editor actions by name.
     */
    private void setUpEditorActionTable(JTextComponent textComponent) {
        editorKitActionMap = new HashMap<Object, Action>();
        Action[] actionsArray = textComponent.getActions();
        for (int i = 0; i < actionsArray.length; i++) {
            Action a = actionsArray[i];
            editorKitActionMap.put(a.getValue(Action.NAME), a);
        }
    }

    private Action getActionByName(String name) {
        return editorKitActionMap.get(name);
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

    public void read(File guideFile) throws SAXException, ParserConfigurationException, IOException {
        assert guideFile != null;

        AmigaPathList amigaPaths = new AmigaPathList();
        File baseFolder = guideFile.getParentFile();
        File[] possibleGrotaxXmlFolders = new File[] { baseFolder, new File(System.getProperty("user.dir")) };
        int folderIndex = 0;
        boolean grotagXmlFound = false;

        while (!grotagXmlFound && (folderIndex < possibleGrotaxXmlFolders.length)) {
            File grotagXml = new File(possibleGrotaxXmlFolders[folderIndex], "grotag.xml");
            try {
                amigaPaths.read(grotagXml);
                grotagXmlFound = true;
            } catch (FileNotFoundException errorToIgnore) {
                // Just move on and try the next file.
                folderIndex += 1;
            }
        }
        read(guideFile, amigaPaths);
    }

    public void read(File guideFile, AmigaPathList newAmigaPaths) {
        assert guideFile != null;
        assert newAmigaPaths != null;
        ReadWorker worker = new ReadWorker(guideFile, newAmigaPaths);
        worker.start();
    }

    private void doRead(File guideFile, AmigaPathList newAmigaPaths) throws IOException {
        assert guideFile != null;
        assert newAmigaPaths != null;

        File newTempFolder = createTempFolder();
        GuidePile newPile = null;

        synchronized (pageLock) {
            progressBar.setValue(0);
            progressBar.setIndeterminate(true);
            progressBar.setVisible(true);
            openChooser.setCurrentDirectory(guideFile.getParentFile());
            try {
                setStatus("Reading " + guideFile);
                newPile = GuidePile.createGuidePile(guideFile, newAmigaPaths);
                HtmlDomFactory factory = new HtmlDomFactory(newPile, newTempFolder);

                factory.copyStyleFile();
                factory.setAddDublinCore(false);
                factory.setAddNavigationBar(false);
                factory.setCopyNonGuides(false);

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
                messageModel.update();
                tools.initColumnWidths(messageTable);
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
                setTitle(DEFAULT_TITLE);
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
    }

    private final void setUpHtmlPane() {
        htmlPane = new JTextPane();
        htmlPane.addHyperlinkListener(this);
        htmlPane.setPreferredSize(new Dimension(640, 512));
        htmlPane.setEditable(false);
        htmlScrollPane = new JScrollPane(htmlPane);
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
    }

    /**
     * Handle to this frame for inner classes which cannot refer to it using
     * <code>this</code>.
     */
    private JFrame getGrotagFrame() {
        return this;
    }

    public void hyperlinkUpdate(HyperlinkEvent linkEvent) {
        try {
            if (linkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                URL urlToOpen = linkEvent.getURL();
                try {
                    BufferedImage possibleImage = null;
                    try {
                        possibleImage = ImageIO.read(urlToOpen);
                    } catch (IIOException error) {
                        log.fine("assume url is not an image: " + urlToOpen);
                    }
                    if (possibleImage == null) {
                        setPage(urlToOpen);
                    } else {
                        ImageFrame imageFrame = new ImageFrame(possibleImage);
                        imageFrame.setTitle(tools.getName(urlToOpen));
                        imageFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                        imageFrame.pack();
                        imageFrame.setVisible(true);
                    }
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
