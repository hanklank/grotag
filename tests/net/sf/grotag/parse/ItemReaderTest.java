package net.sf.grotag.parse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for ItemReader.
 * 
 * @author Thomas Aglassinger
 */
public class ItemReaderTest {
    private Logger logger;

    @Before
    public void setUp() throws Exception {
        logger = Logger.getLogger(ItemReaderTest.class.getName());
    }

    private File createGuideFile(String targetName, String content)
            throws UnsupportedEncodingException, IOException {
        File target = File.createTempFile("ItemReaderTest." + targetName,
                ".guide");
        target.deleteOnExit();
        OutputStream targetStream = new FileOutputStream(target);
        Writer writer = new OutputStreamWriter(targetStream, "ISO-8859-1");
        try {
            writer.write(content);
        } finally {
            writer.close();
        }
        return target;
    }

    @Test
    public void testSpace() throws Exception {
        final String SPACE = " \t ";
        File guide = createGuideFile("testSpace", SPACE + "x");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        assertEquals(3, items.size());
        AbstractItem item = items.get(0);
        logger.info(item.toString());
        assertTrue(item instanceof SpaceItem);
        assertEquals(SPACE, ((SpaceItem) item).getSpace());
    }

    @Test
    public void testString() throws Exception {
        File guide = createGuideFile("testText", "@title \"hugo\"");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        assertEquals(1, items.size());
        AbstractItem item = items.get(0);
        logger.info(item.toString());
        assertTrue(item instanceof CommandItem);
        CommandItem titleItem = (CommandItem) item;
        List<AbstractItem> options = titleItem.getItems();
        assertEquals(2, options.size());
        assertTrue(options.get(0) instanceof SpaceItem);
        assertTrue(options.get(1) instanceof StringItem);
        assertEquals("hugo", ((StringItem) options.get(1)).getText());
    }

    @Test
    public void testText() throws Exception {
        File guide = createGuideFile("testText", "a\\\\b\\@");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        assertEquals(2, items.size());
        AbstractItem item = items.get(0);
        logger.info(item.toString());
        assertTrue(item instanceof TextItem);
        assertEquals("a\\b@", ((TextItem) item).getText());
    }

    @Test
    public void testDanglingAtSign() throws Exception {
        File guide = createGuideFile("testDanglingAtSign", "@");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        assertEquals(2, items.size());
        AbstractItem item = items.get(0);
        logger.info(item.toString());
        assertTrue(item instanceof TextItem);
        assertEquals("@", ((TextItem) item).getText());
    }

    @Test
    public void testCommand() throws Exception {
        File guide = createGuideFile("testDanglingAtSign", "@dAtAbAsE hugo");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        assertEquals(1, items.size());
        AbstractItem item = items.get(0);
        logger.info(item.toString());
        assertTrue(item instanceof CommandItem);
        CommandItem commandItem = (CommandItem) item;
        assertEquals("database", commandItem.getCommandName());
        assertEquals("dAtAbAsE", commandItem.getOriginalCommandName());
        assertFalse(commandItem.isInline());
        assertNotNull(commandItem.getItems());
        assertEquals(2, commandItem.getItems().size());
    }

    private File getTestGuide(String relativeName) {
        return new File(new File("tests", "guides"), relativeName);
    }

    @Test
    public void testLichtTools() throws Exception {
        File guide = getTestGuide("LichtTools.guide");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        for (AbstractItem item : reader.getItems()) {
            logger.info(item.toString());
        }

    }
}
