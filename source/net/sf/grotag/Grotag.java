package net.sf.grotag;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import net.sf.grotag.common.Tools;
import net.sf.grotag.guide.DocBookDomFactory;
import net.sf.grotag.guide.DomWriter;
import net.sf.grotag.guide.Guide;
import net.sf.grotag.guide.GuidePile;
import net.sf.grotag.guide.HtmlDomFactory;
import net.sf.grotag.guide.NodeInfo;
import net.sf.grotag.view.GrotagFrame;

import org.w3c.dom.Document;

import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;

/**
 * Grotag - Amigaguide converter and pretty printer.
 * 
 * @author Thomas Aglassinger
 */
public class Grotag {
    private GrotagFrame viewer;

    private final class ViewRunnable implements Runnable {
        private File file;

        private ViewRunnable(File newFile) {
            assert newFile != null;
            file = newFile;
        }

        public void run() {
            try {
                viewer.read(file);
            } catch (Exception error) {
                String filePath = Tools.getInstance().sourced(file);
                Logger viewLog = Logger.getLogger(ViewRunnable.class.getName());
                viewLog.log(Level.WARNING, "cannot view file: " + filePath, error);
            }
        }
    }

    private GrotagJsap jsap;

    private Grotag() throws JSAPException {
        jsap = new GrotagJsap();
    }

    private void work(String[] arguments) throws IOException, ParserConfigurationException, TransformerException {
        JSAPResult options = jsap.parse(arguments);

        if (!options.success()) {
            // Throw exception for broken command line.
            Iterator<String> errorRider = (Iterator<String>) options.getErrorMessageIterator();
            String errorMessage;

            if (errorRider.hasNext()) {
                errorMessage = errorRider.next();
            } else {
                errorMessage = null;
            }
            throw new IllegalArgumentException(errorMessage);
        }

        boolean isDocBook = options.getBoolean(GrotagJsap.ARG_DOCBOOK);
        boolean isHtml = options.getBoolean(GrotagJsap.ARG_HTML);
        boolean isPretty = options.getBoolean(GrotagJsap.ARG_PRETTY);
        boolean isValidate = options.getBoolean(GrotagJsap.ARG_VALIDATE);
        if (isDocBook || isHtml || isPretty || isValidate) {
            File files[] = options.getFileArray(GrotagJsap.ARG_FILE);
            // According to JSAP API documentation, this is never is null.
            assert files != null;
            if (files.length == 0) {
                throw new IllegalArgumentException("Amigaguide input file must be specified");
            }
            if (isDocBook) {
                docBook(files);
            } else if (isHtml) {
                html(files);
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
            File files[] = options.getFileArray(GrotagJsap.ARG_FILE);
            // According to JSAP API documentation, this is never is null.
            assert files != null;
            if (files.length == 0) {
                throw new IllegalArgumentException("Amigaguide input file must be specified");
            } else if (files.length > 1) {
                throw new IllegalArgumentException("only one Amigaguide input file must be specified for viewing");
            }
            viewer = new GrotagFrame();
            viewer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            viewer.pack();
            viewer.setVisible(true);
            SwingUtilities.invokeLater(new ViewRunnable(files[0]));
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
            throw new IllegalArgumentException("with --" + GrotagJsap.ARG_DOCBOOK
                    + " only 2 files must be specified instead of " + fileCount);
        }
        GuidePile pile = GuidePile.createGuidePile(inputFile);
        DocBookDomFactory domFactory = new DocBookDomFactory(pile);
        Document dom = domFactory.createBook();
        DomWriter domWriter = new DomWriter(DomWriter.Dtd.DOCBOOK);

        domWriter.write(dom, outputFile);
    }

    private void html(File[] files) throws IOException, ParserConfigurationException, TransformerException {
        int fileCount = files.length;
        File inputFile;
        File outputFolder;
        if (fileCount == 0) {
            throw new IllegalArgumentException("Amigaguide input file must be specified");
        } else if (fileCount == 1) {
            inputFile = files[0];
            outputFolder = new File(System.getProperty("user.dir"));
        } else if (fileCount == 2) {
            inputFile = files[0];
            outputFolder = files[1];
        } else {
            throw new IllegalArgumentException("with --" + GrotagJsap.ARG_HTML
                    + " only 2 files must be specified instead of " + fileCount);
        }
        createHtml(inputFile, outputFolder);
    }

    private void createHtml(File inputFile, File outputFolder) throws IOException, ParserConfigurationException,
            TransformerConfigurationException, TransformerException {
        GuidePile pile = GuidePile.createGuidePile(inputFile);
        for (Guide guide : pile.getGuides()) {
            HtmlDomFactory factory = new HtmlDomFactory(pile, outputFolder);
            factory.copyStyleFile();
            for (NodeInfo nodeInfo : guide.getNodeInfos()) {
                Document htmlDocument = factory.createNodeDocument(guide, nodeInfo);
                File targetFile = factory.getTargetFileFor(guide, nodeInfo);
                DomWriter htmlWriter = new DomWriter(DomWriter.Dtd.HTML);
                htmlWriter.write(htmlDocument, targetFile);
            }
        }
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

    public static void main(final String[] arguments) throws Exception {
        Logger mainLog = Logger.getLogger(Grotag.class.getName());
        int exitCode = 1;
        try {
            Grotag grotag = new Grotag();
            grotag.work(arguments);
            exitCode = 0;
        } catch (IllegalArgumentException error) {
            mainLog.log(Level.FINE, "cannot process command line options: " + error.getMessage(), error);
            System.err.println("cannot process command line options: " + error.getMessage());
        } catch (Exception error) {
            mainLog.log(Level.SEVERE, "cannot run Grotag: " + error.getMessage(), error);
        }
        if (exitCode > 0) {
            System.exit(exitCode);
        }
    }
}
