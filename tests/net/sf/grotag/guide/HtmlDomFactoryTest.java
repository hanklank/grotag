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
        String testName = testTools.getTestName(HtmlDomFactoryTest.class, "testCreateNodeDocument");
        File targetFolder = testTools.getTestActualFile(testName);

        GuidePile pile = GuidePile.createGuidePile(guideFile);
        Guide guide = pile.getGuides().get(0);
        HtmlDomFactory factory = new HtmlDomFactory(pile, targetFolder);
        factory.copyStyleFile(new File("source", "amigaguide.css"));
        for (NodeInfo nodeInfo : guide.getNodeInfos()) {
            Document htmlDocument = factory.createNodeDocument(guide, nodeInfo);
            assertNotNull(htmlDocument);

            File targetFile = factory.getTargetFileFor(guide, nodeInfo);
            DomWriter htmlWriter = new DomWriter(DomWriter.Dtd.HTML);
            htmlWriter.write(htmlDocument, targetFile);
        }
    }
}
