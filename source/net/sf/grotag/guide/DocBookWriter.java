package net.sf.grotag.guide;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
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

    private Guide guide;
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

    private DocBookWriter(Guide newGuide, Writer newWriter) {
        assert newGuide != null;
        assert newWriter != null;

        log = Logger.getLogger(DocBookWriter.class.getName());
        tools = Tools.getInstance();
        guide = newGuide;
        writer = newWriter;

        agNodeToDbNodeMap = new HashMap<String, String>();
        int nodeCounter = 1;
        for (NodeInfo nodeInfo : guide.getNodeInfos()) {
            String agNodeName = nodeInfo.getName();
            String dbNodeName = "n" + nodeCounter;

            agNodeToDbNodeMap.put(agNodeName, dbNodeName);
            nodeCounter += 1;
        }
    }

    private void createDom() throws ParserConfigurationException {
        DocumentBuilderFactory domBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder domBuilder = domBuilderFactory.newDocumentBuilder();
        dom = domBuilder.newDocument();
        bookElement = dom.createElement("book");
        dom.appendChild(bookElement);
        bookElement.appendChild(createChapter());
    }

    private void writeDom() throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        // set all necessary features for your transformer -> see OutputKeys
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//OASIS//DTD DocBook XML V4.1.2//EN");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                "http://www.oasis-open.org/docbook/xml/4.1.2/docbookx.dtd");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(dom), new StreamResult(writer));
    }

    private Element createChapter() {
        Element result = dom.createElement("chapter");

        // Create chapter title.
        Element title = dom.createElement("title");
        Text titleText = dom.createTextNode(guide.getDatabaseInfo().getName());
        title.appendChild(titleText);
        result.appendChild(title);

        for (NodeInfo nodeInfo : guide.getNodeInfos()) {
            result.appendChild(createSection(nodeInfo));
        }
        return result;
    }

    private Element createSection(NodeInfo nodeInfo) {
        Element result = dom.createElement("section");
        result.setAttribute("id", agNodeToDbNodeMap.get(nodeInfo.getName()));

        // Create title.
        Element title = dom.createElement("title");
        Text titleText = dom.createTextNode(nodeInfo.getTitle());
        title.appendChild(titleText);
        result.appendChild(title);

        // Traverse node items.
        NodeParserState parserState = NodeParserState.BEFORE_NODE;
        Element paragraph = dom.createElement("literallayout");
        String text = "";

        for (AbstractItem item : guide.getItems()) {
            log.fine("parserState=" + parserState);
            if (parserState == NodeParserState.BEFORE_NODE) {
                if (item == nodeInfo.getStartNode()) {
                    parserState = NodeParserState.INSIDE_NODE;
                    log.fine("found start node" + item);
                }
            } else if (parserState == NodeParserState.INSIDE_NODE) {
                if (item == nodeInfo.getEndNode()) {
                    parserState = NodeParserState.AFTER_NODE;
                    log.fine("found end node" + item);
                } else {
                    if (item instanceof SpaceItem) {
                        text += ((SpaceItem) item).getSpace();
                    } else if (item instanceof AbstractTextItem) {
                        text += ((AbstractTextItem) item).getText();
                    } else if (item instanceof NewLineItem) {
                        text += "\n";
                    } else if (item instanceof CommandItem) {
                        CommandItem command = (CommandItem) item;
                        if (command.isLink()) {
                            boolean isLocalLink = command.getOption(0).toLowerCase().equals("link");
                            // Append current text so far.
                            paragraph.appendChild(dom.createTextNode(text));
                            text = "";
                            // Create and append link.
                            String linkDescription = command.getOriginalCommandName();
                            linkDescription = linkDescription.substring(1, linkDescription.length() - 1);
                            Text linkDescriptionText = dom.createTextNode(linkDescription);

                            if (isLocalLink) {
                                Element link = dom.createElement("link");
                                link.setAttribute("linkend", agNodeToDbNodeMap.get(command.getOption(1)));
                                link.appendChild(linkDescriptionText);
                                paragraph.appendChild(link);
                            } else {
                                paragraph.appendChild(linkDescriptionText);
                            }
                        }
                    }
                    log.fine(tools.sourced(text));
                }
            } else {
                assert parserState == NodeParserState.AFTER_NODE : "parserState=" + parserState;
                // Do nothing, just move on past the end.
            }
        }

        assert parserState != NodeParserState.INSIDE_NODE : "parserState=" + parserState;

        if (text.length() > 0) {
            // Remove last newline because it is inserted anyway by <paragraph>
            // or <literallayout>.
            if (text.endsWith("\n")) {
                text = text.substring(0, text.length() - 1);
            }
            paragraph.appendChild(dom.createTextNode(text));
            result.appendChild(paragraph);
        }

        return result;
    }

    public static void write(Guide sourceGuide, Writer targetWriter) throws ParserConfigurationException,
            TransformerException {
        assert sourceGuide != null;
        assert targetWriter != null;
        DocBookWriter docBookWriter = new DocBookWriter(sourceGuide, targetWriter);
        docBookWriter.createDom();
        docBookWriter.writeDom();
    }

    public static void write(Guide sourceGuide, File targetFile) throws IOException, ParserConfigurationException,
            TransformerException {
        assert sourceGuide != null;
        assert targetFile != null;

        Writer targetWriter = new BufferedWriter(new FileWriter(targetFile));
        try {
            write(sourceGuide, targetWriter);
        } finally {
            targetWriter.close();
        }
    }
}
