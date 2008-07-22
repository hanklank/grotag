package net.sf.grotag.parse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.text.ParseException;

import org.junit.Test;

public class TagOptionTest {
    private TagOption option;

    @Test
    public void testSimpleTagOptions() throws ParseException {
        option = new TagOption("width number", 5);
        assertEquals(TagOption.Type.NUMBER, option.getType());
        assertNull(option.getDefaultValue());
    }

    @Test
    public void testTagOptionWithDefault() throws ParseException {
        option = new TagOption("alink node number=0", 10);
        assertEquals(TagOption.Type.NUMBER, option.getType());
        assertEquals("0", option.getDefaultValue());
    }

    @Test(expected = ParseException.class)
    public void testBrokenType() throws ParseException {
        option = new TagOption("alink hugo", 5);
    }

    @Test(expected = ParseException.class)
    public void testMissingDefaultValue() throws ParseException {
        option = new TagOption("alink node number=", 10);
    }

    @Test(expected = ParseException.class)
    public void testTooLong() throws ParseException {
        option = new TagOption("alink node number=0=17", 10);
    }
}
