package net.sf.grotag;

import java.io.File;

import net.sf.grotag.guide.DocBookWriter;
import net.sf.grotag.guide.Guide;

/**
 * Grotag - Amigaguide viewer and converter.
 * 
 * @author Thomas Aglassinger
 */
public class Grotag {
    enum Action {
        DOCBOOK, HELP, PRETTY, VALIDATE
    }

    public static void main(String[] args) throws Exception {
        Action action = Action.HELP;
        String sourceFilePath = null;
        String targetFilePath = null;

        for (String option : args) {
            if (option.startsWith("-")) {
                option = option.substring(1);
                action = Action.valueOf(option.toUpperCase());
            } else if (sourceFilePath == null) {
                sourceFilePath = option;
            } else if (targetFilePath == null) {
                targetFilePath = option;
            } else {
                throw new IllegalArgumentException("cannot process additional option: " + option);
            }
        }
        if ((sourceFilePath == null)
                && ((action == Action.DOCBOOK) || (action == Action.PRETTY) || (action == Action.VALIDATE))) {
            throw new IllegalArgumentException("source file must be specified");
        }
        if ((targetFilePath == null) && ((action == Action.DOCBOOK) || (action == Action.PRETTY))) {
            throw new IllegalArgumentException("target file must be specified");
        }

        if (action == Action.HELP) {
            System.err.println("Usage: java -jar Grotag.jar -docbook|-pretty|-validate source_file [target_file]");
        } else {
            Guide guide = Guide.createGuide(new File(sourceFilePath));
            File targetFile = new File(targetFilePath);
            if (action == Action.DOCBOOK) {
                DocBookWriter.write(guide, targetFile);
            } else if (action == Action.PRETTY) {
                guide.writePretty(targetFile);
            } else {
                assert false: "action=" + action;
            }
        }
    }
}
