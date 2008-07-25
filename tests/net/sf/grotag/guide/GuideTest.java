package net.sf.grotag.guide;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class GuideTest {

    @Before
    public void setUp() throws Exception {
        // DO nothing.
    }
    
    private File getTestFile(String fileName) {
        return new File(new File("tests", "input"), fileName);
    }
    @Test
    public void testMacroGuide() throws Exception {
        // Guide guide = Guide.createGuide(getTestFile("macros.guide"));
        Guide guide = Guide.createGuide(getTestFile("nodes.guide"));
        assertNotNull(guide);
    }

}
