package net.sf.grotag.guide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.grotag.common.AmigaTools;
import net.sf.grotag.common.Tools;
import net.sf.grotag.parse.FileSource;
import net.sf.grotag.parse.MessageItem;
import net.sf.grotag.parse.MessagePool;

public class GuidePile {
    private AmigaTools amigaTools;
    private Map<String, Guide> guides;
    private Logger log;
    private MessagePool messagePool;
    private Tools tools;

    public GuidePile() {
        log = Logger.getLogger(GuidePile.class.getName());
        tools = Tools.getInstance();
        amigaTools = AmigaTools.getInstance();
        messagePool = MessagePool.getInstance();

        guides = new HashMap<String, Guide>();
    }

    public Map<String, Guide> getGuides() {
        return guides;
    }

    public void add(File guideFile) throws IOException {
        assert guideFile != null;
        String guidePath = guideFile.getAbsolutePath();
        assert !guides.containsKey(guidePath);
        Guide guide = Guide.createGuide(guideFile);
        guides.put(guidePath, guide);
    }

    public void addRecursive(File guideFile) throws IOException {
        assert guideFile != null;
        String guidePath = guideFile.getAbsolutePath();
        if (!guides.containsKey(guidePath)) {
            add(guideFile);
            for (Link link : guides.get(guidePath).getLinks()) {
                String target = link.getTarget();
                int slashIndex = target.lastIndexOf('/');
                link.setState(Link.State.BROKEN);
                if (slashIndex >= 0) {
                    String linkAmigaPath = target.substring(0, slashIndex);
                    String linkNodeName = target.substring(slashIndex + 1);
                    File baseFolder = guideFile.getParentFile();
                    File linkFile = amigaTools.getFileFor(linkAmigaPath, baseFolder);
                    log.fine("mapping link: " + tools.sourced(linkAmigaPath) + " -> " + tools.sourced(linkFile) + ", "
                            + tools.sourced(linkNodeName));
                    try {
                        Guide guide = Guide.createGuide(linkFile);
                        guides.put(linkFile.getAbsolutePath(), guide);
                        link.setState(Link.State.VALID_GUIDE_UNCHECKED_NODE);
                    } catch (FileNotFoundException error) {
                        MessageItem message = new MessageItem(link.getSourceItem(),
                                "ignored link to to file that does not exist: " + tools.sourced(linkFile));
                        messagePool.add(message);
                    } catch (IOException error) {
                        MessageItem message = new MessageItem(link.getSourceItem(), "cannot read linked file: "
                                + tools.sourced(linkAmigaPath));
                        MessageItem seeAlso = new MessageItem(new FileSource(linkFile), "system error: "
                                + error.getMessage());
                        message.setSeeAlso(seeAlso);
                        messagePool.add(message);
                    } catch (IllegalArgumentException errorToIgnore) {
                        link.setState(Link.State.VALID_OTHER_FILE);
                        log.log(Level.WARNING, "skipped non-guide: " + tools.sourced(linkFile), errorToIgnore);
                    }
                } else {
                    // Local link, no need to check the file.
                    link.setState(Link.State.VALID_GUIDE_UNCHECKED_NODE);
                }
            }
        }
    }

    public void validateLinks() {
        // TODO: implement
    }
}
