package com.feedrank.controller;

import com.feedrank.dto.response.AdminStatsResponse;
import com.feedrank.dto.response.ImpressionResponse;
import com.feedrank.security.CurrentUser;
import com.feedrank.service.AdminService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final AdminService admin;

  public AdminController(AdminService admin) {
    this.admin = admin;
  }

  @GetMapping("/stats")
  public ResponseEntity<AdminStatsResponse> stats(Authentication auth) {
    return ResponseEntity.ok(admin.stats(CurrentUser.id(auth)));
  }

  @GetMapping("/impressions")
  public ResponseEntity<List<ImpressionResponse>> impressions(Authentication auth) {
    return ResponseEntity.ok(admin.recentImpressions(CurrentUser.id(auth)));
  }
}
