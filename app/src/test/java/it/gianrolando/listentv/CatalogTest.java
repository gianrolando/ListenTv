package it.gianrolando.listentv;

import static org.junit.Assert.*;

import android.content.Context;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CatalogTest {
  private Context context;
  private Catalog catalog;

  @Before
  public void setUp() {
    context = RuntimeEnvironment.getApplication();
    context.getSharedPreferences("catalog", Context.MODE_PRIVATE).edit().clear().commit();
    catalog = new Catalog(context);
  }

  @Test
  public void favoritesDoNotRebuildTheCatalogAndChannelIdsStayStable() {
    List<Catalog.Channel> first = catalog.all();
    assertEquals(48, first.size());
    assertEquals(48, first.stream().map(Catalog.Channel::id).distinct().count());
    catalog.toggle("rai1");
    assertSame(first, catalog.all());
    assertSame(first.get(0), catalog.find("rai1"));
    assertTrue(catalog.favorite("rai1"));
    assertNull(catalog.find("rairadio2"));
    assertEquals("discovery", catalog.find("tv8").group());
    assertEquals("sky", catalog.find("tv8").provider());
  }

  @Test
  public void customSourcesInvalidateOtherCatalogInstancesAndRemoveFavorites() throws Exception {
    Catalog other = new Catalog(context);
    List<Catalog.Channel> first = other.all();
    catalog.add("Test source", "https://example.com/live.m3u8");
    List<Catalog.Channel> updated = other.all();
    assertNotSame(first, updated);
    assertEquals(49, updated.size());
    Catalog.Channel custom = updated.get(48);
    assertEquals(custom, other.find(custom.id()));
    catalog.toggle(custom.id());
    assertTrue(other.favorite(custom.id()));
    catalog.remove(custom.id());
    assertEquals(48, other.all().size());
    assertNull(other.find(custom.id()));
    assertFalse(other.favorite(custom.id()));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void callersCannotMutateTheCachedCatalog() {
    catalog.all().clear();
  }

  @Test
  public void malformedCustomDataDoesNotHideBuiltInChannels() {
    context
        .getSharedPreferences("catalog", Context.MODE_PRIVATE)
        .edit()
        .putString("custom", "bad json")
        .commit();
    assertEquals(48, catalog.all().size());
    assertNotNull(catalog.find("rai1"));
  }
}
