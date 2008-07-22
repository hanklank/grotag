package net.sf.grotag.guide;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.grotag.common.Tools;
import net.sf.grotag.parse.AbstractItem;
import net.sf.grotag.parse.CommandItem;
import net.sf.grotag.parse.ItemReader;
import net.sf.grotag.parse.MessageItem;
import net.sf.grotag.parse.MessagePool;
import net.sf.grotag.parse.SpaceItem;
import net.sf.grotag.parse.StringItem;
import net.sf.grotag.parse.Tag;
import net.sf.grotag.parse.TagPool;
import net.sf.grotag.parse.TextItem;

/**
 * An Amigaguide document.
 * 
 * @author Thomas Aglassinger
 */
public class Guide {
    private File guideFile;
    private List<AbstractItem> items;
    private TagPool tagPool;
    private MessagePool messagePool;
    private Tools tools;
    private Map<String, Macro> macroMap;
    private Map<String, CommandItem> macroItemMap;

    private Guide(File newGuideFile) {
        assert newGuideFile != null;

        tools = Tools.getInstance();
        messagePool = MessagePool.getInstance();

        guideFile = newGuideFile;
        tagPool = new TagPool();
        macroMap = new TreeMap<String, Macro>();
        macroItemMap = new TreeMap<String, CommandItem>();
    }

    private void defineMacros() {
        for (AbstractItem item : items) {
            if (item instanceof CommandItem) {
                CommandItem possibleMacroItem = (CommandItem) item;
                String commandName = possibleMacroItem.getCommandName();

                if (!possibleMacroItem.isInline()
                        && (commandName.equals("macro"))) {
                    Macro macro = createMacro(possibleMacroItem);

                    if (macro != null) {
                        String macroName = macro.getName();
                        CommandItem existingMacro = macroItemMap.get(macroName);
                        if (existingMacro == null) {
                            Tag existingTag = tagPool.getTag(macroName,
                                    Tag.Scope.INLINE);
                            if (existingTag != null) {
                                messagePool.add(new MessageItem(
                                        possibleMacroItem,
                                        "replaced standard tag "
                                                + tools.sourced(existingTag
                                                        .getName()
                                                        + " with macro")));
                            }
                            macroMap.put(macroName, macro);
                            macroItemMap.put(macroName, possibleMacroItem);
                        } else {
                            String originalMacroName = macroMap.get(macroName).getOriginalName();
                            MessageItem currentMacroMessage = new MessageItem(
                                    possibleMacroItem,
                                    "ignored duplicate definition of macro "
                                            + tools.sourced(originalMacroName));
                            MessageItem existingMacroMessage = new MessageItem(
                                    existingMacro,
                                    "previous definition of macro");

                            currentMacroMessage
                                    .setSeeAlso(existingMacroMessage);
                            messagePool.add(currentMacroMessage);
                        }
                    }
                }
            }
        }
    }

    public static Guide createGuide(File newGuideFile) throws IOException {
        Guide result = new Guide(newGuideFile);
        result.readItems();
        result.defineMacros();
        return result;
    }

    private Macro createMacro(CommandItem macro) {
        assert macro.getCommandName().equals("macro");
        Macro result = null;
        String macroName = null;
        String macroText = null;
        int itemCount = macro.getItems().size();

        if (itemCount >= 2) {
            AbstractItem firstItem = macro.getItems().get(0);
            assert firstItem instanceof SpaceItem : "first macro item must be "
                    + SpaceItem.class + " but is " + firstItem.getClass();
            AbstractItem macroNameItem = macro.getItems().get(1);
            macroName = getText(macroNameItem);

            if (itemCount >= 4) {
                AbstractItem thirdItem = macro.getItems().get(2);
                assert thirdItem instanceof SpaceItem : "third macro item must be "
                        + SpaceItem.class + " but is " + firstItem.getClass();
                AbstractItem macroTextItem = macro.getItems().get(3);
                macroText = getText(macroTextItem);
            }
            result = new Macro(macroName, macroText);
        }
        return result;
    }

    private String getText(AbstractItem item) {
        String result;
        if (item instanceof TextItem) {
            result = ((TextItem) item).getText();
        } else if (item instanceof StringItem) {
            result = ((StringItem) item).getString();
        } else {
            assert false : item.getClass().getName();
            result = null;
        }
        return result;
    }

    private void readItems() throws IOException {
        ItemReader itemReader = new ItemReader(guideFile);

        itemReader.read();
        items = itemReader.getItems();
    }
}
