package net.sf.grotag.guide;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import net.sf.grotag.common.TestTools;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test case for HtmlDocFactory.
 * 
 * @author Thomas Aglassinger
 */
public class HtmlDomFactoryTest {
    private TestTools testTools;

    @Before
    public void setUp() throws Exception {
        testTools = TestTools.getInstance();
    }

    @Test
    public void testCreateNodeDocument() throws IOException, ParserConfigurationException, TransformerException {
        File guideFile = testTools.getTestInputFile("basics.guide");
        File targetFolder = testTools.getTestActualFile("basics");
        targetFolder.mkdirs();

        GuidePile pile = GuidePile.createGuidePile(guideFile);
        Guide guide = pile.getGuides().get(0);
        NodeInfo nodeInfo = guide.getNodeInfos().get(0);
        HtmlDomFactory factory = new HtmlDomFactory(pile, guide, nodeInfo);
        Document htmlDocument = factory.createNodeDocument();
        assertNotNull(htmlDocument);

        File targetFile = new File(targetFolder, nodeInfo.getName() + ".html");
        DomWriter htmlWriter = new DomWriter(DomWriter.Dtd.HTML);
        htmlWriter.write(htmlDocument, targetFile);
    }
}
