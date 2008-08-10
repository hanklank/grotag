package net.sf.grotag.guide;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.grotag.common.Tools;

import org.w3c.dom.Document;

public class DomWriter {
    private static final String OUTPUT_ENCODING = "UTF-8";

    private Logger log;
    private Tools tools;

    public DomWriter() {
        log = Logger.getLogger(DomWriter.class.getName());
        tools = Tools.getInstance();
    }

    public void write(Document dom, File targetFile) throws IOException, TransformerException {
        assert dom != null;
        assert targetFile != null;

        log.log(Level.INFO, "write dom to " + tools.sourced(targetFile));
        Writer targetWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile),
                OUTPUT_ENCODING));
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//OASIS//DTD DocBook XML V4.5//EN");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                    "http://www.oasis-open.org/docbook/xml/4.5/docbookx.dtd");
            transformer.setOutputProperty(OutputKeys.ENCODING, OUTPUT_ENCODING);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(dom), new StreamResult(targetWriter));
        } finally {
            targetWriter.close();
        }
    }
}
