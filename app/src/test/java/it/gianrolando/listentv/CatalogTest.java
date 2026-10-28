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
    assertEquals(51, first.size());
    assertEquals(51, first.stream().map(Catalog.Channel::id).distinct().count());
    catalog.toggle("rai1");
    assertSame(first, catalog.all());
    assertSame(first.get(0), catalog.find("rai1"));
    assertTrue(catalog.favorite("rai1"));
    assertNull(catalog.find("rairadio2"));
    assertEquals("discovery", catalog.find("tv8").group());
    assertEquals("sky", catalog.find("tv8").provider());
    for (String id : List.of("sportitalia", "ilsole24oretv", "telecapodistria"))
      assertFalse(List.of("rai", "mediaset", "discovery").contains(catalog.find(id).group()));
  }

  @Test
  public void customSourcesInvalidateOtherCatalogInstancesAndRemoveFavorites() throws Exception {
    Catalog other = new Catalog(context);
    List<Catalog.Channel> first = other.all();
    catalog.add("Test source", "https://example.com/live.m3u8");
    List<Catalog.Channel> updated = other.all();
    assertNotSame(first, updated);
    assertEquals(52, updated.size());
    Catalog.Channel custom = updated.get(51);
    assertEquals(custom, other.find(custom.id()));
    catalog.toggle(custom.id());
    assertTrue(other.favorite(custom.id()));
    catalog.remove(custom.id());
    assertEquals(51, other.all().size());
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
    assertEquals(51, catalog.all().size());
    assertNotNull(catalog.find("rai1"));
  }

  @Test
  public void channelArtworkRemainsValidForAndroidAuto() {
    for (String id : List.of("rai1", "sportitalia", "ilsole24oretv", "telecapodistria")) {
      byte[] bytes = ChannelArtwork.data(context, id);
      assertNotNull(bytes);
      android.graphics.Bitmap bitmap =
          android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
      assertNotNull(bitmap);
      assertEquals(512, bitmap.getWidth());
      assertEquals(512, bitmap.getHeight());
    }
  }
}
