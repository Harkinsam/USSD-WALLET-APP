package com.skaet.ussd.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UssdSessionService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final long SESSION_TIMEOUT = 300; // 5 minutes in seconds

    public UssdSession getSession(String sessionId, String phoneNumber) {
        try {
            UssdSession session = (UssdSession) redisTemplate.opsForValue().get(sessionId);

            log.info("Existing session retrieved: {}", sessionId);

            return session;
        } catch (Exception e) {
            log.error("Error handling session {}: {}", sessionId, e.getMessage(), e);
            return createNewSession(sessionId, phoneNumber);
        }
    }

    private UssdSession createNewSession(String sessionId, String phoneNumber) {
        UssdSession session = new UssdSession();
        session.setSessionId(sessionId);
        session.setPhoneNumber(phoneNumber);
        session.setData(new HashMap<>());
        return session;
    }

    public void saveSession(UssdSession session) {
        try {
            if (session != null && session.getSessionId() != null) {
                redisTemplate.opsForValue().set(
                        session.getSessionId(),
                        session,
                        SESSION_TIMEOUT,
                        TimeUnit.SECONDS
                );
                log.debug("Session saved: {}", session.getSessionId());
            } else {
                log.warn("Attempted to save a null session or session ID.");
            }
        } catch (Exception e) {
            log.error("Error saving session {}: {}",
                    (session != null ? session.getSessionId() : "NULL"), e.getMessage(), e);
        }
    }

    public void clearSession(String sessionId) {
        try {
            if (sessionId != null) {
                redisTemplate.delete(sessionId);
                log.debug("Session cleared: {}", sessionId);
            } else {
                log.warn("Attempted to clear a null session ID.");
            }
        } catch (Exception e) {
            log.error("Error clearing session {}: {}", sessionId, e.getMessage(), e);
        }
    }
}

