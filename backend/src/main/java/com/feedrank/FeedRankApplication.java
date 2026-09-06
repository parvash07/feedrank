package com.feedrank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FeedRankApplication {
  public static void main(String[] args) {
    SpringApplication.run(FeedRankApplication.class, args);
  }
}
