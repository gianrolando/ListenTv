package it.gianrolando.listentv;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.*;
import org.junit.Test;

public class EpgProgrammeTest {
  @Test
  public void convertsExplicitSummerAndWinterOffsets() {
    assertEquals(
        Instant.parse("2026-07-26T19:00:00Z").toEpochMilli(),
        EpgProgramme.xmlTime("20260726210000 +0200"));
    assertEquals(
        Instant.parse("2026-10-26T20:00:00Z").toEpochMilli(),
        EpgProgramme.xmlTime("20261026210000 +0100"));
    assertEquals(
        EpgProgramme.xmlTime("20261026210000 +0100"), EpgProgramme.xmlTime("202610262100 +0100"));
  }

  @Test
  public void programmeEndsExactlyWhenTheNextOneStarts() {
    EpgProgramme first = new EpgProgramme("rai1", "First", 100, 200),
        next = new EpgProgramme("rai1", "Next", 200, 300);
    assertEquals(first, EpgProgramme.current(List.of(first, next), 199));
    assertEquals(next, EpgProgramme.current(List.of(first, next), 200));
    assertNull(EpgProgramme.current(List.of(first, next), 300));
    assertEquals(50, first.progress(150));
  }

  @Test
  public void mapsLegacyIdsWithoutMislabelingItaliana() {
    Set<String> known = Set.of("rainews", "discoveryturbo", "italiana", "rai1");
    assertEquals("rainews", EpgProgramme.channelId("RaiNews24.it", known));
    assertEquals("discoveryturbo", EpgProgramme.channelId("motortrend", known));
    assertNull(EpgProgramme.channelId("raiscuola", known));
    assertNull(EpgProgramme.channelId("SamsungRai1.it", known));
    assertFalse(EpgProgramme.validTitle("EPG non disponibile"));
  }

  @Test(expected = RuntimeException.class)
  public void rejectsInvalidCalendarDates() {
    EpgProgramme.xmlTime("20260230210000 +0100");
  }

  @Test
  public void timestampsAreIndependentOfDeviceLocale() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("ar-EG"));
      assertEquals(
          Instant.parse("2026-10-26T20:00:00Z").toEpochMilli(),
          EpgProgramme.xmlTime("20261026210000 +0100"));
    } finally {
      Locale.setDefault(previous);
    }
  }

  @Test
  public void cachedProgrammesRequireValidTitlesDurationsAndWindow() {
    long now = Instant.parse("2026-10-26T20:00:00Z").toEpochMilli();
    assertTrue(new EpgProgramme("rai1", "News", now - 1000, now + 1000).retain(now));
    assertFalse(
        new EpgProgramme("rai1", "EPG non disponibile", now - 1000, now + 1000).retain(now));
    assertFalse(new EpgProgramme("rai1", "News", now, now).retain(now));
    assertFalse(new EpgProgramme("rai1", "News", Long.MIN_VALUE, now).retain(now));
    assertFalse(new EpgProgramme("rai1", "News", now, now + 25 * 60 * 60 * 1000L).retain(now));
    assertFalse(
        new EpgProgramme("rai1", "News", now - 8 * 60 * 60 * 1000L, now - 7 * 60 * 60 * 1000L)
            .retain(now));
    assertFalse(
        new EpgProgramme("rai1", "News", now + 49 * 60 * 60 * 1000L, now + 50 * 60 * 60 * 1000L)
            .retain(now));
  }

  @Test
  public void partialRefreshPreservesCurrentRaiProgrammeAndReplacesCorrectedSlots() {
    long now = Instant.parse("2026-10-26T20:00:00Z").toEpochMilli();
    EpgProgramme current = new EpgProgramme("rai4k", "Current", now - 1000, now + 1000);
    EpgProgramme next = new EpgProgramme("rai4k", "Next", now + 1000, now + 2000);
    List<EpgProgramme> partial = EpgProgramme.merge(List.of(current), List.of(next), now);
    assertEquals(current, EpgProgramme.current(partial, now));
    EpgProgramme corrected = new EpgProgramme("rai4k", "Corrected", current.start(), current.end());
    List<EpgProgramme> refreshed = EpgProgramme.merge(partial, List.of(corrected), now);
    assertEquals(2, refreshed.size());
    assertEquals(corrected, EpgProgramme.current(refreshed, now));
    assertTrue(
        EpgProgramme.merge(List.of(current), List.of(), now + 8 * 60 * 60 * 1000L).isEmpty());
  }
}
