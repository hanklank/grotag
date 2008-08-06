package net.sf.grotag.guide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.grotag.common.Tools;
import net.sf.grotag.parse.FileSource;
import net.sf.grotag.parse.MessageItem;
import net.sf.grotag.parse.MessagePool;

public class GuidePile {
    private Map<String, Guide> guideMap;
    private List<Guide> guideList;
    private List<Link> linksToValidate;
    private Logger log;
    private MessagePool messagePool;
    private Tools tools;

    public GuidePile() {
        log = Logger.getLogger(GuidePile.class.getName());
        tools = Tools.getInstance();
        messagePool = MessagePool.getInstance();

        guideMap = new TreeMap<String, Guide>();
        guideList = new ArrayList<Guide>();
        linksToValidate = new ArrayList<Link>();
    }

    public List<Guide> getGuides() {
        return guideList;
    }

    /**
     * The guide in the pile stored in <code>guideFile</code> or
     * <code>null</code> if no such guide exists. Note that this still is
     * <code>null</code> if the file exists but is not a guide.
     */
    public Guide getGuide(File guideFile) {
        Guide result;
        String guidePath = guideFile.getAbsolutePath();
        result = guideMap.get(guidePath);
        return result;
    }

    public void add(File guideFile) throws IOException {
        assert guideFile != null;
        String guidePath = guideFile.getAbsolutePath();
        assert !guideMap.containsKey(guidePath);
        Guide guide = Guide.createGuide(guideFile);
        guideMap.put(guidePath, guide);
        guideList.add(guide);
    }

    private void scheduleLinkForValidation(Link linkToValidate) {
        assert linkToValidate != null;
        linkToValidate.setState(Link.State.VALID_GUIDE_UNCHECKED_NODE);
        linksToValidate.add(linkToValidate);
    }

    public void addRecursive(File guideFile) throws IOException {
        assert guideFile != null;
        String guidePath = guideFile.getAbsolutePath();
        if (!guideMap.containsKey(guidePath)) {
            add(guideFile);
            for (Link link : guideMap.get(guidePath).getLinks()) {
                if (link.getType().equals("link")) {
                    File linkFile = link.getTargetFile();
                    assert linkFile != null;
                    try {
                        addRecursive(linkFile);
                        scheduleLinkForValidation(link);
                    } catch (FileNotFoundException error) {
                        MessageItem message = new MessageItem(link.getLinkCommand(),
                                "ignored link to to file that does not exist: " + tools.sourced(linkFile));
                        messagePool.add(message);
                    } catch (IOException error) {
                        MessageItem message = new MessageItem(link.getLinkCommand(), "cannot read linked file for "
                                + tools.sourced(link.getTarget()));
                        MessageItem seeAlso = new MessageItem(new FileSource(linkFile), "related input/output error: "
                                + error.getMessage());
                        message.setSeeAlso(seeAlso);
                        messagePool.add(message);
                    } catch (IllegalArgumentException errorToIgnore) {
                        link.setState(Link.State.VALID_OTHER_FILE);
                        log.log(Level.WARNING, "skipped non-guide: " + tools.sourced(linkFile), errorToIgnore);
                    }
                } else {
                    // Local link, no need to check the file.
                    scheduleLinkForValidation(link);
                }
            }
        }
    }

    public void validateLinks() {
        for (Link link : linksToValidate) {
            Link.State linkState = link.getState();
            if ((linkState == Link.State.UNCHECKED) && (linkState == Link.State.VALID_GUIDE_UNCHECKED_NODE)) {
                File linkedFile = link.getTargetFile();
                String linkedNodeName = link.getTargetNode();
                Guide guideContainingNode = guideMap.get(linkedFile.getAbsolutePath());
                assert guideContainingNode != null;
                NodeInfo nodeInfo = guideContainingNode.getNodeInfo(linkedNodeName);
                if (nodeInfo == null) {
                    MessageItem message = new MessageItem(link.getLinkCommand(), "cannot find node "
                            + tools.sourced(linkedNodeName) + " in " + tools.sourced(linkedFile));
                    messagePool.add(message);
                }
            }
        }
    }
}
