package net.sf.grotas.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import net.sf.grotas.common.Tools;

public class ItemReader {
    private static final char BEFORE_DATABASE = 'b';
    private LineTokenizer tokenizer;
    private File guideFile;
    private char state;
    private List<AbstractItem> items;
    private Tools tools;
    private int lineNumber;

    public ItemReader(File newGuideFile) {
        assert newGuideFile != null;
        tools = Tools.getInstance();
        guideFile = newGuideFile;
        items = new LinkedList<AbstractItem>();
    }

    // TODO: collect warnings in WarningPool or something.
    private void fireWarning(String message, int messageColumn) {
        assert message != null;
        assert messageColumn >= 0;
        System.err.println("(" + lineNumber + ":" + messageColumn + ") - "
                + message);
    }

    public void read() throws IOException {
        InputStream guideStream = new FileInputStream(guideFile);
        BufferedReader guideReader = new BufferedReader(new InputStreamReader(
                guideStream, "ISO-8859-1"));
        int columnNumber;
        String line;

        lineNumber = 0;
        items = new LinkedList<AbstractItem>();
        state = BEFORE_DATABASE;
        try {
            do {
                line = guideReader.readLine();
                if (line != null) {
                    tokenizer = new LineTokenizer(guideFile, lineNumber, line);
                    while (tokenizer.hasNext()) {
                        columnNumber = tokenizer.getColumn();
                        tokenizer.advance();
                        if (tokenizer.getType() == LineTokenizer.TYPE_SPACE) {
                            items.add(new SpaceItem(guideFile, lineNumber,
                                    columnNumber, tokenizer.getToken()));
                        } else if (tokenizer.getType() == LineTokenizer.TYPE_COMMAND) {
                            readCommand();
                        } else {
                            items.add(new TextItem(guideFile, lineNumber,
                                    columnNumber, tokenizer.getToken()));
                        }
                    }
                }
                lineNumber += 1;
            } while (line != null);
        } finally {
            guideReader.close();
        }
    }

    private void readCommand() {
        int commandColumnNumber = tokenizer.getColumn();
        List<AbstractItem> commandItems = new LinkedList<AbstractItem>();
        String commandName;

        assert tokenizer.getType() == LineTokenizer.TYPE_COMMAND;
        assert tokenizer.hasNext();
        tokenizer.advance();
        boolean isInlineCommand = tokenizer.getType() == LineTokenizer.TYPE_OPEN_BRACE;

        // Skip possible "{".
        if (isInlineCommand) {
            assert tokenizer.hasNext();
            tokenizer.advance();
        }

        assert tokenizer.getType() != LineTokenizer.TYPE_SPACE : "\"@{\" with white space must have been handled by "
                + LineTokenizer.class;
        commandName = tokenizer.getToken();
        while (tokenizer.hasNext()
                && !(isInlineCommand && tokenizer.getType() == LineTokenizer.TYPE_CLOSE_BRACE)) {
            int columnNumber = tokenizer.getColumn();
            tokenizer.advance();
            if (tokenizer.getType() == LineTokenizer.TYPE_SPACE) {
                commandItems.add(new SpaceItem(guideFile, lineNumber,
                        columnNumber, tokenizer.getToken()));
            } else if (tokenizer.getType() == LineTokenizer.TYPE_STRING) {
                commandItems.add(new TextItem(guideFile, lineNumber,
                        columnNumber, tokenizer.getToken()));
            } else {
                commandItems.add(new TextItem(guideFile, lineNumber,
                        columnNumber, tokenizer.getToken()));
            }
        }
        items.add(new CommandItem(guideFile, lineNumber, commandColumnNumber,
                commandName, isInlineCommand, commandItems));
    }

    /** Items in the Amigaguide. */
    public List<AbstractItem> getItems() {
        return items;
    }
}
