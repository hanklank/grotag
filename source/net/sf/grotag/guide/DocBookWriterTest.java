package net.sf.grotag.guide;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import net.sf.grotag.common.TestTools;

import org.junit.Before;
import org.junit.Test;

public class DocBookWriterTest {
    TestTools testTools;

    @Before
    public void setUp() throws Exception {
        testTools = TestTools.getInstance();
    }

    @Test
    public void testWriteBasicsGuide() throws IOException, ParserConfigurationException, TransformerException {
        File inFile = testTools.getTestInputFile("basics.guide");
        File outFile = testTools.getTestActualFile("basics.xml");
        GuidePile pile = new GuidePile();
        pile.addRecursive(inFile);
        DocBookWriter.write(pile, outFile);
        assertTrue(outFile.exists());
    }

    @Test
    public void testWriteRootGuide() throws IOException, ParserConfigurationException, TransformerException {
        File inFile = testTools.getTestInputFile("root.guide");
        File outFile = testTools.getTestActualFile("root.xml");
        GuidePile pile = new GuidePile();
        pile.addRecursive(inFile);
        DocBookWriter.write(pile, outFile);
        assertTrue(outFile.exists());
    }
}
