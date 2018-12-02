package net.sf.grotag.common;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.*;

/**
 * Various tools to simplify testing.
 * 
 * @author Thomas Aglassinger
 */
public class TestTools {
    private static TestTools instance;

    public enum Folder {
        ACTUAL("actual"), EXPECTED("expected"), GUIDES("guides"), INPUT("input");

        private String value;

        private Folder(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }

    private String tmpDir = System.getProperty("java.io.tmpdir");

    public static final synchronized TestTools getInstance() {
        if (instance == null) {
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

    public void copyDirectory() throws IOException {
        Path path = Paths.get(tmpDir + "/test/.temp");
            if(!Files.exists(path)) {
                Files.createDirectories(path);

        }
             ClassLoader classLoader = getClass().getClassLoader();
        File f  = new File(classLoader.getResource("input").getFile());
        FileUtils.copyDirectoryToDirectory(f,path.toFile());
    }

    public File getTestFile(Folder baseFolder, String fileName)  {
        Path resultPath;
        boolean hasResouceToCopy = baseFolder != Folder.ACTUAL;
        String currentFolder = System.getProperty("java.io.tmpdir");
        if (hasResouceToCopy) {
            resultPath = Paths.get(currentFolder, "test", ".temp", baseFolder.getValue(), fileName);
        } else {
            resultPath = Paths.get(currentFolder, "target", "test", baseFolder.getValue(), fileName);
        }
        try {
            Files.createDirectories(resultPath.getParent());
        } catch (IOException error) {
            throw new IllegalStateException("cannot create target folder for file '" + resultPath.toAbsolutePath().toString() + "'", error);
        }
        if (hasResouceToCopy) {
            String resourcePath = "/" + baseFolder.getValue() + "/" + fileName;
            InputStream sourceStream = getClass().getResourceAsStream(resourcePath);
            if (sourceStream == null) {
                throw new IllegalStateException("cannot finde test resource '" + resourcePath + "'");
            }
            try {
                Files.copy(sourceStream, resultPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException error) {
                throw new IllegalStateException("cannot create temp file '"
                        + resultPath.toAbsolutePath().toString() + "' for resource '" + resourcePath + "'", error);
            }
        }
        return resultPath.toFile();
    }

    public File getTestFile(String fileName) {
        return getTestFile(Folder.INPUT, fileName);
    }

    public void assertFilesAreEqual(String fileName) throws IOException {
        File expectedFile = getTestFile(Folder.EXPECTED, fileName);
        File actualFile = getTestFile(Folder.ACTUAL, fileName);
        assertFilesAreEqual(expectedFile, actualFile);
    }

    @SuppressWarnings("unchecked")
    public String getTestName(Class classToTest, String methodToTest) {
        assert classToTest != null;
        assert methodToTest != null;
        String result = classToTest.getName();
        int dotIndex = result.lastIndexOf('.');
        assert dotIndex >= 0;
        result = result.substring(dotIndex + 1) + "." + methodToTest;
        return result;
    }

    public void assertFilesAreEqual(File expected, File actual) throws IOException {
        FileInputStream expectedFileInStream = new FileInputStream(expected);
        try {
            InputStreamReader expectedInStreamReader = new InputStreamReader(expectedFileInStream);
            try {
                BufferedReader expectedReader = new BufferedReader(expectedInStreamReader);
                try {
                    FileInputStream actualFileInStream = new FileInputStream(actual);
                    try {
                        InputStreamReader actualInStreamReader = new InputStreamReader(actualFileInStream);
                        try {
                            BufferedReader actualReader = new BufferedReader(actualInStreamReader);
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
                            actualInStreamReader.close();
                        }
                    } finally {
                        actualFileInStream.close();
                    }
                } finally {
                    expectedReader.close();
                }
            } finally {
                expectedInStreamReader.close();
            }
        } finally {
            expectedFileInStream.close();
        }
    }
}
