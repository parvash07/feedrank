package com.feedrank.controller;

import com.feedrank.dto.response.PreferenceVectorResponse;
import com.feedrank.dto.response.PreferencesResponse;
import com.feedrank.security.CurrentUser;
import com.feedrank.service.PreferenceService;
import com.feedrank.service.PreferenceSummaryService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preferences")
public class PreferencesController {

  private final PreferenceSummaryService summary;
  private final PreferenceService prefs;

  public PreferencesController(PreferenceSummaryService summary, PreferenceService prefs) {
    this.summary = summary;
    this.prefs = prefs;
  }

  @GetMapping
  public ResponseEntity<PreferencesResponse> mine(Authentication auth) {
    return ResponseEntity.ok(summary.summary(CurrentUser.id(auth)));
  }

  @GetMapping("/vector")
  public ResponseEntity<PreferenceVectorResponse> vector(Authentication auth) {
    var v = summary.vector(CurrentUser.id(auth));
    return ResponseEntity.ok(new PreferenceVectorResponse(
        v.present(), v.dim(), v.preview(), v.updatedAt(), summary.explorationEpsilon()));
  }

  @PostMapping("/decay")
  public ResponseEntity<Map<String, Object>> decay(Authentication auth,
      @RequestParam(defaultValue = "0.98") double factor) {
    prefs.decayUser(CurrentUser.id(auth), factor);
    return ResponseEntity.ok(Map.of("ok", true, "factor", factor));
  }
}
