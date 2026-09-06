package com.feedrank.service;

import com.feedrank.dto.request.LoginRequest;
import com.feedrank.dto.request.RegisterRequest;
import com.feedrank.dto.response.AuthResponse;
import com.feedrank.entity.User;
import com.feedrank.exception.DuplicateResourceException;
import com.feedrank.exception.UnauthorizedException;
import com.feedrank.repository.UserRepository;
import com.feedrank.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Auth business logic (controllers only handle HTTP). */
@Service
public class AuthService {

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtUtil jwt;

  public AuthService(UserRepository users, PasswordEncoder encoder, JwtUtil jwt) {
    this.users = users;
    this.encoder = encoder;
    this.jwt = jwt;
  }

  @Transactional
  public AuthResponse register(RegisterRequest req) {
    if (users.existsByUsername(req.username())) {
      throw new DuplicateResourceException("username taken");
    }
    User u = new User();
    u.setUsername(req.username());
    u.setPasswordHash(encoder.encode(req.password()));
    users.save(u);
    return new AuthResponse(jwt.generate(u.getId(), u.getUsername()), u.getUsername(), u.getId());
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest req) {
    var u = users.findByUsername(req.username())
        .filter(user -> encoder.matches(req.password(), user.getPasswordHash()))
        .orElseThrow(() -> new UnauthorizedException("invalid credentials"));
    return new AuthResponse(jwt.generate(u.getId(), u.getUsername()), u.getUsername(), u.getId());
  }
}
