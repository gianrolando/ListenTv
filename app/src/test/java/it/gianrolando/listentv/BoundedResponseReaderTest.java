package it.gianrolando.listentv;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class BoundedResponseReaderTest {
    private ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test public void readsMetadataWithoutDownloadingMultiMegabytePageBody() throws Exception {
        String head = "<html><head><meta name=\"app-name\" content=\"web/test\"/></head>";
        ByteArrayInputStream in = stream(head + "<body>" + "x".repeat(6 * 1024 * 1024));
        assertEquals(head, BoundedResponseReader.read(in, 128 * 1024, true));
        assertTrue("Body must remain unread", in.available() > 5 * 1024 * 1024);
    }

    @Test public void headEndCanCrossReadBoundaryAndUseMixedCase() throws Exception {
        String head = "<head>" + "x".repeat(8183) + "</HeAd>";
        assertEquals(head, BoundedResponseReader.read(stream(head + "<body>"), 128 * 1024, true));
    }

    @Test public void missingHeadCannotDownloadAnUnboundedPage() throws Exception {
        ByteArrayInputStream in = stream("x".repeat(1024 * 1024));
        try {
            BoundedResponseReader.read(in, 128 * 1024, true);
            fail("Expected a bounded-read failure");
        } catch (IOException expected) {
            assertEquals(1024 * 1024 - 128 * 1024, in.available());
        }
    }

    @Test public void completeApiResponsesKeepTheirExistingLimit() throws Exception {
        assertEquals("{}", BoundedResponseReader.read(stream("{}"), 2, false));
        try {
            BoundedResponseReader.read(stream("123"), 2, false);
            fail("Oversized API response must fail");
        } catch (IOException expected) {
            assertEquals("Response too large.", expected.getMessage());
        }
    }

    @Test public void incompleteHtmlHeadIsRejected() throws Exception {
        try {
            BoundedResponseReader.read(stream("<head><meta name=\"app-name\">"), 128 * 1024, true);
            fail("Incomplete head must fail");
        } catch (IOException expected) {
            assertEquals("HTML head not found.", expected.getMessage());
        }
    }
}
