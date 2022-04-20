package org.prebid.cache.helpers;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ValidateRedisPropertyConditional implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return context.getEnvironment().containsProperty("spring.redis.single-node.timeout")
                || context.getEnvironment().containsProperty("spring.redis.cluster.timeout");
    }
}
