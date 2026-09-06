package com.feedrank.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.feedrank.entity.FeedItem;
import com.feedrank.repository.FeedItemRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

/** Phase 1: polls Hacker News API on a schedule, normalizes + dedupes into Postgres. */
@Service
public class HnIngestionService {
  private static final Logger log = LoggerFactory.getLogger(HnIngestionService.class);

  private final WebClient hn;
  private final FeedItemRepository items;
  private final TagExtractor tags;
  private final boolean enabled;
  private final int maxItemsPerRun;
  private final boolean seedOnStartup;

  public HnIngestionService(WebClient hnWebClient, FeedItemRepository items, TagExtractor tags,
      @Value("${app.ingestion.enabled:true}") boolean enabled,
      @Value("${app.ingestion.max-items-per-run:60}") int maxItemsPerRun,
      @Value("${app.ingestion.seed-on-startup:true}") boolean seedOnStartup) {
    this.hn = hnWebClient;
    this.items = items;
    this.tags = tags;
    this.enabled = enabled;
    this.maxItemsPerRun = maxItemsPerRun;
    this.seedOnStartup = seedOnStartup;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    if (enabled && seedOnStartup) {
      try { ingestTopStories(); }
      catch (Exception e) { log.warn("startup ingestion failed (will retry on schedule): {}", e.toString()); }
    }
  }

  @Scheduled(fixedDelayString = "${app.ingestion.cron-fixed-delay-ms:900000}")
  public void scheduledIngest() {
    if (!enabled) return;
    try { ingestTopStories(); }
    catch (Exception e) { log.warn("scheduled ingestion failed: {}", e.toString()); }
  }

  @Transactional
  public IngestResult ingestTopStories() {
    List<Integer> ids;
    try {
      Integer[] arr = hn.get().uri("/topstories.json")
          .retrieve().bodyToMono(Integer[].class)
          .timeout(Duration.ofSeconds(20)).block();
      ids = arr == null ? List.of() : Arrays.asList(arr);
    } catch (Exception e) {
      log.warn("HN topstories fetch failed: {}", e.toString());
      return new IngestResult(0, 0, true);
    }
    int fetched = 0, inserted = 0;
    for (Integer hnId : ids.stream().limit(maxItemsPerRun).toList()) {
      String externalId = "hn-" + hnId;
      if (items.existsByExternalId(externalId)) continue;
      try {
        JsonNode node = hn.get().uri("/item/{id}.json", hnId)
            .retrieve().bodyToMono(JsonNode.class)
            .timeout(Duration.ofSeconds(10)).block();
        fetched++;
        if (node == null || node.path("deleted").asBoolean(false)
            || node.path("dead").asBoolean(false)) continue;
        String title = node.path("title").asText(null);
        if (title == null || title.isBlank()) continue;

        FeedItem item = new FeedItem();
        item.setExternalId(externalId);
        item.setTitle(title);
        item.setUrl(node.path("url").asText(null));
        item.setSource("hackernews");
        item.setAuthor(node.path("by").asText(null));
        item.setScore(node.path("score").asInt(0));
        item.setTagList(tags.extract(title));
        item.setSummary(null);
        long hnTimeSec = node.path("time").asLong(0);
        item.setHnTime(hnTimeSec == 0 ? null : hnTimeSec);
        item.setCreatedAt(hnTimeSec == 0 ? Instant.now() : Instant.ofEpochSecond(hnTimeSec));
        items.save(item);
        inserted++;
      } catch (Exception e) {
        log.debug("skip hn-{}: {}", hnId, e.toString());
      }
    }
    log.info("HN ingest: fetched={} inserted={}", fetched, inserted);
    return new IngestResult(fetched, inserted, false);
  }

  public record IngestResult(int fetched, int inserted, boolean failed) {}
}
