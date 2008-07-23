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

    /** Replace all items calling a macro by the resolved sequence of Items. */
    private void resolveMacros() {
        int itemIndex = 0;
        while (itemIndex < items.size()) {
            AbstractItem item = items.get(itemIndex);
            if (item instanceof CommandItem) {
                CommandItem tagItem = (CommandItem) item;
                if (tagItem.isInline()) {
                    Tag macro = tagPool.getMacro(tagItem.getCommandName());
                    if (macro != null) {
                        resolveMacro(tagItem, macro);
                        // messagePool.add(new MessageItem(tagItem, "resolving
                        // macro @{" + macro.getName() + "}..."));
                    }
                }
            }
            itemIndex += 1;
        }
    }

    private void resolveMacro(CommandItem caller, Tag macro) {
        // Replace macro options.
        String macroText = macro.getMacroTextItem().getText();
        String optionsResolved = "";
        int i = 0;
        while (i < macroText.length()) {
            char some = macroText.charAt(i);
            if ((some == '$')
                    && (i < (macroText.length() - 1) && Character
                            .isDigit(macroText.charAt(i + 1)))) {
                // TODO: Check how Amigaguide handles integer overflow for macro
                // options.
                int optionIndex = 0;
                String optionText;

                while ((i < (macroText.length() - 1) && Character
                        .isDigit(macroText.charAt(i + 1)))) {
                    i += 1;
                    optionIndex = 10 * optionIndex
                            + (macroText.charAt(i) - '0');
                }
                int accessOptionIndex = 1 + 2 * (optionIndex - 1);

                if (accessOptionIndex < caller.getItems().size()) {
                    optionText = ((AbstractTextItem) caller.getItems().get(
                            accessOptionIndex)).getText();
                    System.out.println("  substituting $" + optionIndex
                            + " by: " + tools.sourced(optionText));
                } else {
                    optionText = "";
                    System.out.println("  substituting $" + optionIndex
                            + " by empty text");
                }
                optionsResolved  += optionText;
            } else {
                optionsResolved += some;
            }
            i += 1;
        }
        System.out.println("resolved macro: " + optionsResolved);
    }

    public static Guide createGuide(File newGuideFile) throws IOException {
        Guide result = new Guide(newGuideFile);
        result.readItems();
        result.defineMacros();
        result.resolveMacros();
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
