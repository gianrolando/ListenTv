package it.gianrolando.listentv;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EpgXmlTvTest {
    private List<EpgProgramme> read(String xml) throws Exception {
        return EpgXmlTv.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),Set.of("rai1","rainews"),EpgProgramme.xmlTime("20261026200000 +0100"));
    }
    @Test public void readsTitlesAndFiltersUnknownExpiredAndPlaceholderEntries() throws Exception {
        List<EpgProgramme> result=read("<tv>"
            +"<programme channel='rai1' start='20261026190000 +0100' stop='20261026210000 +0100'><title>News &amp; <![CDATA[weather]]></title><desc>Not retained</desc></programme>"
            +"<programme channel='rainews24' start='20261026190000 +0100' stop='20261026210000 +0100'><title>EPG non disponibile</title></programme>"
            +"<programme channel='other' start='20261026190000 +0100' stop='20261026210000 +0100'><title>Other</title></programme>"
            +"<programme channel='rai1' start='invalid' stop='invalid'><title>Bad timestamp</title></programme>"
            +"<programme channel='rai1' start='20261025010000 +0100' stop='20261025020000 +0100'><title>Yesterday</title></programme></tv>");
        assertEquals(1,result.size());assertEquals("News & weather",result.get(0).title());
    }
    @Test(expected=IOException.class) public void rejectsDocumentTypesBeforeEntityExpansion() throws Exception {
        read("<!DOCTYPE tv [<!ENTITY secret SYSTEM 'file:///nonexistent'>]><tv><programme><title>&secret;</title></programme></tv>");
    }
    @Test(expected=IOException.class) public void rejectsNonGuideDocuments() throws Exception {read("<html><body>Unavailable</body></html>");}
    @Test(expected=IOException.class) public void enforcesExpandedByteLimit() throws Exception {
        InputStream input=new EpgXmlTv.LimitedInput(new ByteArrayInputStream(new byte[20]),10);
        input.read(new byte[20]);
    }
}
