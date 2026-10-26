package it.gianrolando.listentv;

import org.junit.Test;
import static org.junit.Assert.*;
import java.time.Instant;
import java.util.*;

public class EpgProgrammeTest {
    @Test public void convertsExplicitSummerAndWinterOffsets() {
        assertEquals(Instant.parse("2026-07-26T19:00:00Z").toEpochMilli(),EpgProgramme.xmlTime("20260726210000 +0200"));
        assertEquals(Instant.parse("2026-10-26T20:00:00Z").toEpochMilli(),EpgProgramme.xmlTime("20261026210000 +0100"));
        assertEquals(EpgProgramme.xmlTime("20261026210000 +0100"),EpgProgramme.xmlTime("202610262100 +0100"));
    }
    @Test public void programmeEndsExactlyWhenTheNextOneStarts() {
        EpgProgramme first=new EpgProgramme("rai1","First",100,200),next=new EpgProgramme("rai1","Next",200,300);
        assertEquals(first,EpgProgramme.current(List.of(first,next),199));
        assertEquals(next,EpgProgramme.current(List.of(first,next),200));
        assertNull(EpgProgramme.current(List.of(first,next),300));
        assertEquals(50,first.progress(150));
    }
    @Test public void mapsLegacyIdsWithoutMislabelingItaliana() {
        Set<String> known=Set.of("rainews","discoveryturbo","italiana","rai1");
        assertEquals("rainews",EpgProgramme.channelId("RaiNews24.it",known));
        assertEquals("discoveryturbo",EpgProgramme.channelId("motortrend",known));
        assertNull(EpgProgramme.channelId("raiscuola",known));
        assertNull(EpgProgramme.channelId("SamsungRai1.it",known));
        assertFalse(EpgProgramme.validTitle("EPG non disponibile"));
    }
    @Test(expected=RuntimeException.class) public void rejectsInvalidCalendarDates() {EpgProgramme.xmlTime("20260230210000 +0100");}
}
