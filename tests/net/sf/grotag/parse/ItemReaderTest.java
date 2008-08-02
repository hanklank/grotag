package net.sf.grotag.parse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import net.sf.grotag.common.TestTools;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for ItemReader.
 * 
 * @author Thomas Aglassinger
 */
public class ItemReaderTest {
    private Logger logger;
    private TestTools testTools;

    @Before
    public void setUp() throws Exception {
        testTools = TestTools.getInstance();
        logger = Logger.getLogger(ItemReaderTest.class.getName());
    }

    private StringSource createStringSource(String shortName, String text) {
        assert shortName != null;
        assert text != null;
        return new StringSource(ItemReaderTest.class.getName() + File.separator + shortName, text);
    }

    @Test
    public void testSpace() throws Exception {
        final String SPACE = " \t ";
        AbstractSource guide = createStringSource("testSpace", SPACE + "x");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        assertEquals(3, items.size());
        AbstractItem item = items.get(0);
        logger.info(item.toString());
        assertTrue(item instanceof SpaceItem);
        assertEquals(SPACE, ((SpaceItem) item).getSpace());
    }

    // @Test
    public void testString() throws Exception {
        StringSource guide = createStringSource("testText", "@title \"hugo\"");
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

    // @Test
    public void testText() throws Exception {
        StringSource guide = createStringSource("testText", "a\\\\b\\@");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        assertEquals(2, items.size());
        AbstractItem item = items.get(0);
        logger.info(item.toString());
        assertTrue(item instanceof TextItem);
        assertEquals("a\\b@", ((TextItem) item).getText());
    }

    // @Test
    public void testDanglingAtSign() throws Exception {
        StringSource guide = createStringSource("testDanglingAtSign", "@");
        ItemReader reader = new ItemReader(guide);
        reader.read();
        List<AbstractItem> items = reader.getItems();
        assertEquals(2, items.size());
        AbstractItem item = items.get(0);
        logger.info(item.toString());
        assertTrue(item instanceof TextItem);
        assertEquals("@", ((TextItem) item).getText());
    }

    // @Test
    public void testCommand() throws Exception {
        AbstractSource guide = createStringSource("testDanglingAtSign", "@dAtAbAsE hugo");
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

    @Test
    public void testLichtTools() throws Exception {
        AbstractSource guide = new FileSource(testTools.getTestInputFile("basics.guide"));
        ItemReader reader = new ItemReader(guide);
        reader.read();
        for (AbstractItem item : reader.getItems()) {
            logger.info(item.toString());
        }
    }
}
