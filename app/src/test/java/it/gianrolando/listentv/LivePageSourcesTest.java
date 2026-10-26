package it.gianrolando.listentv;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;

public class LivePageSourcesTest {
    @Test public void skipsPlayerScriptsAndPreservesSignedPaths() throws Exception {
        String expected="https://cdn.example/token=a&path=%2Flive&expires=123/live/ch01/playlist.m3u8";
        String page="<script src=\"https://cdn.example/player.js\"></script>video.src = \""+expected+"\";";
        assertEquals(expected,LivePageSources.hls(page));
    }
    @Test public void handlesEscapedSlashesAndEntities() throws Exception {
        assertEquals("https://cdn.example/live.m3u8?a=1&b=2",LivePageSources.hls("src='https:\\/\\/cdn.example\\/live.m3u8?a=1&amp;b=2'"));
    }
    @Test public void rejectsInsecureAndCredentialBearingSources() throws Exception {
        for(String source:new String[]{"http://cdn.example/live.m3u8","https://user:secret@cdn.example/live.m3u8","https://cdn.example/player.js"}) {
            try { LivePageSources.hls("src=\""+source+"\"");fail(source); } catch(IOException expected) { }
        }
    }
}
