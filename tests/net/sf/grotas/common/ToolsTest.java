package net.sf.grotas.common;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ToolsTest {
    private Tools tools;

    @Before
    public void setUp() throws Exception {
        tools = Tools.getInstance();
    }

    @Test
    public void testWithoutTrailingWhiteSpace() {
        assertEquals("hugo", tools.withoutTrailingWhiteSpace("hugo"));
        assertEquals("hugo", tools.withoutTrailingWhiteSpace("hugo "));
        assertEquals("hugo", tools.withoutTrailingWhiteSpace("hugo \t \t"));
        assertEquals(" hugo", tools.withoutTrailingWhiteSpace(" hugo"));
        assertEquals("", tools.withoutTrailingWhiteSpace(" "));
        assertEquals("", tools.withoutTrailingWhiteSpace(""));
    }
}
