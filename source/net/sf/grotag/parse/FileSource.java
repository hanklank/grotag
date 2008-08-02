package net.sf.grotag.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * An input source to read lines from a file.
 * 
 * @author Thomas Aglassinger
 */
public class FileSource extends AbstractSource {
    private File file;

    public FileSource(File newFile) {
        assert newFile != file;
        file = newFile;
    }

    public File getFile() {
        return file;
    }

    @Override
    public BufferedReader createBufferedReader() throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO-8859-1"));
    }

    @Override
    public String getShortName() {
        return file.getName();
    }

    @Override
    public String getFullName() {
        return file.getAbsolutePath();
    }
}
