package net.sf.grotag.guide;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import net.sf.grotag.common.AmigaTools;
import net.sf.grotag.common.Tools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class HtmlDomFactory extends AbstractDomFactory {
    private GuidePile pile;
    private File pileBaseFolder;
    private File pileTargetFolder;
    private Tools tools;
    private Map<String, File> targetFileMap;
    private File styleFile;

    public HtmlDomFactory(GuidePile newPile, File newPileTargetFolder) throws ParserConfigurationException {
        super(newPile);
        assert newPileTargetFolder != null;

        tools = Tools.getInstance();

        pile = newPile;
        pileTargetFolder = newPileTargetFolder;

        List<Guide> guides = pile.getGuides();
        assert guides != null;
        assert guides.size() > 0;
        Guide baseGuide = guides.get(0);
        pileBaseFolder = baseGuide.getSourceFile().getParentFile();
        targetFileMap = createTargetFileMap();
        styleFile = new File(pileTargetFolder, "amigaguide.css");
    }

    public File getTargetFileFor(Guide guide, NodeInfo nodeInfo) {
        File result = targetFileMap.get(nodeKey(guide, nodeInfo));
        assert result != null;
        return result;
    }

    private Map<String, File> createTargetFileMap() {
        Map<String, File> result = new HashMap<String, File>();

        for (Guide guide : pile.getGuides()) {
            Map<String, String> nodeToFileNameMap = new HashMap<String, String>();
            Set<String> fileNameSet = new HashSet<String>();
            File guideFile = guide.getSourceFile();
            String relativeGuideFolder = tools.getRelativePath(pileBaseFolder, guideFile);
            File htmlTargetFolder = new File(pileTargetFolder, tools.getWithoutLastSuffix(relativeGuideFolder));

            for (NodeInfo nextNodeInfo : guide.getNodeInfos()) {
                String nodeName = nextNodeInfo.getName();
                assert nodeName.equals(nodeName.toLowerCase());

                // Make sure the "main" node ends up in the HTML file "index".
                if (nodeName.equals("main")) {
                    nodeName = "index";
                } else if (nodeName.equals("index")) {
                    nodeName = "list";
                }
                String fileName = nodeName;
                int uniqueCounter = 0;
                while (fileNameSet.contains(fileName)) {
                    uniqueCounter += 1;
                    fileName = nodeName + "." + uniqueCounter;
                }
                fileNameSet.add(fileName);
                nodeToFileNameMap.put(nodeName, fileName);
                File htmlTargetFile = new File(htmlTargetFolder, fileName + ".html");
                result.put(nodeKey(guide, nextNodeInfo), htmlTargetFile);
            }
        }
        return result;
    }

    @Override
    protected Node createAmigaguideNode() {
        Element result = getDom().createElement("span");
        result.setAttribute("class", "b");
        result.appendChild(getDom().createTextNode("Amigaguide\u00ae"));
        return result;
    }

    @Override
    protected Node createDataLinkNode(Guide sourceGuide, File mappedFile, String mappedNode, String linkLabel) {
        Element result = getDom().createElement("a");
        File sourceFile = sourceGuide.getSourceFile();
        Guide targetGuide = pile.getGuide(mappedFile);
        NodeInfo targetNodeInfo = targetGuide.getNodeInfo(mappedNode);
        File targetFile = getTargetFileFor(targetGuide, targetNodeInfo);
        String relativeTargetUrl = tools.getRelativeUrl(sourceFile, targetFile);
        System.out.println(tools.sourced(relativeTargetUrl));

        result.setAttribute("href", relativeTargetUrl);
        System.out.println(linkLabel);
        result.appendChild(getDom().createTextNode(linkLabel));
        return result;
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

    public Document createNodeDocument(Guide guide, NodeInfo nodeInfo) throws ParserConfigurationException {
        createDom();
        Element html = getDom().createElement("html");
        Element head = createHead(guide, nodeInfo);
        html.appendChild(head);
        Element body = createNodeBody(guide, nodeInfo);
        appendNodeContent(body, guide, nodeInfo);
        getDom().appendChild(html);
        html.appendChild(body);
        return getDom();
    }

    @Override
    protected Node createLinkToNonGuideNode(Guide sourceGuide, File linkedFile, String linkLabel) {
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
    private Element createHead(Guide guide, NodeInfo nodeInfo) {
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

        // Append style sheet.
        String styleUrl = tools.getRelativeUrl(targetFileMap.get(nodeKey(guide, nodeInfo)), getStyleFile());
        Element style = getDom().createElement("link");
        style.setAttribute("rel", "stylesheet");
        style.setAttribute("type", "text/css");
        style.setAttribute("href", styleUrl);
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

        // Append keywords.
        boolean isFirstNodeInGuide = (guide.getNodeInfos().get(0) == nodeInfo);
        if (isFirstNodeInGuide) {
            // TODO: Add keywords.
        }

        return result;
    }

    @Override
    protected Node createOtherFileLinkNode(Guide sourceGuide, File linkedFile, String linkLabel) {
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

    public void copyStyleFile(File cssFile) throws IOException {
        tools.copyFile(cssFile, getStyleFile());
    }

    /**
     * The CSS style file used by all HTML files generated by this factory.
     */
    public File getStyleFile() {
        return styleFile;
    }
}
