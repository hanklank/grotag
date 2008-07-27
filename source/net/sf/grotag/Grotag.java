package net.sf.grotag;

import java.io.File;
import java.io.IOException;

import net.sf.grotag.guide.Guide;

/**
 * Grotag - Amigaguide viewer and converter.
 * 
 * @author Thomas Aglassinger
 */
public class Grotag {
    enum Action {
        HELP, PRETTY, VALIDATE
    }

    public static void main(String[] args) throws IOException {
        Action action = Action.HELP;
        String sourceFilePath = null;
        String targetFilePath = null;
        
        for (String option: args) {
            if (option.startsWith("-")) {
                option = option.substring(1);
                action = Action.valueOf(option);
            } else if (sourceFilePath == null) {
                sourceFilePath = option;
            } else if (targetFilePath == null) {
                targetFilePath = option;
            } else {
                throw new IllegalArgumentException("cannot process additional option: " + option);
            }
        }
        if ((sourceFilePath == null) &&  ((action == Action.PRETTY) || (action == Action.VALIDATE))) {
            throw new IllegalArgumentException("source file must be specified");
        }
        if ((targetFilePath == null) &&  (action == Action.PRETTY)) {
            throw new IllegalArgumentException("target file must be specified");
        }
        
        if (action == Action.HELP) {
            System.err.println("Usage: java -jar Grotag.jar -pretty|-validate source_file [target_file]");
        } else {
            Guide guide = Guide.createGuide(new File(sourceFilePath));
            if (action == Action.PRETTY) {
                guide.writePretty(new File(targetFilePath));
            }
        }
    }
}
