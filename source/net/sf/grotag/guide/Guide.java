package net.sf.grotag.guide;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.grotag.common.Tools;
import net.sf.grotag.parse.AbstractItem;
import net.sf.grotag.parse.AbstractTextItem;
import net.sf.grotag.parse.CommandItem;
import net.sf.grotag.parse.ItemReader;
import net.sf.grotag.parse.MessageItem;
import net.sf.grotag.parse.MessagePool;
import net.sf.grotag.parse.NewLineItem;
import net.sf.grotag.parse.SpaceItem;
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
    private List<CommandItem> nodeList;
    private Tools tools;
    private int uniqueNodeCounter;
    private Map<String, CommandItem> nodeMap;
    private Map<String, CommandItem> endNodeMap;

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

    /**
     * Replace all items calling a macro by the resolved sequence of items.
     */
    private void resolveMacros() throws IOException {
        int itemIndex = 0;
        while (itemIndex < items.size()) {
            AbstractItem item = items.get(itemIndex);
            System.out.println("process " + item);
            if (item instanceof CommandItem) {
                CommandItem tagItem = (CommandItem) item;
                if (tagItem.isInline()) {
                    Tag macro = tagPool.getMacro(tagItem.getCommandName());
                    if (macro != null) {
                        // messagePool.add(new MessageItem(tagItem, "resolving
                        // macro @{" + macro.getName() + "}..."));
                        // Write resolved macro to file and parse it.
                        String resolvedMacro = resolveMacro(tagItem, macro);
                        File macroSnippletFile = File.createTempFile("macro-",
                                ".guide");

                        macroSnippletFile.deleteOnExit();
                        System.out.println("writing resolved macro to: "
                                + tools.sourced(macroSnippletFile
                                        .getAbsolutePath()));
                        BufferedWriter macroSnippletWriter = new BufferedWriter(
                                new OutputStreamWriter(new FileOutputStream(
                                        macroSnippletFile), "ISO-8859-1"));
                        try {
                            macroSnippletWriter.write(resolvedMacro);
                        } finally {
                            macroSnippletWriter.close();
                        }

                        try {
                            ItemReader itemReader = new ItemReader(
                                    macroSnippletFile);
                            itemReader.read();
                            List<AbstractItem> macroItems = itemReader
                                    .getItems();

                            assert macroItems.size() > 0;
                            assert macroItems.get(macroItems.size() - 1) instanceof NewLineItem;
                            macroItems.remove(macroItems.size() - 1);
                            items.remove(itemIndex);
                            items.addAll(itemIndex, macroItems);
                            itemIndex -= 1;
                        } finally {
                            // TODO: Warn if file cannot be deleted.
                            // TODO: Keep macro snipplet file in case it
                            // contains error.
                            macroSnippletFile.delete();
                        }
                    }
                }
            }
            itemIndex += 1;
        }
    }

    private String resolveMacro(CommandItem caller, Tag macro) {
        // Replace macro options.
        String macroText = macro.getMacroTextItem().getText();
        String result = "";
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
                result += optionText;
            } else {
                result += some;
            }
            i += 1;
        }
        System.out.println("resolved macro: " + result);

        return result;
    }

    private void collectNodes() {
        nodeList = new ArrayList<CommandItem>();
        nodeMap = new TreeMap<String, CommandItem>();
        endNodeMap = new TreeMap<String, CommandItem>();

        String nodeName = null;
        int i = 0;

        while (i < items.size()) {
            AbstractItem item = items.get(i);
            if (item instanceof CommandItem) {
                CommandItem command = (CommandItem) item;
                String commandName = command.getCommandName();

                if (commandName.equals("node")) {
                    if (nodeName != null) {
                        // Add missing @endnode.
                        CommandItem endNodeItem = new CommandItem(command
                                .getFile(), command.getLine(), command
                                .getColumn(), "endnode", false,
                                new ArrayList<AbstractItem>());
                        items.add(i, endNodeItem);
                        endNodeMap.put(nodeName, endNodeItem);
                        i += 1;
                        CommandItem previousNode = nodeList
                                .get(nodeList.size() - 1);
                        MessageItem message = new MessageItem(command,
                                "added missing @endnode before @node");
                        MessageItem seeAlso = new MessageItem(previousNode,
                                "previous @node");
                        message.setSeeAlso(seeAlso);
                        messagePool.add(message);
                    }
                    nodeName = command.getOption(0);
                    if (nodeName != null) {
                        CommandItem nodeWithSameName = nodeMap.get(nodeName);
                        if (nodeWithSameName != null) {
                            // Change duplicate node name to something unique.
                            AbstractTextItem uniqueNodeNameItem = getUniqueNodeNameItem(command
                                    .getItems().get(1));
                            command.setOption(0, uniqueNodeNameItem.getText());
                            MessageItem message = new MessageItem(command,
                                    "changed duplicate node name "
                                            + tools.sourced(nodeName)
                                            + " to "
                                            + tools.sourced(uniqueNodeNameItem
                                                    .getText()));
                            MessageItem seeAlso = new MessageItem(
                                    nodeWithSameName,
                                    "existing node with same name");
                            message.setSeeAlso(seeAlso);
                            messagePool.add(message);
                        }
                    } else {
                        nodeName = getUniqueNodeName();
                        command.getItems().add(
                                new SpaceItem(command.getFile(), command
                                        .getLine(), command.getColumn(), " "));
                        command.getItems().add(
                                new TextItem(command.getFile(), command
                                        .getLine(), command.getColumn(),
                                        nodeName));
                        MessageItem message = new MessageItem(command,
                                "assigned name " + tools.sourced(nodeName)
                                        + " to unnamed node");
                        messagePool.add(message);
                    }
                    nodeList.add(command);
                    nodeMap.put(nodeName, command);
                } else if (commandName.equals("endnode")) {
                    if (nodeName == null) {
                        items.remove(i);
                        i -= 1;
                        messagePool.add(new MessageItem(command,
                                "removed dangling @endnode"));
                    } else {
                        endNodeMap.put(nodeName, command);
                        nodeName = null;
                    }
                }

            }
            i += 1;
        }
        if (nodeName != null) {
            // Add missing @endnode at end of file
            AbstractItem lastItem = items.get(items.size() - 1);
            assert lastItem instanceof NewLineItem : "lastItem="
                    + lastItem.getClass().getName();
            CommandItem endNodeItem = new CommandItem(lastItem.getFile(),
                    lastItem.getLine(), lastItem.getColumn(), "endnode", false,
                    new ArrayList<AbstractItem>());
            items.add(i, endNodeItem);
            endNodeMap.put(nodeName, endNodeItem);
            CommandItem previousNode = nodeMap.get(nodeName);
            MessageItem message = new MessageItem(lastItem,
                    "added missing @endnode at end of file");
            MessageItem seeAlso = new MessageItem(previousNode,
                    "previous @node");
            message.setSeeAlso(seeAlso);
            messagePool.add(message);
        }

        for (CommandItem node : nodeList) {
            System.out.println("node: " + node);
            System.out.println("  endnode: "
                    + endNodeMap.get(node.getOption(0)));
        }
    }

    private AbstractTextItem getUniqueNodeNameItem(AbstractItem location) {
        AbstractTextItem result;
        String nodeName = getUniqueNodeName();
        result = new TextItem(location.getFile(), location.getLine(), location
                .getLine(), nodeName);
        return result;
    }

    public static Guide createGuide(File newGuideFile) throws IOException {
        Guide result = new Guide(newGuideFile);
        result.readItems();
        result.defineMacros();
        result.resolveMacros();
        result.collectNodes();
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

    private String getUniqueNodeName() {
        String result = null;

        do {
            uniqueNodeCounter += 1;
            result = "unnamed." + uniqueNodeCounter;
        } while (nodeMap.containsKey(result));

        return result;
    }

    private void readItems() throws IOException {
        ItemReader itemReader = new ItemReader(guideFile);

        itemReader.read();
        items = itemReader.getItems();
    }
}
