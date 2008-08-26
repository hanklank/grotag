package net.sf.grotag.view;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.parser.ParserDelegator;

import net.sf.grotag.guide.Relation;

/**
 * Information about an HTML document.
 * 
 * @author Thomas Aglassinger
 */
class HtmlInfo {
    private Map<Relation, URL> relationMap;
    private Map<String, Relation> relToRelationMap;
    private Logger log;
    private URL baseUrl;

    /**
     * HTML parser callback to extract &lt;link&gt; relations.
     * 
     * @author Thomas Aglassinger
     */
    private class InfoCallback extends HTMLEditorKit.ParserCallback {
        public InfoCallback() {
            relationMap.clear();
        }

        @Override
        public void handleSimpleTag(Tag tag, MutableAttributeSet attributes, int pos) {
            assert tag != null;
            assert attributes != null;
            if (tag == Tag.LINK) {
                String rel = (String) attributes.getAttribute(HTML.Attribute.REL);
                if (rel != null) {
                    String href = (String) attributes.getAttribute(HTML.Attribute.HREF);
                    if (href != null) {
                        Relation relation = relToRelationMap.get(rel);
                        if (relation != null) {
                            try {
                                relationMap.put(relation, new URL(baseUrl, href));
                                log.info("added relation: " + relation + "=" + relationMap.get(relation));
                            } catch (MalformedURLException error) {
                                log.log(Level.SEVERE, "cannot add related href: " + href, error);
                                // FIXME: Add stack trace to log.
                                error.printStackTrace();
                            }
                        } else {
                            log.warning("ignored unknown rel: " + rel);
                        }
                    } else {
                        log.warning("ignored <link> without href at " + pos);
                    }
                } else {
                    log.warning("ignored <link> without rel at " + pos);
                }
            }
        }

        @Override
        public void handleStartTag(Tag t, MutableAttributeSet a, int pos) {
            // Do nothing.
        }

        @Override
        public void handleComment(char[] data, int pos) {
            // Do nothing.
        }

        @Override
        public void handleEndOfLineString(String eol) {
            // Do nothing.
        }

        @Override
        public void handleEndTag(Tag t, int pos) {
            // Do nothing.
        }

        @Override
        public void handleText(char[] data, int pos) {
            // Do nothing.
        }
    }

    HtmlInfo(URL newBaseUrl) throws IOException {
        assert newBaseUrl != null;

        log = Logger.getLogger(HtmlInfo.class.getName());
        relationMap = new TreeMap<Relation, URL>();
        relToRelationMap = new TreeMap<String, Relation>();
        relToRelationMap.put("help", Relation.help);
        relToRelationMap.put("index", Relation.index);
        relToRelationMap.put("next", Relation.next);
        relToRelationMap.put("prev", Relation.previous);
        relToRelationMap.put("toc", Relation.toc);

        baseUrl = newBaseUrl;
        InfoCallback callback = new InfoCallback();
        ParserDelegator parser = new ParserDelegator();
        Reader reader = new InputStreamReader(baseUrl.openStream());
        try {
            parser.parse(reader, callback, true);
        } finally {
            reader.close();
        }
    }

    Map<Relation, URL> getRelationMap() {
        return relationMap;
    }
}
