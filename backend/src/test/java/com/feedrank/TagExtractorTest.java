package com.feedrank;

import com.feedrank.service.TagExtractor;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TagExtractorTest {
  private final TagExtractor ex = new TagExtractor();

  @Test void extractsTechTags() {
    List<String> tags = ex.extract("Show HN: I built a Rust Postgres vector database for AI startups");
    assertTrue(tags.contains("ai") || tags.contains("postgres") || tags.contains("rust"));
    assertTrue(tags.size() <= 5);
  }

  @Test void emptyTitleGivesGeneral() {
    assertEquals(List.of("general"), ex.extract(""));
    assertEquals(List.of("general"), ex.extract(null));
  }

  @Test void stopwordsFiltered() {
    List<String> tags = ex.extract("The Way of the New Way");
    assertFalse(tags.contains("the"));
  }
}
