package net.sf.grotag.guide;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.ParserConfigurationException;

import net.sf.grotag.common.AmigaTools;
import net.sf.grotag.common.Tools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class HtmlDomFactory extends AbstractDomFactory {
    private Guide guide;
    private NodeInfo nodeInfo;
    private Tools tools;

    public HtmlDomFactory(GuidePile newPile, Guide newGuide, NodeInfo newNodeInfo) throws ParserConfigurationException {
        super(newPile);
        assert newGuide != null;
        assert newNodeInfo != null;

        tools = Tools.getInstance();

        guide = newGuide;
        nodeInfo = newNodeInfo;
    }

    @Override
    protected Node createAmigaguideNode() {
        Element result = getDom().createElement("span");
        result.setAttribute("class", "b");
        result.appendChild(getDom().createTextNode("Amigaguide\u00ae"));
        return result;
    }

    @Override
    protected Node createDataLinkNode(File mappedFile, String mappedNode, String linkLabel) {
        // TODO: Implement proper link.
        Text label = getDom().createTextNode(linkLabel);
        return label;
    }

    @Override
    // TODO: Consolidate and use createEmbeddedFailed instead.
    protected Node createEmbeddedFile(File embeddedFile) {
        Element result = createParagraph(Wrap.NONE, false);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(embeddedFile),
                    AmigaTools.ENCODING));
            try {
                String textLine = reader.readLine();
                while (textLine != null) {
                    result.appendChild(getDom().createTextNode(textLine + "\n"));
                    textLine = reader.readLine();
                }
            } finally {
                reader.close();
            }
        } catch (UnsupportedEncodingException error) {
            throw new IllegalStateException("Amiga encoding not supported", error);
        } catch (IOException error) {
            result = createParagraph(Wrap.SMART, true);
            result.appendChild(getDom().createTextNode("Missing embedded file"));
            result.appendChild(getDom().createTextNode(
                    "@embed for " + tools.sourced(embeddedFile) + " failed: " + error.getMessage()));
        }

        return result;
    }

    public Document createNodeDocument() {
        Element html = getDom().createElement("html");
        Element head = createHead();
        html.appendChild(head);
        Element body = createNodeBody(guide, nodeInfo);
        appendNodeContent(body, guide, nodeInfo);
        getDom().appendChild(html);
        html.appendChild(body);
        return getDom();
    }

    @Override
    protected Node createLinkToNonGuideNode(File linkedFile, String linkLabel) {
        // TODO: Implement proper link.
        Text label = getDom().createTextNode(linkLabel);
        return label;
    }

    private Element createMetaElement(String name, String content) {
        assert name != null;
        assert name.length() > 0;
        assert content != null;
        assert content.length() > 0;

        Element result = getDom().createElement("meta");
        result.setAttribute("name", name);
        result.setAttribute("content", content);
        return result;
    }

    /**
     * Create <code>&lt;head&gt;</code> including meta elements according to
     * <a href="http://dublincore.org/">Dublin Core</a>.
     */
    private Element createHead() {
        assert guide != null;
        assert nodeInfo != null;

        Element result = getDom().createElement("head");
        result.setAttribute("profile", "http://dublincore.org/documents/2008/08/04/dc-html/");

        // Append title.
        Element title = getDom().createElement("title");
        DatabaseInfo dbInfo = guide.getDatabaseInfo();
        title.appendChild(getDom().createTextNode(dbInfo.getName()));
        result.appendChild(title);
        result.appendChild(createMetaElement("DC.title", dbInfo.getName()));

        // Append style sheet
        Element style = getDom().createElement("link");
        style.setAttribute("rel", "stylesheet");
        style.setAttribute("type", "text/css");
        // FIXME: Consider that CSS can be in parent folder.
        style.setAttribute("href", "amigaguide.css");
        result.appendChild(style);

        // Append author.
        String author = dbInfo.getAuthor();
        if (author != null) {
            result.appendChild(createMetaElement("DC.creator", author));
        }

        // Append copyright.
        String copyright = dbInfo.getCopyright();
        if (copyright != null) {
            result.appendChild(createMetaElement("DC.rights", copyright));
        }

        boolean isFirstNodeInGuide = (guide.getNodeInfos().get(0) == nodeInfo);
        if (isFirstNodeInGuide) {
            // TODO: Add keywords.
        }

        return result;
    }

    @Override
    protected Node createOtherFileLinkNode(File linkedFile, String linkLabel) {
        // TODO: Implement proper link.
        Text label = getDom().createTextNode(linkLabel);
        return label;
    }

    @Override
    protected Element createParagraph(Wrap wrap, boolean isProportional) {
        Element result;
        if (wrap == Wrap.NONE) {
            result = getDom().createElement("pre");
        } else {
            assert (wrap == Wrap.SMART) || (wrap == Wrap.WORD) : "wrap=" + wrap;
            result = getDom().createElement("p");
            if (!isProportional) {
                result.setAttribute("class", "monospace");
            }
        }
        return result;
    }

    @Override
    protected Element createNodeBody(Guide guide, NodeInfo nodeInfo) {
        Element result = getDom().createElement("body");
        return result;
    }

    @Override
    protected Element createNodeHeading(String heading) {
        assert heading != null;
        Element result = getDom().createElement("h1");
        result.appendChild(getDom().createTextNode(heading));
        return result;
    }
}
