package com.feedrank.controller;

import com.feedrank.dto.request.InteractionRequest;
import com.feedrank.dto.response.InteractionResponse;
import com.feedrank.mapper.InteractionMapper;
import com.feedrank.repository.InteractionRepository;
import com.feedrank.security.CurrentUser;
import com.feedrank.service.PreferenceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

  private final PreferenceService prefs;
  private final InteractionRepository interactions;
  private final InteractionMapper mapper;

  public InteractionController(PreferenceService prefs, InteractionRepository interactions,
      InteractionMapper mapper) {
    this.prefs = prefs;
    this.interactions = interactions;
    this.mapper = mapper;
  }

  @PostMapping
  public ResponseEntity<InteractionResponse> log(Authentication auth,
      @Valid @RequestBody InteractionRequest req) {
    var in = prefs.logInteraction(CurrentUser.id(auth), req.itemId(),
        req.interactionType(), req.dwellTimeMs());
    return ResponseEntity.ok(mapper.toResponse(in));
  }

  @GetMapping
  public ResponseEntity<List<InteractionResponse>> mine(Authentication auth) {
    var history = interactions.findTop200ByUserIdOrderByCreatedAtDesc(CurrentUser.id(auth));
    return ResponseEntity.ok(history.stream().map(mapper::toResponse).toList());
  }
}
