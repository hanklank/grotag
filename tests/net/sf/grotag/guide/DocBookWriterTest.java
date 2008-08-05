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

    private void testWriteGuide(String baseName) throws IOException, ParserConfigurationException, TransformerException {
        File inFile = testTools.getTestInputFile(baseName + ".guide");
        File outFile = testTools.getTestActualFile(baseName + ".xml");
        GuidePile pile = new GuidePile();
        
        if (!inFile.exists()) {
            inFile = testTools.getTestGuideFile(baseName + ".guide");
        }
        pile.addRecursive(inFile);
        DocBookWriter.write(pile, outFile);
        assertTrue(outFile.exists());
    }

    @Test
    public void testWriteBasicsGuide() throws IOException, ParserConfigurationException, TransformerException {
        testWriteGuide("basics");
    }

    @Test
    public void testWriteRootGuide() throws IOException, ParserConfigurationException, TransformerException {
        testWriteGuide("root");
    }

    @Test
    public void testWriteAgrGuide() throws IOException, ParserConfigurationException, TransformerException {
        testWriteGuide("agr");
    }

    @Test
    public void testWriteAgrTestGuide() throws IOException, ParserConfigurationException, TransformerException {
        testWriteGuide("agr_test");
    }
}
