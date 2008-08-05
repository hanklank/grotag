package net.sf.grotag.guide;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import net.sf.grotag.common.TestTools;
import net.sf.grotag.common.Tools;

import org.junit.Before;
import org.junit.Test;

public class GuidePileTest {
    private TestTools testTools;
    private Logger log;

    @Before
    public void setUp() throws Exception {
        testTools = TestTools.getInstance();
        log = Logger.getLogger(GuidePileTest.class.getName());
    }

    @Test
    public void testAdd() throws IOException {
        File rootGuideFile = testTools.getTestInputFile("root.guide");
        GuidePile pile = new GuidePile();
        pile.addRecursive(rootGuideFile);
        assertEquals(3, pile.getGuides().size());
        pile.validateLinks();
    }

    @Test
    public void testRkrm() throws IOException {
        File rkrmDevicesFolder = testTools.getTestGuideFile("reference_library");
        File rkrmDevicesGuide = new File(new File(rkrmDevicesFolder, "devices"), "Dev_1");

        if (rkrmDevicesGuide.exists()) {
            GuidePile pile = new GuidePile();
            pile.addRecursive(rkrmDevicesGuide);
            assertEquals(29, pile.getGuides().size());
            pile.validateLinks();
        } else {
            log.warning("skipped test for " + Tools.getInstance().sourced(rkrmDevicesGuide));
        }
    }
}
