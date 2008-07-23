package net.sf.grotag.guide;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.sf.grotag.common.Tools;
import net.sf.grotag.parse.AbstractItem;
import net.sf.grotag.parse.AbstractTextItem;
import net.sf.grotag.parse.CommandItem;
import net.sf.grotag.parse.ItemReader;
import net.sf.grotag.parse.MessageItem;
import net.sf.grotag.parse.MessagePool;
import net.sf.grotag.parse.SpaceItem;
import net.sf.grotag.parse.Tag;
import net.sf.grotag.parse.TagPool;

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

    private Guide(File newGuideFile) {
        assert newGuideFile != null;

        tools = Tools.getInstance();
        messagePool = MessagePool.getInstance();

        guideFile = newGuideFile;
        tagPool = new TagPool();
    }

    private void defineMacros() {
        for (AbstractItem item : items) {
            if (item instanceof CommandItem) {
                CommandItem possibleMacroItem = (CommandItem) item;
                String commandName = possibleMacroItem.getCommandName();

                if (!possibleMacroItem.isInline()
                        && (commandName.equals("macro"))) {
                    Tag macro = createMacro(possibleMacroItem);

                    if (macro != null) {
                        String macroName = macro.getName();
                        Tag existingMacro = tagPool.getTag(macroName,
                                Tag.Scope.INLINE);

                        if (existingMacro != null) {
                            if (existingMacro.isMacro()) {
                                MessageItem currentMacroMessage = new MessageItem(
                                        possibleMacroItem,
                                        "ignored duplicate definition of macro "
                                                + tools.sourced(macroName));
                                MessageItem existingMacroMessage = new MessageItem(
                                        existingMacro.getMacroTextItem(),
                                        "previous definition of macro");

                                currentMacroMessage
                                        .setSeeAlso(existingMacroMessage);
                                messagePool.add(currentMacroMessage);
                            } else {
                                messagePool.add(new MessageItem(
                                        possibleMacroItem,
                                        "replaced standard tag "
                                                + tools.sourced(existingMacro
                                                        .getName())
                                                + " with macro"));
                            }
                        } else {
                            tagPool.addTag(macro);
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

    private Tag createMacro(CommandItem macro) {
        assert macro.getCommandName().equals("macro");
        Tag result = null;
        String macroName = null;
        int itemCount = macro.getItems().size();

        if (itemCount >= 2) {
            AbstractItem firstItem = macro.getItems().get(0);
            assert firstItem instanceof SpaceItem : "first macro item must be "
                    + SpaceItem.class + " but is " + firstItem.getClass();
            AbstractTextItem macroNameItem = (AbstractTextItem) macro
                    .getItems().get(1);
            macroName = macroNameItem.getText().toLowerCase();

            AbstractTextItem macroTextItem;
            if (itemCount >= 4) {
                AbstractItem thirdItem = macro.getItems().get(2);
                assert thirdItem instanceof SpaceItem : "third macro item must be "
                        + SpaceItem.class + " but is " + firstItem.getClass();
                macroTextItem = (AbstractTextItem) macro.getItems().get(3);
            } else {
                macroTextItem = null;
            }
            result = Tag.createMacro(macroName, macroTextItem);
        }
        return result;
    }

    private void readItems() throws IOException {
        ItemReader itemReader = new ItemReader(guideFile);

        itemReader.read();
        items = itemReader.getItems();
    }
}
