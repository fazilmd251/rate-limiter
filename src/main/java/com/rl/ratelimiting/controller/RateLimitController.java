package com.rl.ratelimiting.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RateLimitController {
    @GetMapping("/hello")
    public String hello(){
        return "Hi, you are within the rate limit...";
    }
}
