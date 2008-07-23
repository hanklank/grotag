package net.sf.grotag.parse;

import java.util.Map;
import java.util.TreeMap;

public class TagPool {
    private Map<String, Tag> tagMap;
    private Map<String, Tag> linkMap;

    public TagPool() {
        TagOption any = new TagOption(TagOption.Type.ANY);
        TagOption color = new TagOption(TagOption.Type.COLOR);
        TagOption file = new TagOption(TagOption.Type.FILE);
        TagOption filenode = new TagOption(TagOption.Type.FILENODE);
        TagOption node = new TagOption(TagOption.Type.NODE);
        TagOption number = new TagOption(TagOption.Type.NUMBER);
        TagOption some = new TagOption(TagOption.Type.SOME);
        TagOption text = new TagOption(TagOption.Type.TEXT);

        tagMap = new TreeMap<String, Tag>();
        linkMap = new TreeMap<String, Tag>();

        // Amigaguide version 34 (Workbench 1.3 and 2.x)
        Tag.Version v34 = Tag.Version.V34;
        addTag(new Tag("$ver:", v34, Tag.Scope.GLOBAL, true, some));
        addTag(new Tag("$(c)", v34, Tag.Scope.GLOBAL, true, some));
        addTag(new Tag("author", v34, Tag.Scope.GLOBAL, true, some));
        // TODO: Check if @database has option "some" instead of "any".
        addTag(new Tag("database", v34, Tag.Scope.GLOBAL, true, any));
        // TODO: Mark @dnode as "obsolete".
        addTag(new Tag("dnode", v34, Tag.Scope.GLOBAL, true, any));
        addTag(new Tag("endnode", v34, Tag.Scope.GLOBAL));
        addTag(new Tag("font", v34, Tag.Scope.GLOBAL, true, new TagOption[] {
                text, number }));
        // TODO: Mark @height as "unused".
        addTag(new Tag("height", v34, Tag.Scope.GLOBAL, true, number));
        addTag(new Tag("help", v34, Tag.Scope.GLOBAL, true, node));
        addTag(new Tag("index", v34, Tag.Scope.GLOBAL, true, node));
        // TODO: Mark @master as "unused".
        addTag(new Tag("master", v34, Tag.Scope.GLOBAL, true, text));
        addTag(new Tag("node", v34, Tag.Scope.GLOBAL, new TagOption[] { text,
                text }));
        addTag(new Tag("rem", v34, Tag.Scope.GLOBAL));
        addTag(new Tag("remark", v34, Tag.Scope.GLOBAL));
        // TODO: Mark @width as "unused".
        addTag(new Tag("width", v34, Tag.Scope.GLOBAL, true, number));

        addTag(new Tag("font", v34, Tag.Scope.NODE, true, new TagOption[] {
                text, number }));
        addTag(new Tag("help", v34, Tag.Scope.NODE, true, node));
        addTag(new Tag("index", v34, Tag.Scope.NODE, true, node));
        addTag(new Tag("keywords", v34, Tag.Scope.NODE, true, any));
        addTag(new Tag("next", v34, Tag.Scope.NODE, true, node));
        addTag(new Tag("prev", v34, Tag.Scope.NODE, true, node));
        addTag(new Tag("title", v34, Tag.Scope.NODE, true, text));
        addTag(new Tag("toc", v34, Tag.Scope.NODE, true, node));

        addTag(new Tag("bg", v34, Tag.Scope.INLINE, true, color));
        addTag(new Tag("fg", v34, Tag.Scope.INLINE, true, color));

        // TODO: Use optional "number" instead of "any" for @{alink}
        addTag(Tag.createLink("alink", v34, new TagOption[] { node, any }));
        addTag(Tag.createLink("close", v34));
        addTag(Tag.createLink("link", v34, new TagOption[] { filenode, any }));
        addTag(Tag.createLink("rx", v34, text));
        addTag(Tag.createLink("rxs", v34, file));
        addTag(Tag.createLink("system", v34, text));
        addTag(Tag.createLink("quit", v34));

        // Amigaguide version 39 (Workbench 3.0)
        v34 = Tag.Version.V39;
        Tag.Version v39 = Tag.Version.V39;
        addTag(new Tag("wordwrap", v39, Tag.Scope.GLOBAL, true));
        addTag(new Tag("xref", v39, Tag.Scope.GLOBAL, true, file));

        addTag(new Tag("embed", v39, Tag.Scope.NODE, file));
        addTag(new Tag("proportional", v39, Tag.Scope.NODE, true));
        addTag(new Tag("wordwrap", v39, Tag.Scope.NODE, true));

        addTag(new Tag("b", v39, Tag.Scope.INLINE));
        addTag(new Tag("i", v39, Tag.Scope.INLINE));
        addTag(new Tag("u", v39, Tag.Scope.INLINE));
        addTag(new Tag("ub", v39, Tag.Scope.INLINE));
        addTag(new Tag("ui", v39, Tag.Scope.INLINE));
        addTag(new Tag("uu", v39, Tag.Scope.INLINE));

        addTag(Tag.createLink("beep", v34));
        addTag(Tag.createLink("guide", v34, file));

        // Amigaguide version 40 (Workbench 3.1)
        Tag.Version v40 = Tag.Version.V40;
        addTag(new Tag("macro", v40, Tag.Scope.GLOBAL, false, text));
        addTag(new Tag("onclose", v40, Tag.Scope.GLOBAL, true, file));
        addTag(new Tag("onopen", v40, Tag.Scope.GLOBAL, true, file));
        addTag(new Tag("smartwrap", v40, Tag.Scope.GLOBAL, true));
        addTag(new Tag("tab", v40, Tag.Scope.GLOBAL, true, number));

        addTag(new Tag("onclose", v40, Tag.Scope.NODE, true, file));
        addTag(new Tag("onopen", v40, Tag.Scope.NODE, true, file));
        addTag(new Tag("smartwrap", v40, Tag.Scope.NODE, true));
        addTag(new Tag("tab", v40, Tag.Scope.NODE, true, number));

        addTag(new Tag("amigaguide", v40, Tag.Scope.INLINE));
        addTag(new Tag("apen", v40, Tag.Scope.INLINE, number));
        addTag(new Tag("body", v40, Tag.Scope.INLINE));
        addTag(new Tag("bpen", v40, Tag.Scope.INLINE, number));
        addTag(new Tag("cleartabs", v40, Tag.Scope.INLINE));
        addTag(new Tag("code", v40, Tag.Scope.INLINE));
        addTag(new Tag("jcenter", v40, Tag.Scope.INLINE));
        addTag(new Tag("jleft", v40, Tag.Scope.INLINE));
        addTag(new Tag("jright", v40, Tag.Scope.INLINE));
        addTag(new Tag("lindent", v40, Tag.Scope.INLINE, number));
        addTag(new Tag("line", v40, Tag.Scope.INLINE));
        addTag(new Tag("par", v40, Tag.Scope.INLINE));
        addTag(new Tag("pard", v40, Tag.Scope.INLINE));
        addTag(new Tag("pari", v40, Tag.Scope.INLINE, number));
        addTag(new Tag("plain", v40, Tag.Scope.INLINE));
        addTag(new Tag("settabs", v40, Tag.Scope.INLINE, some));
        addTag(new Tag("tab", v40, Tag.Scope.INLINE));
    }

    private String tagKey(String name, Tag.Scope scope) {
        return scope.toString() + ":" + name.toLowerCase();
    }

    private String tagKey(Tag tag) {
        return tagKey(tag.getName(), tag.getScope());
    }

    public void addTag(Tag tag) {
        Map<String, Tag> targetMap = getMapForScope(tag.getScope());
        targetMap.put(tagKey(tag), tag);
    }

    private Map<String, Tag> getMapForScope(Tag.Scope scope) {
        Map<String, Tag> lookupMap;

        if (scope == Tag.Scope.LINK) {
            lookupMap = linkMap;
        } else {
            lookupMap = tagMap;
        }
        return lookupMap;
    }

    public Tag getTag(String name, Tag.Scope scope) {
        Map<String, Tag> lookupMap = getMapForScope(scope);
        Tag result = lookupMap.get(tagKey(name, scope));
        if ((result == null) && (scope == Tag.Scope.NODE)) {
            result = lookupMap.get(tagKey(name, Tag.Scope.GLOBAL));
        }
        return result;
    }
    
    public Tag getMacro(String name) {
        Tag result = getTag(name, Tag.Scope.INLINE);
        if ((result != null) && !result.isMacro()) {
            result = null;
        }
        return result;
    }
}
