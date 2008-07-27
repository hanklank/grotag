package net.sf.grotag.guide;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

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
import net.sf.grotag.parse.TagOption;
import net.sf.grotag.parse.TagPool;
import net.sf.grotag.parse.TextItem;

/**
 * An Amigaguide document.
 * 
 * @author Thomas Aglassinger
 */
public class Guide {
    private Logger logger;
    private File guideFile;
    private List<AbstractItem> items;
    private TagPool tagPool;
    private MessagePool messagePool;
    private List<CommandItem> nodeList;
    private Tools tools;
    private int uniqueNodeCounter;
    private Map<String, CommandItem> nodeMap;
    private Map<String, CommandItem> endNodeMap;
    private Map<String, CommandItem> uniqueGlobalCommandsOccurred;
    private Map<String, CommandItem> uniqueNodeCommandsOccurred;
    private boolean hasMacros;

    private Guide(File newGuideFile) {
        assert newGuideFile != null;

        tools = Tools.getInstance();
        messagePool = MessagePool.getInstance();
        logger = Logger.getLogger(Guide.class.getName());

        guideFile = newGuideFile;
        tagPool = new TagPool();
    }

    private void defineMacros() {
        for (AbstractItem item : items) {
            if (isLineCommand(item)) {
                CommandItem possibleMacroItem = (CommandItem) item;
                String commandName = possibleMacroItem.getCommandName();

                if (commandName.equals("macro")) {
                    Tag macro = createMacro(possibleMacroItem);

                    if (macro != null) {
                        String macroName = macro.getName();
                        Tag existingMacro = tagPool.getTag(macroName, Tag.Scope.INLINE);

                        if (existingMacro != null) {
                            if (existingMacro.isMacro()) {
                                MessageItem currentMacroMessage = new MessageItem(possibleMacroItem,
                                        "ignored duplicate definition of macro " + tools.sourced(macroName));
                                MessageItem existingMacroMessage = new MessageItem(existingMacro.getMacroTextItem(),
                                        "previous definition of macro");

                                currentMacroMessage.setSeeAlso(existingMacroMessage);
                                messagePool.add(currentMacroMessage);
                            } else {
                                messagePool.add(new MessageItem(possibleMacroItem, "replaced standard tag "
                                        + tools.sourced(existingMacro.getName()) + " with macro"));
                            }
                        } else {
                            hasMacros = true;
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
            logger.fine("process " + item);
            if (isInlineCommand(item)) {
                CommandItem tagItem = (CommandItem) item;
                Tag macro = tagPool.getMacro(tagItem.getCommandName());
                if (macro != null) {
                    // messagePool.add(new MessageItem(tagItem, "resolving
                    // macro @{" + macro.getName() + "}..."));
                    // Write resolved macro to file and parse it.
                    String resolvedMacro = resolveMacro(tagItem, macro);
                    File macroSnippletFile = File.createTempFile("macro-", ".guide");

                    macroSnippletFile.deleteOnExit();
                    logger.fine("writing resolved macro to: " + tools.sourced(macroSnippletFile.getAbsolutePath()));
                    BufferedWriter macroSnippletWriter = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(macroSnippletFile), "ISO-8859-1"));
                    try {
                        macroSnippletWriter.write(resolvedMacro);
                    } finally {
                        macroSnippletWriter.close();
                    }

                    try {
                        ItemReader itemReader = new ItemReader(macroSnippletFile);
                        itemReader.read();
                        List<AbstractItem> macroItems = itemReader.getItems();

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
            if ((some == '$') && (i < (macroText.length() - 1) && Character.isDigit(macroText.charAt(i + 1)))) {
                // TODO: Check how Amigaguide handles integer overflow for macro
                // options.
                int optionIndex = 0;
                String optionText;

                while ((i < (macroText.length() - 1) && Character.isDigit(macroText.charAt(i + 1)))) {
                    i += 1;
                    optionIndex = 10 * optionIndex + (macroText.charAt(i) - '0');
                }
                int accessOptionIndex = 1 + 2 * (optionIndex - 1);

                if (accessOptionIndex < caller.getItems().size()) {
                    optionText = ((AbstractTextItem) caller.getItems().get(accessOptionIndex)).getText();
                    logger.fine("  substituting $" + optionIndex + " by: " + tools.sourced(optionText));
                } else {
                    optionText = "";
                    logger.fine("  substituting $" + optionIndex + " by empty text");
                }
                result += optionText;
            } else {
                result += some;
            }
            i += 1;
        }
        logger.fine("resolved macro: " + result);

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
            if (isLineCommand(item)) {
                CommandItem command = (CommandItem) item;
                String commandName = command.getCommandName();

                if (commandName.equals("node")) {
                    if (nodeName != null) {
                        // Add missing @endnode.
                        CommandItem endNodeItem = new CommandItem(command.getFile(), command.getLine(), command
                                .getColumn(), "endnode", false, new ArrayList<AbstractItem>());
                        items.add(i, endNodeItem);
                        endNodeMap.put(nodeName, endNodeItem);
                        i += 1;
                        CommandItem previousNode = nodeList.get(nodeList.size() - 1);
                        MessageItem message = new MessageItem(command, "added missing @endnode before @node");
                        MessageItem seeAlso = new MessageItem(previousNode, "previous @node");
                        message.setSeeAlso(seeAlso);
                        messagePool.add(message);
                    }
                    nodeName = command.getOption(0);
                    if (nodeName != null) {
                        CommandItem nodeWithSameName = nodeMap.get(nodeName);
                        if (nodeWithSameName != null) {
                            // Change duplicate node name to something unique.
                            AbstractTextItem uniqueNodeNameItem = getUniqueNodeNameItem(command.getItems().get(1));
                            command.setOption(0, uniqueNodeNameItem.getText());
                            MessageItem message = new MessageItem(command, "changed duplicate node name "
                                    + tools.sourced(nodeName) + " to " + tools.sourced(uniqueNodeNameItem.getText()));
                            MessageItem seeAlso = new MessageItem(nodeWithSameName, "existing node with same name");
                            message.setSeeAlso(seeAlso);
                            messagePool.add(message);
                        }
                    } else {
                        nodeName = getUniqueNodeName();
                        command.getItems().add(
                                new SpaceItem(command.getFile(), command.getLine(), command.getColumn(), " "));
                        command.getItems().add(
                                new TextItem(command.getFile(), command.getLine(), command.getColumn(), nodeName));
                        MessageItem message = new MessageItem(command, "assigned name " + tools.sourced(nodeName)
                                + " to unnamed node");
                        messagePool.add(message);
                    }
                    nodeList.add(command);
                    nodeMap.put(nodeName, command);
                } else if (commandName.equals("endnode")) {
                    if (nodeName == null) {
                        items.remove(i);
                        i -= 1;
                        messagePool.add(new MessageItem(command, "removed dangling @endnode"));
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
            assert lastItem instanceof NewLineItem : "lastItem=" + lastItem.getClass().getName();
            CommandItem endNodeItem = new CommandItem(lastItem.getFile(), lastItem.getLine(), lastItem.getColumn(),
                    "endnode", false, new ArrayList<AbstractItem>());
            items.add(i, endNodeItem);
            endNodeMap.put(nodeName, endNodeItem);
            CommandItem previousNode = nodeMap.get(nodeName);
            MessageItem message = new MessageItem(lastItem, "added missing @endnode at end of file");
            MessageItem seeAlso = new MessageItem(previousNode, "previous @node");
            message.setSeeAlso(seeAlso);
            messagePool.add(message);
        }

        for (CommandItem node : nodeList) {
            logger.fine("node: " + node);
            logger.fine("  endnode: " + endNodeMap.get(node.getOption(0)));
        }
    }

    private void validateCommands() {
        uniqueGlobalCommandsOccurred = new TreeMap<String, CommandItem>();
        uniqueNodeCommandsOccurred = new TreeMap<String, CommandItem>();
        boolean insideNode = false;
        int itemIndex = 0;

        while (itemIndex < items.size()) {
            AbstractItem item = items.get(itemIndex);
            if (item instanceof CommandItem) {
                CommandItem command = (CommandItem) item;
                if (command.getCommandName().equals("node")) {
                    assert !insideNode;
                    insideNode = true;
                } else if (command.getCommandName().equals("endnode")) {
                    assert insideNode;
                    insideNode = false;
                    uniqueNodeCommandsOccurred.clear();
                }

                int oldItemCount = items.size();
                Tag.Scope scope = getScopeFor(command, insideNode);
                if (scope == Tag.Scope.LINK) {
                    validateLink(itemIndex, command);
                } else {
                    boolean removeCommand = false;
                    Tag tag = tagPool.getTag(command.getCommandName(), scope);
                    if (tag != null) {
                        TagOption[] tagOptions = tag.getOptions();
                        boolean lastOptionIsAnyOrSome = false;
                        int optionIndex = 0;

                        removeCommand = !isValidPossiblyUniqueCommand(command, tag);

                        if (!removeCommand) {
                            validateUnusedAndObsoleteCommand(command, tag);
                        }
                        while (!removeCommand && (tagOptions != null) && (optionIndex < tagOptions.length)) {
                            TagOption tagOption = tagOptions[optionIndex];
                            String optionValue = command.getOption(optionIndex);
                            String validationError = tagOption.validationError(optionValue);
                            if (validationError != null) {
                                AbstractItem baseItem = command.getOptionItem(optionIndex);

                                if (baseItem == null) {
                                    baseItem = command;
                                }
                                MessageItem message = new MessageItem(baseItem, "removed "
                                        + command.toShortAmigaguide() + " because option #" + (optionIndex + 1)
                                        + " is broken: " + validationError);
                                messagePool.add(message);
                                removeCommand = true;
                            } else {
                                assert !lastOptionIsAnyOrSome : "option of type \"any\" or \"some\" must be the last: "
                                        + tag;
                                lastOptionIsAnyOrSome = (tagOption.getType() == TagOption.Type.ANY)
                                        || (tagOption.getType() == TagOption.Type.SOME);
                            }
                            optionIndex += 1;
                        }

                        if (!removeCommand && !lastOptionIsAnyOrSome) {
                            AbstractTextItem optionItem = command.getOptionItem(optionIndex);
                            if (optionItem != null) {
                                MessageItem message = new MessageItem(optionItem, "ignored unexpected option #"
                                        + (optionIndex + 1) + " (and possible further options) for "
                                        + command.toShortAmigaguide() + ": " + tools.sourced(optionItem.getText()));
                                messagePool.add(message);
                            }
                        }
                    } else {
                        MessageItem message = new MessageItem(command, "removed unknown command "
                                + command.toShortAmigaguide());
                        messagePool.add(message);
                        removeCommand = true;
                    }

                    // TODO: Move global commands inside node below @database.
                }
                // Adjust itemIndex to whatever number validateXXX() has
                // added or removed.
                itemIndex += items.size() - oldItemCount;
            }
            itemIndex += 1;
        }
        assert insideNode == false;

        // No more need for those, but GC wouldn't know.
        uniqueGlobalCommandsOccurred = null;
        uniqueNodeCommandsOccurred = null;
    }

    /**
     * Is <code>command</code> a non-unique command or a unique command that
     * has not occurred so far within the scope defined by <code>tag</code>?
     */
    private boolean isValidPossiblyUniqueCommand(CommandItem command, Tag tag) {
        assert command != null;
        assert tag != null;
        assert command.getCommandName().equals(tag.getName());

        boolean result = true;
        if (tag.isUnique()) {
            Tag.Scope scope = tag.getScope();
            Map<String, CommandItem> uniqueCommandsOccurred;

            if (scope == Tag.Scope.GLOBAL) {
                uniqueCommandsOccurred = uniqueGlobalCommandsOccurred;
            } else {
                assert scope == Tag.Scope.NODE;
                uniqueCommandsOccurred = uniqueNodeCommandsOccurred;
            }

            CommandItem existingUniqueCommand = uniqueCommandsOccurred.get(command.getCommandName());
            if (existingUniqueCommand != null) {
                String messageText = "removed duplicate " + command.toShortAmigaguide()
                        + " because it must be unique within ";
                if (scope == Tag.Scope.GLOBAL) {
                    messageText += "document";
                } else {
                    assert scope == Tag.Scope.NODE;
                    messageText += "node";
                }

                MessageItem message = new MessageItem(command, messageText);
                MessageItem seeAlso = new MessageItem(existingUniqueCommand, "previous occurrence");
                message.setSeeAlso(seeAlso);
                messagePool.add(message);
                result = false;
            } else {
                uniqueCommandsOccurred.put(command.getCommandName(), command);
            }
        }
        return result;
    }

    private void validateUnusedAndObsoleteCommand(CommandItem command, Tag tag) {
        assert command != null;
        assert tag != null;
        assert command.getCommandName().equals(tag.getName());

        String reasonToIgnore = null;

        if (tag.isObsolete()) {
            reasonToIgnore = "obsolete";
        } else if (tag.isUnused()) {
            reasonToIgnore = "unused";
        }

        if (reasonToIgnore != null) {
            MessageItem message = new MessageItem(command, "ignored " + reasonToIgnore + " command "
                    + command.toShortAmigaguide());
            messagePool.add(message);
        }
    }

    private void validateLink(int itemIndex, CommandItem command) {
        String linkType = command.getOption(0);
        String reasonToReplaceLinkByText = null;
        if (linkType != null) {
            Tag linkTag = tagPool.getTag(linkType, Tag.Scope.LINK);
            if (linkTag == null) {
                reasonToReplaceLinkByText = "unknown";
            } else {
                // TODO: Schedule link for link target check.
                // TODO: Validate link options.
            }
        } else {
            reasonToReplaceLinkByText = "empty";
        }
        if (reasonToReplaceLinkByText != null) {
            MessageItem message = new MessageItem(command, "replaced " + reasonToReplaceLinkByText
                    + " link by its text: " + command.toPrettyAmigaguide());
            messagePool.add(message);
            String line = command.getOriginalCommandName();
            // FIXME: Deconstruct line into AbstractItems (using ItemReader)
            // before inserting it.
            TextItem textItem = new TextItem(command.getFile(), command.getLine(), command.getColumn() + 2, line);
            items.set(itemIndex, textItem);
        }
    }

    private Tag.Scope getScopeFor(CommandItem command, boolean insideNode) {
        assert command != null;
        Tag.Scope result;
        if (command.isInline()) {
            if (command.getCommandName().startsWith("\"")) {
                result = Tag.Scope.LINK;
            } else {
                result = Tag.Scope.INLINE;
            }
        } else if (insideNode) {
            result = Tag.Scope.NODE;
        } else {
            result = Tag.Scope.GLOBAL;
        }
        return result;
    }

    /**
     * Is <code>item</code> a line command, for example <code>@node</code>?
     */
    private boolean isLineCommand(AbstractItem item) {
        return (item instanceof CommandItem) && !((CommandItem) item).isInline();
    }

    /**
     * Is <code>item</code> an inline command, for example <code>@{b}</code>?
     */
    private boolean isInlineCommand(AbstractItem item) {
        return (item instanceof CommandItem) && ((CommandItem) item).isInline();
    }

    private AbstractTextItem getUniqueNodeNameItem(AbstractItem location) {
        AbstractTextItem result;
        String nodeName = getUniqueNodeName();
        result = new TextItem(location.getFile(), location.getLine(), location.getLine(), nodeName);
        return result;
    }

    public static Guide createGuide(File newGuideFile) throws IOException {
        Guide result = new Guide(newGuideFile);
        result.readItems();
        result.defineMacros();
        result.resolveMacros();
        result.collectNodes();
        result.validateCommands();
        return result;
    }

    private Tag createMacro(CommandItem macro) {
        assert macro.getCommandName().equals("macro");
        Tag result = null;
        String macroName = null;
        int itemCount = macro.getItems().size();

        if (itemCount >= 2) {
            AbstractItem firstItem = macro.getItems().get(0);
            assert firstItem instanceof SpaceItem : "first macro item must be " + SpaceItem.class + " but is "
                    + firstItem.getClass();
            AbstractTextItem macroNameItem = (AbstractTextItem) macro.getItems().get(1);
            macroName = macroNameItem.getText().toLowerCase();

            AbstractTextItem macroTextItem;
            if (itemCount >= 4) {
                AbstractItem thirdItem = macro.getItems().get(2);
                assert thirdItem instanceof SpaceItem : "third macro item must be " + SpaceItem.class + " but is "
                        + firstItem.getClass();
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

    /**
     * Items the guide consists of after it has been fixed and cleaned up.
     */
    public List<AbstractItem> getItems() {
        return items;
    }

    // TODO: Implement pretty printing with macros and remove
    // checkNoMacrosHaveBeenDefined().
    private void checkNoMacrosHaveBeenDefined() {
        if (hasMacros) {
            throw new IllegalStateException("pretty printing with defined macros must be implemented");
        }
    }

    public void writePretty(Writer writer) throws IOException {
        checkNoMacrosHaveBeenDefined();
        for (AbstractItem item : getItems()) {
            writer.write(item.toPrettyAmigaguide());
        }
    }

    public void writePretty(File targetFile) throws IOException {
        checkNoMacrosHaveBeenDefined();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile),
                "ISO-8859-1"));
        try {
            writePretty(writer);
        } finally {
            writer.close();
        }
    }
}
