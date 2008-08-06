package net.sf.grotag.guide;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

import net.sf.grotag.common.Tools;
import net.sf.grotag.parse.AbstractItem;
import net.sf.grotag.parse.AbstractTextItem;
import net.sf.grotag.parse.CommandItem;
import net.sf.grotag.parse.NewLineItem;
import net.sf.grotag.parse.SpaceItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class DocBookWriter {
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

    private DocBookWriter(GuidePile newPile, Writer newWriter) {
        assert newPile != null;
        assert newWriter != null;

        log = Logger.getLogger(DocBookWriter.class.getName());
        tools = Tools.getInstance();
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
        for (Guide guide : pile.getGuides()) {
            bookElement.appendChild(createChapter(guide));
        }
    }

    private void writeDom() throws TransformerException {
        log.info("write dom");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//OASIS//DTD DocBook XML V4.5//EN");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                "http://www.oasis-open.org/docbook/xml/4.5/docbookx.dtd");
        transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
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

    private Element createElementForWrap(Wrap wrap) {
        Element result;
        String tagName;

        if (wrap == Wrap.NONE) {
            tagName = "literallayout";
        } else {
            tagName = "para";
            assert wrap != Wrap.DEFAULT;
        }
        result = dom.createElement(tagName);
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
        Element paragraph = createElementForWrap(wrap);
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
                    Element linkToAppend = null;

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
                        if (command.isLink()) {
                            // Create and append link.
                            log.log(Level.INFO, "connect link: {0}", command);
                            Link link = new Link(command);
                            String linkType = link.getType();
                            String targetNode = link.getTargetNode();
                            boolean isLocalLink = linkType.equals("link");
                            String linkLabel = link.getLabel();
                            Text linkDescriptionText = dom.createTextNode(linkLabel);
                            File linkedFile = link.getTargetFile();
                            Guide targetGuide = pile.getGuide(linkedFile);

                            if (targetGuide != null) {
                                // Link within DocBook document.
                                String mappedNode = agNodeToDbNodeMap.get(nodeKey(targetGuide, targetNode));

                                if (isLocalLink) {
                                    if (mappedNode != null) {
                                        linkToAppend = dom.createElement("link");
                                        linkToAppend.setAttribute("linkend", mappedNode);
                                        linkToAppend.appendChild(linkDescriptionText);
                                    } else {
                                        log.warning("skipped link to unknown node: " + command.toPrettyAmigaguide());
                                    }
                                }
                            } else if (linkedFile.exists()) {
                                try {
                                    // TODO: Copy linked file to same folder as
                                    // target document.
                                    URL linkedUrl = new URL("file", "localhost", linkedFile.getAbsolutePath());
                                    linkToAppend = dom.createElement("ulink");
                                    linkToAppend.setAttribute("url", linkedUrl.toExternalForm());
                                    linkToAppend.appendChild(linkDescriptionText);
                                } catch (MalformedURLException error) {
                                    IllegalArgumentException wrapperError = new IllegalArgumentException(
                                            "cannot create file URL for " + tools.sourced(linkedFile), error);
                                    throw wrapperError;
                                }
                            } else {
                                log.warning("skipped link to unknown file: " + command.toPrettyAmigaguide());
                            }

                            // Link was not appended for some reason, so at
                            // least make sure the link label shows up.
                            if (linkToAppend == null) {
                                text += linkLabel;
                            } else {
                                flushText = true;
                            }
                        }
                    }
                    if (flushText) {
                        log.log(Level.FINER, "append text: {0}", tools.sourced(text));
                        if (text.length() > 0) {
                            paragraph.appendChild(dom.createTextNode(withoutPossibleTrailingNewLine(text)));
                        }
                        text = "";
                    }
                    if (linkToAppend != null) {
                        paragraph.appendChild(linkToAppend);
                    }
                    if (flushParagraph) {
                        result.appendChild(paragraph);
                        paragraph = createElementForWrap(wrap);
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

        Writer targetWriter = new BufferedWriter(new FileWriter(targetFile));
        try {
            write(pile, targetWriter);
        } finally {
            targetWriter.close();
        }
    }
}
