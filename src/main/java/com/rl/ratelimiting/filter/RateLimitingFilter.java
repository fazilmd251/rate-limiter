package com.rl.ratelimiting.filter;

import com.rl.ratelimiting.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final RateLimitingService rateLimitingService;

    public RateLimitingFilter(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp=getClientIp(request);
        Bucket tokenBucket =rateLimitingService.resolveBucket(clientIp);
        var probe=tokenBucket.tryConsumeAndReturnRemaining(1);
        if(probe.isConsumed()){
            response.addHeader("X-Rate-Limit-Remaining",String.valueOf(probe.getRemainingTokens()));
        filterChain.doFilter(request,response);
        }else{
            var waitForRefill=probe.getNanosToWaitForRefill()/1_000_000_000;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After-Seconds",String.valueOf(waitForRefill));
            response.setContentType("applicatio/json");

            String jsonResponse= """
                    {
                    "status":%s,
                    "error":"too many requests",
                    "message":"You have exhausted your api quota",
                    "retryAfterSeconds":%s
                    }
                    """.formatted(HttpStatus.TOO_MANY_REQUESTS.value(),waitForRefill);
            response.getWriter().write(jsonResponse);
        }
    }
    private String getClientIp(HttpServletRequest request){
        String xFHeader= request.getHeader("X-Forwarded-For");
        if(xFHeader==null||xFHeader.isEmpty()){
            return request.getRemoteAddr();
        }

        return xFHeader.split(",")[0].trim();
    }
}
