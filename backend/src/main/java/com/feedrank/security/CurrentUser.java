package com.feedrank.security;

import org.springframework.security.core.Authentication;

/** Extracts the authenticated user id (stored as principal by JwtAuthFilter). */
public final class CurrentUser {
  private CurrentUser() {}

  public static Long id(Authentication auth) {
    return (Long) auth.getPrincipal();
  }
}
