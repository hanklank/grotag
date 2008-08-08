package net.sf.grotag;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import net.sf.grotag.common.Tools;
import net.sf.grotag.guide.DocBookWriter;
import net.sf.grotag.guide.Guide;
import net.sf.grotag.guide.GuidePile;

import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;

/**
 * Grotag - Amigaguide converter and pretty printer.
 * 
 * @author Thomas Aglassinger
 */
public class Grotag {
    private GrotagJsap jsap;

    private Grotag() throws JSAPException {
        jsap = new GrotagJsap();
    }

    private void work(String[] arguments) throws IOException, ParserConfigurationException, TransformerException {
        JSAPResult options = jsap.parse(arguments);

        if (!options.success()) {
            // Throw exception for broken command line.
            Iterator<String> errorRider = options.getErrorMessageIterator();
            String errorMessage;

            if (errorRider.hasNext()) {
                errorMessage = errorRider.next();
            } else {
                errorMessage = null;
            }
            throw new IllegalArgumentException(errorMessage);
        }

        boolean isDocBook = options.getBoolean(GrotagJsap.ARG_DOCBOOK);
        boolean isPretty = options.getBoolean(GrotagJsap.ARG_PRETTY);
        boolean isValidate = options.getBoolean(GrotagJsap.ARG_VALIDATE);
        if (isDocBook || isPretty || isValidate) {
            File files[] = options.getFileArray(GrotagJsap.ARG_FILE);
            // According to JSAP API documentation, this is never is null.
            assert files != null;
            if (files.length == 0) {
                throw new IllegalArgumentException("Amigaguide input file must be specified");
            }
            if (isDocBook) {
                docBook(files);
            } else if (isPretty) {
                pretty(files);
            } else if (isValidate) {
                validate(files);
            } else {
                assert false;
            }
        } else if (options.getBoolean(GrotagJsap.ARG_HELP)) {
            jsap.printHelp(System.err);
        } else if (options.getBoolean(GrotagJsap.ARG_LICENSE)) {
            jsap.printLicense(System.out);
        } else if (options.getBoolean(GrotagJsap.ARG_VERSION)) {
            jsap.printVersion(System.out);
        } else {
            assert false : "GUI must be implemented";
        }
    }

    private void docBook(File[] files) throws IOException, ParserConfigurationException, TransformerException {
        int fileCount = files.length;
        File inputFile;
        File outputFile;
        if (fileCount == 0) {
            throw new IllegalArgumentException("Amigaguide input file must be specified");
        } else if (fileCount == 1) {
            inputFile = files[0];
            File outputFileFolder = inputFile.getParentFile();
            String inputFileName = inputFile.getName();
            String outputFileName = Tools.getInstance().getWithoutLastSuffix(inputFileName) + ".xml";
            outputFile = new File(outputFileFolder, outputFileName);
        } else if (fileCount == 2) {
            inputFile = files[0];
            outputFile = files[1];
        } else {
            throw new IllegalArgumentException("only 2 files must be specified instead of " + fileCount);
        }
        GuidePile pile = GuidePile.createGuidePile(inputFile);
        DocBookWriter.write(pile, outputFile);
    }

    private void pretty(File[] files) throws IOException {
        for (File guideFile : files) {
            Guide guide = Guide.createGuide(guideFile);
            guide.writePretty(guideFile);
        }
    }

    private void validate(File[] files) throws IOException {
        for (File guideFile : files) {
            GuidePile.createGuidePile(guideFile);
        }
    }

    public static void main(final String[] arguments) {
        Logger mainLog = Logger.getLogger(Grotag.class.getName());
        int exitCode = 1;
        try {
            Grotag grotag = new Grotag();
            grotag.work(arguments);
            exitCode = 0;
        } catch (IllegalArgumentException error) {
            mainLog.log(Level.INFO, "cannot process command line options: " + error.getMessage(), error);
        } catch (Throwable error) {
            mainLog.log(Level.SEVERE, "cannot run Grotag" + error.getMessage(), error);
        }
        System.exit(exitCode);
    }
}
