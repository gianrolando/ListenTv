package it.gianrolando.listentv;

import static org.junit.Assert.*;
import org.junit.Test;

public class DiagnosticTextTest {
    @Test public void excludesMessagesThatMayContainStreamTokens() {
        Exception cause=new IllegalArgumentException("https://example.com/stream?token=secret-token");
        Exception error=new RuntimeException("Bearer secret-token",cause);
        String report=DiagnosticText.describe(error);
        assertTrue(report.contains("java.lang.RuntimeException"));
        assertTrue(report.contains("java.lang.IllegalArgumentException"));
        assertFalse(report.contains("secret-token"));
        assertFalse(report.contains("https://"));
        assertFalse(report.contains("Bearer"));
    }
}
