package com.multi.tenant.config;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

public class DefaultAuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        String userId = null;//SecurityUtils.getCurrentUserId();
        if (userId != null) {
            return Optional.of(userId);
        } else {
            return Optional.of("test");
        }
    }

}
