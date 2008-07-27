package net.sf.grotag.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Various tools to simplify testing.
 * 
 * @author Thomas Aglassinger
 */
public class TestTools {
    private static TestTools instance;
    
    public enum Folder {
        ACTUAL, EXPECTED, GUIDES, INPUT
    }

    public static final synchronized TestTools getInstance() {
        if (instance != null) {
            instance = new TestTools();
        }
        return instance;
    }
    
    private TestTools() {
        // Instantiate tools to setup logging.
        Tools.getInstance();
    }

    public File getTestActualFile(String fileName) {
        return getTestFile(Folder.ACTUAL, fileName);
    }

    public File getTestExpectedFile(String fileName) {
        return getTestFile(Folder.EXPECTED, fileName);
    }

    public File getTestInputFile(String fileName) {
        return getTestFile(Folder.INPUT, fileName);
    }

    public File getTestGuideFile(String fileName) {
        return getTestFile(Folder.GUIDES, fileName);
    }

    public File getTestFile(Folder baseFolder, String fileName) {
        File folder = new File("tests", baseFolder.toString().toLowerCase());
        File result = new File(folder, fileName);

        // Make sure target folder for "actual" files exists, as it is not part
        // of the repository.
        if ((baseFolder == Folder.ACTUAL) && !folder.exists()) {
            folder.mkdirs();
        }
        return result;
    }

    public File getTestFile(String fileName) {
        return getTestFile(Folder.INPUT, fileName);
    }

    public void assertFilesAreEqual(String fileName) throws IOException {
        File expectedFile = getTestFile(Folder.EXPECTED, fileName);
        File actualFile = getTestFile(Folder.ACTUAL, fileName);
        assertFilesAreEqual(expectedFile, actualFile);
    }

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
}
