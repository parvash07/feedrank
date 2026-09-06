package com.feedrank.controller;

import com.feedrank.dto.response.FeedItemResponse;
import com.feedrank.dto.response.IngestResponse;
import com.feedrank.mapper.FeedItemMapper;
import com.feedrank.security.CurrentUser;
import com.feedrank.service.HnIngestionService;
import com.feedrank.service.ranking.FeedService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

  private final FeedService feed;
  private final HnIngestionService ingestion;
  private final FeedItemMapper mapper;

  public FeedController(FeedService feed, HnIngestionService ingestion, FeedItemMapper mapper) {
    this.feed = feed;
    this.ingestion = ingestion;
    this.mapper = mapper;
  }

  @GetMapping
  public ResponseEntity<List<FeedItemResponse>> feed(Authentication auth,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(required = false) String mode,
      @RequestParam(defaultValue = "true") boolean explore) {
    var scored = feed.feedFor(CurrentUser.id(auth), clamp(limit), mode, explore);
    return ResponseEntity.ok(scored.stream().map(mapper::toResponse).toList());
  }

  @GetMapping("/latest")
  public ResponseEntity<List<FeedItemResponse>> latest(@RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(feed.latestChronological(clamp(limit))
        .stream().map(mapper::toResponse).toList());
  }

  @PostMapping("/ingest")
  public ResponseEntity<IngestResponse> triggerIngest() {
    var r = ingestion.ingestTopStories();
    return ResponseEntity.ok(new IngestResponse(r.fetched(), r.inserted()));
  }

  private static int clamp(int limit) {
    return Math.min(Math.max(limit, 1), 100);
  }
}
