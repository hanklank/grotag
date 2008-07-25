package net.sf.grotag;

import java.io.File;
import java.io.IOException;

import net.sf.grotag.guide.Guide;
import net.sf.grotag.parse.AbstractItem;

/**
 * Grotag - Amigaguide viewer and converter.
 * 
 * @author Thomas Aglassinger
 */
public class Grotag {
    public static void main(String[] args) {
        if (args.length == 1) {
            String guideToRead = args[0];
            try {
                Guide guide = Guide.createGuide(new File(guideToRead));
                for (AbstractItem item: guide.getItems()) {
                    System.out.print(item.toPrettyAmigaguide());
                }
            } catch (IOException error) {
                System.err.println("cannot read \"" + guideToRead + "\": " + error.getMessage());
            }
            
        } else {
            System.err.println("Usage: java -jar Grotag.jar <file.guide>");
        }
    }
}
