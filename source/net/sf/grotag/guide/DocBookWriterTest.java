package net.sf.grotag.guide;

import static org.junit.Assert.*;

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
    public void testWriteGuideFile() throws IOException, ParserConfigurationException, TransformerException {
        File inFile = testTools.getTestInputFile("basics.guide");
        File outFile = testTools.getTestActualFile("basics.xml");
        Guide guide = Guide.createGuide(inFile);
        DocBookWriter.write(guide, outFile);
        assertTrue(outFile.exists());
    }
}
