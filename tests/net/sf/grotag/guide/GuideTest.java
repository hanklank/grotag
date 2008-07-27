package net.sf.grotag.guide;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Before;
import org.junit.Test;

/**
 * TestCase for <code>Guide</code>.
 * 
 * @author Thomas Aglassinger
 */
public class GuideTest {
    // TODO: Move to TestTools.
    private enum Folder {
        ACTUAL, EXPECTED, INPUT
    }

    @Before
    public void setUp() throws Exception {
        // DO nothing.
    }

    // TODO: Move to TestTools.
    private File getTestFile(Folder baseFolder, String fileName) {
        File folder = new File("tests", baseFolder.toString().toLowerCase());
        File result = new File(folder, fileName);

        // Make sure target folder for "actual" files exists, as it is not part
        // of the repository.
        if ((baseFolder == Folder.ACTUAL) && !folder.exists()) {
            folder.mkdirs();
        }
        return result;
    }

    // TODO: Move to TestTools.
    private File getTestFile(String fileName) {
        return getTestFile(Folder.INPUT, fileName);
    }

    // TODO: Move to TestTools.
    public void assertFilesAreEqual(String fileName) throws IOException {
        File expectedFile = getTestFile(Folder.EXPECTED, fileName);
        File actualFile = getTestFile(Folder.ACTUAL, fileName);
        assertFilesAreEqual(expectedFile, actualFile);
    }

    // TODO: Move to TestTools.
    public void assertFilesAreEqual(File expected, File actual) throws IOException {
        BufferedReader expectedReader = new BufferedReader(new InputStreamReader(new FileInputStream(expected)));
        try {
            BufferedReader actualReader = new BufferedReader(new InputStreamReader(new FileInputStream(actual)));
            try {
                int lineNumber = 0;
                String expectedLine;
                String actualLine;

                do {
                    lineNumber += 1;
                    expectedLine = expectedReader.readLine();
                    actualLine = actualReader.readLine();
                } while ((expectedLine != null) && (actualLine != null));
            } finally {
                actualReader.close();
            }
        } finally {
            expectedReader.close();
        }
    }

    private void testGuide(String fileName) throws IOException {
        File inFile = getTestFile(Folder.INPUT, fileName);
        File outFile = getTestFile(Folder.ACTUAL, fileName);
        Guide guide = Guide.createGuide(inFile);

        assertNotNull(guide);
        guide.writePretty(outFile);
        assertFilesAreEqual(fileName);
    }

    @Test
    public void testNodeGuide() throws Exception {
        testGuide("nodes.guide");
    }

    @Test
    public void testUniqueGuide() throws Exception {
        testGuide("unique.guide");
    }

    @Test
    public void testMacroGuide() throws Exception {
        Guide guide = Guide.createGuide(getTestFile("macros.guide"));
        assertNotNull(guide);
    }
}
