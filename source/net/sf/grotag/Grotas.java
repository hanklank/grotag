package net.sf.grotag;

import java.io.File;

import net.sf.grotag.parse.ItemReader;

/**
 * Grotas - Amigaguide viewer and converter.
 * 
 * @author Thomas Aglassinger
 */
public class Grotas {
    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            ItemReader reader = new ItemReader(new File(args[0]));
            reader.read();
        } else {
            System.err.println("Usage: java -jar Grotas.jar <file.guide>");
        }
    }
}
