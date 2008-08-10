package net.sf.grotag.guide;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.grotag.common.AmigaTools;
import net.sf.grotag.common.Tools;
import net.sf.grotag.parse.AbstractItem;
import net.sf.grotag.parse.AbstractTextItem;
import net.sf.grotag.parse.CommandItem;
import net.sf.grotag.parse.NewLineItem;
import net.sf.grotag.parse.SpaceItem;
import net.sf.grotag.parse.Tag;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class DocBookWriter {
    private static final String OUTPUT_ENCODING = "UTF-8";

    private enum NodeParserState {
        BEFORE_NODE, INSIDE_NODE, AFTER_NODE;
    }

    private GuidePile pile;
    private Writer writer;
    private Document dom;
    private Element bookElement;
    private Logger log;
    private Tools tools;

    /**
     * Mapping of Amigaguide node names to DocBook node names. DocBook node
     * names must conform to the NCName definition, which means the name has to
     * start with a letter and then must use only certain characters.
     */
    private Map<String, String> agNodeToDbNodeMap;
    private AmigaTools amigaTools;

    private DocBookWriter(GuidePile newPile, Writer newWriter) {
        assert newPile != null;
        assert newWriter != null;

        log = Logger.getLogger(DocBookWriter.class.getName());
        tools = Tools.getInstance();
        amigaTools = AmigaTools.getInstance();

        pile = newPile;
        writer = newWriter;

        // Map the Amigaguide node names to DocBook id's that conform to the
        // NCName definition.
        agNodeToDbNodeMap = new HashMap<String, String>();
        int nodeCounter = 1;
        for (Guide guide : pile.getGuides()) {
            for (NodeInfo nodeInfo : guide.getNodeInfos()) {
                String agNodeName = nodeKey(guide, nodeInfo);
                String dbNodeName = "n" + nodeCounter;

                log.log(Level.INFO, "add mapped node {0} from {1}", new Object[] { dbNodeName, agNodeName });

                assert !agNodeToDbNodeMap.containsKey(agNodeName) : "duplicate agNode: " + tools.sourced(agNodeName);
                assert !agNodeToDbNodeMap.containsValue(dbNodeName) : "duplicate dbNode: " + tools.sourced(dbNodeName);

                agNodeToDbNodeMap.put(agNodeName, dbNodeName);
                nodeCounter += 1;
            }
        }
    }

    protected Node createAmigaguideNode() {
        Element result = dom.createElement("productname");
        result.setAttribute("class", "registered");
        result.appendChild(dom.createTextNode("Amigaguide"));
        return result;
    }

    protected Node createDataLinkNode(File mappedFile, String mappedNode, String linkLabel) {
        Element result = dom.createElement("link");
        result.setAttribute("linkend", mappedNode);
        result.appendChild(dom.createTextNode(linkLabel));
        return result;
    }

    protected Node createOtherFileLinkNode(File linkedFile, String linkLabel) {
        // TODO: Copy linked file to same
        // folder as target document.
        URL linkedUrl = createUrl("file", "localhost", linkedFile);
        Element result = dom.createElement("ulink");
        result.setAttribute("url", linkedUrl.toExternalForm());
        result.appendChild(dom.createTextNode(linkLabel));
        return result;
    }

    private Node createEmbeddedFile(File embeddedFile) {
        Element result = createParagraph(Wrap.NONE, false);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(embeddedFile),
                    AmigaTools.ENCODING));
            try {
                String textLine = reader.readLine();
                while (textLine != null) {
                    result.appendChild(dom.createTextNode(textLine + "\n"));
                    textLine = reader.readLine();
                }
            } finally {
                reader.close();
            }
        } catch (UnsupportedEncodingException error) {
            throw new IllegalStateException("Amiga encoding not supported", error);
        } catch (IOException error) {
            result = dom.createElement("caution");
            Element cautionTitle = dom.createElement("title");
            Element cautionContent = dom.createElement("para");
            cautionTitle.appendChild(dom.createTextNode("Missing embedded file"));
            cautionContent.appendChild(dom.createTextNode("@embed for " + tools.sourced(embeddedFile) + " failed: "
                    + error.getMessage()));
            result.appendChild(cautionTitle);
            result.appendChild(cautionContent);
        }

        return result;
    }

    protected URL createUrl(String type, String host, File file) {
        try {
            return new URL("file", "localhost", file.getAbsolutePath());
        } catch (MalformedURLException error) {
            IllegalArgumentException wrappedError = new IllegalArgumentException("cannot create file URL for "
                    + tools.sourced(file), error);
            throw wrappedError;
        }
    }

    protected Node createLinkToNonGuideNode(File linkedFile, String linkLabel) {
        URL linkedUrl = createUrl("file", "localhost", linkedFile);
        Element elementToAppend = dom.createElement("ulink");
        elementToAppend.setAttribute("url", linkedUrl.toExternalForm());
        elementToAppend.appendChild(dom.createTextNode(linkLabel));
        return elementToAppend;
    }

    private String nodeKey(Guide guideContainingNode, String nodeName) {
        assert guideContainingNode != null;
        assert nodeName != null;
        assert nodeName.equals(nodeName.toLowerCase()) : "nodeName must be lower case but is: "
                + tools.sourced(nodeName);
        assert guideContainingNode.getNodeInfo(nodeName) != null : "guide must contain node " + tools.sourced(nodeName)
                + ": " + guideContainingNode.getSource().getFullName();

        String result = nodeName + "@" + guideContainingNode.getSource().getFullName().replaceAll("\\@", "@@");
        return result;
    }

    private String nodeKey(Guide guideContainingNode, NodeInfo nodeInfo) {
        return nodeKey(guideContainingNode, nodeInfo.getName());
    }

    private void createDom() throws ParserConfigurationException {
        log.info("create dom");
        DocumentBuilderFactory domBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder domBuilder = domBuilderFactory.newDocumentBuilder();
        dom = domBuilder.newDocument();
        bookElement = dom.createElement("book");
        dom.appendChild(bookElement);
        if (pile.getGuides().size() > 0) {
            Node metaInfoNode = createMetaInfoNode(pile.getGuides().get(0));
            if (metaInfoNode != null) {
                bookElement.appendChild(metaInfoNode);
            }
        }
        for (Guide guide : pile.getGuides()) {
            bookElement.appendChild(createChapter(guide));
        }
    }

    private Node createMetaInfoNode(Guide guide) {
        Element result;
        if (guide == pile.getGuides().get(0)) {
            DatabaseInfo dbInfo = guide.getDatabaseInfo();
            result = dom.createElement("bookinfo");

            // Add document title.
            String title = dbInfo.getName();
            assert title != null;
            Element titleNode = dom.createElement("title");
            titleNode.appendChild(dom.createTextNode(title));
            result.appendChild(titleNode);

            // Add optional author(s).
            String authorText = dbInfo.getAuthor();
            String[] authors = tools.separated(authorText);
            if (authors != null) {
                for (String author : authors) {
                    author = tools.cutOffAt(author, '(');
                    author = tools.cutOffAt(author, ',');
                    author = author.replace('\t', ' ');
                    author = author.trim();
                    if (author.length() > 0) {
                        Element authorElement = dom.createElement("author");
                        String[] nameParts = author.split(" ");
                        for (int partIndex = 0; partIndex < nameParts.length; partIndex += 1) {
                            String namePart = nameParts[partIndex];
                            String namePartElementName;
                            if ((nameParts.length > 1) && (partIndex == 0)) {
                                namePartElementName = "firstname";
                            } else if (partIndex == (nameParts.length - 1)) {
                                namePartElementName = "surname";
                            } else {
                                namePartElementName = "othername";
                            }
                            Element nameElement = dom.createElement(namePartElementName);
                            nameElement.appendChild(dom.createTextNode(namePart));
                            authorElement.appendChild(nameElement);
                        }
                        result.appendChild(authorElement);
                    }
                }
            }

            // Add (optional) version information.
            String versionText = dbInfo.getVersion();
            if (versionText != null) {
                Element versionElement = dom.createElement("releaseinfo");
                versionElement.appendChild(dom.createTextNode("$VER: " + versionText));
                result.appendChild(versionElement);
            }

            // Add (optional) copyright information.
            String copyrightYear = dbInfo.getCopyrightYear();
            if (copyrightYear != null) {
                String copyrightHolder = dbInfo.getCopyrightHolder();
                assert copyrightHolder != null;

                Element copyrightElement = dom.createElement("copyright");
                Element yearElement = dom.createElement("year");
                Element holderElement = dom.createElement("holder");
                yearElement.appendChild(dom.createTextNode(copyrightYear));
                holderElement.appendChild(dom.createTextNode(copyrightHolder));
                copyrightElement.appendChild(yearElement);
                copyrightElement.appendChild(holderElement);
                result.appendChild(copyrightElement);
            }

        } else {
            result = null;
        }
        return result;
    }

    private void writeDom() throws TransformerException {
        log.info("write dom");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//OASIS//DTD DocBook XML V4.5//EN");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                "http://www.oasis-open.org/docbook/xml/4.5/docbookx.dtd");
        // TODO: Remove: transformer.setOutputProperty(OutputKeys.ENCODING,
        // "ISO-8859-1");
        transformer.setOutputProperty(OutputKeys.ENCODING, OUTPUT_ENCODING);
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(dom), new StreamResult(writer));
    }

    private Element createChapter(Guide guide) {
        assert guide != null;

        Element result = dom.createElement("chapter");
        String chapterTitle = guide.getDatabaseInfo().getName();

        log.info("create chapter " + tools.sourced(chapterTitle));
        // Create chapter title.
        Element title = dom.createElement("title");
        Text titleText = dom.createTextNode(chapterTitle);
        title.appendChild(titleText);
        result.appendChild(title);

        for (NodeInfo nodeInfo : guide.getNodeInfos()) {
            result.appendChild(createSection(guide, nodeInfo));
        }
        return result;
    }

    private Element createParagraph(Wrap wrap, boolean isProportional) {
        Element result;
        String tagName;

        if (wrap == Wrap.NONE) {
            tagName = "literallayout";
        } else {
            tagName = "para";
            assert wrap != Wrap.DEFAULT;
        }
        result = dom.createElement(tagName);
        if (!isProportional) {
            result.setAttribute("class", "monospaced");
        }

        return result;
    }

    private Element createSection(Guide guide, NodeInfo nodeInfo) {
        assert guide != null;
        assert nodeInfo != null;

        Element result = dom.createElement("section");
        String sectionId = agNodeToDbNodeMap.get(nodeKey(guide, nodeInfo));
        String sectionTitle = nodeInfo.getTitle();

        log.log(Level.INFO, "create section with id={0} from node {1}: {2}", new Object[] { tools.sourced(sectionId),
                tools.sourced(nodeInfo.getName()), tools.sourced(sectionTitle) });
        result.setAttribute("id", sectionId);

        // Create title.
        Element title = dom.createElement("title");
        Text titleText = dom.createTextNode(sectionTitle);
        title.appendChild(titleText);
        result.appendChild(title);

        // Traverse node items.
        NodeParserState parserState = NodeParserState.BEFORE_NODE;
        Wrap wrap = nodeInfo.getWrap();
        boolean isProportional = nodeInfo.isProportional();
        Element paragraph = createParagraph(wrap, isProportional);
        String text = "";
        boolean lastTextWasNewLine = false;

        for (AbstractItem item : guide.getItems()) {
            log.log(Level.FINER, "parserState={0}: {1}", new Object[] { parserState, item });
            if (parserState == NodeParserState.BEFORE_NODE) {
                if (item == nodeInfo.getStartNode()) {
                    parserState = NodeParserState.INSIDE_NODE;
                    log.log(Level.FINER, "found start node: {0}", item);
                }
            } else if (parserState == NodeParserState.INSIDE_NODE) {
                if (item == nodeInfo.getEndNode()) {
                    parserState = NodeParserState.AFTER_NODE;
                    log.log(Level.FINER, "found end node: {0}", item);
                } else {
                    boolean flushText = false;
                    boolean flushParagraph = false;
                    Node nodeToAppend = null;
                    Node nodeToAppendAfterParagraph = null;

                    if (item instanceof SpaceItem) {
                        text += ((SpaceItem) item).getSpace();
                        lastTextWasNewLine = false;
                    } else if (item instanceof AbstractTextItem) {
                        text += ((AbstractTextItem) item).getText();
                        lastTextWasNewLine = false;
                    } else if (item instanceof NewLineItem) {
                        if (wrap == Wrap.NONE) {
                            text += "\n";
                        } else if (wrap == Wrap.SMART) {
                            if (lastTextWasNewLine) {
                                flushText = true;
                                flushParagraph = true;
                                lastTextWasNewLine = false;
                            } else {
                                text += "\n";
                                lastTextWasNewLine = true;
                            }
                        } else if (wrap == Wrap.WORD) {
                            flushText = true;
                            flushParagraph = true;
                        } else {
                            assert false : "wrap=" + wrap;
                        }
                    } else if (item instanceof CommandItem) {
                        CommandItem command = (CommandItem) item;
                        String commandName = command.getCommandName();
                        Tag.Name commandTag = Tag.Name.valueOfOrNull(commandName);
                        if (command.isLink()) {
                            // Create and append link.
                            log.log(Level.FINE, "connect link: {0}", command);
                            Link link = pile.getLink(command);
                            String linkLabel = command.getLinkLabel();
                            if (link != null) {
                                if (link.getState() == Link.State.VALID) {
                                    // Valid link to Amigaguide document and
                                    // node.
                                    Link.Type linkType = link.getType();
                                    String targetNode = link.getTargetNode();
                                    File linkedFile = link.getTargetFile();
                                    Guide targetGuide = pile.getGuide(linkedFile);

                                    if (linkType == Link.Type.guide) {
                                        // Assert that target node has been set
                                        // by validateLinks().
                                        assert targetNode != null;
                                    } else {
                                        // Assert that all @{alink}s have been
                                        // changed to @{link}.
                                        assert linkType == Link.Type.link : "linkType=" + linkType;
                                    }

                                    if (targetGuide != null) {
                                        // Link within DocBook document.
                                        String mappedNode = agNodeToDbNodeMap.get(nodeKey(targetGuide, targetNode));

                                        if (link.isDataLink()) {
                                            if (mappedNode != null) {
                                                nodeToAppend = createDataLinkNode(null, mappedNode, linkLabel);
                                            } else {
                                                log.warning("skipped link to unknown node: "
                                                        + command.toPrettyAmigaguide());
                                            }
                                        }
                                    } else if (linkedFile.exists()) {
                                        nodeToAppend = createOtherFileLinkNode(linkedFile, linkLabel);
                                    } else {
                                        log.warning("skipped link to unknown file: " + command.toPrettyAmigaguide());
                                    }
                                } else if (link.getState() == Link.State.VALID_OTHER_FILE) {
                                    // Valid link to non-Amigaguide file.
                                    log.log(Level.FINE, "connect to non-guide: {0}", command);
                                    nodeToAppend = createLinkToNonGuideNode(link.getTargetFile(), link.getLabel());
                                } else {
                                    log.warning("skipped link with state=" + link.getState() + ": "
                                            + command.toPrettyAmigaguide());
                                }
                            } else {
                                log.warning("skipped invalid link: " + command.toPrettyAmigaguide());
                            }

                            // Link was not appended for some reason, so at
                            // least make sure the link label shows up.
                            if (nodeToAppend == null) {
                                text += linkLabel;
                            } else {
                                flushText = true;
                            }
                        } else if (commandTag == Tag.Name.amigaguide) {
                            // Replace @{amigaguide} by text.
                            flushText = true;
                            nodeToAppend = createAmigaguideNode();
                        } else if (commandTag == Tag.Name.embed) {
                            // Include content specified by @embed
                            // FIXME: Add @embed base path.
                            File embeddedFile = amigaTools.getFileFor(command.getOption(0));
                            flushText = true;
                            flushParagraph = true;
                            log.log(Level.INFO, "embed: {0}", tools.sourced(embeddedFile));
                            nodeToAppendAfterParagraph = createEmbeddedFile(embeddedFile);
                        }
                    }
                    if (flushText) {
                        log.log(Level.FINER, "append text: {0}", tools.sourced(text));
                        if (nodeToAppend == null) {
                            text = withoutPossibleTrailingNewLine(text);
                        }
                        if (text.length() > 0) {
                            paragraph.appendChild(dom.createTextNode(text));
                        }
                        text = "";
                    }
                    if (nodeToAppend != null) {
                        paragraph.appendChild(nodeToAppend);
                    }
                    if (flushParagraph) {
                        result.appendChild(paragraph);
                        paragraph = createParagraph(wrap, isProportional);
                    }
                    if (nodeToAppendAfterParagraph != null) {
                        result.appendChild(nodeToAppendAfterParagraph);
                    }
                }
            } else {
                assert parserState == NodeParserState.AFTER_NODE : "parserState=" + parserState;
                // Do nothing, just move on past the end.
            }
        }

        assert parserState != NodeParserState.INSIDE_NODE : "parserState=" + parserState;

        if (text.length() > 0) {
            paragraph.appendChild(dom.createTextNode(withoutPossibleTrailingNewLine(text)));
            result.appendChild(paragraph);
        }

        return result;
    }

    /**
     * Same as <code>some</code> except if the last character is a new line
     * ("\n") in which case it will be removed. This is useful at the end of a
     * paragraph because &lt;literallayout&gt; or &lt;para&gt; insert a newline
     * anyway.
     */
    private String withoutPossibleTrailingNewLine(String some) {
        String result;
        if (some.endsWith("\n")) {
            result = some.substring(0, some.length() - 1);
        } else {
            result = some;
        }
        return result;
    }

    public static void write(GuidePile pile, Writer targetWriter) throws ParserConfigurationException,
            TransformerException {
        assert pile != null;
        assert targetWriter != null;
        DocBookWriter docBookWriter = new DocBookWriter(pile, targetWriter);
        docBookWriter.createDom();
        docBookWriter.writeDom();
    }

    public static void write(GuidePile pile, File targetFile) throws IOException, ParserConfigurationException,
            TransformerException {
        assert pile != null;
        assert targetFile != null;

        Writer targetWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile),
                OUTPUT_ENCODING));
        try {
            write(pile, targetWriter);
        } finally {
            targetWriter.close();
        }
    }
}
