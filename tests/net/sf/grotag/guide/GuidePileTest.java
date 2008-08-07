package net.sf.grotag.guide;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import net.sf.grotag.common.TestTools;
import net.sf.grotag.common.Tools;

import org.junit.Before;
import org.junit.Test;

public class GuidePileTest {
    private TestTools testTools;
    private Tools tools;
    private Logger log;

    @Before
    public void setUp() throws Exception {
        tools = Tools.getInstance();
        testTools = TestTools.getInstance();
        log = Logger.getLogger(GuidePileTest.class.getName());
    }

    @Test
    public void testAdd() throws IOException {
        File rootGuideFile = testTools.getTestInputFile("root.guide");
        GuidePile pile = GuidePile.createGuidePile(rootGuideFile);
        assertEquals(4, pile.getGuides().size());
    }

    @Test
    public void testRkrm() throws IOException {
        File rkrmDevicesFolder = testTools.getTestGuideFile("reference_library");
        File rkrmDevicesGuideFile = new File(new File(rkrmDevicesFolder, "devices"), "Dev_1");

        try {
            GuidePile pile = GuidePile.createGuidePile(rkrmDevicesGuideFile);
            assertEquals(29, pile.getGuides().size());
        } catch (FileNotFoundException errorToIgnore) {
            log.warning("skipped test for " + tools.sourced(rkrmDevicesGuideFile));
        }
    }
}
