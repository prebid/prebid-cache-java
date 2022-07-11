package org.prebid.cache.helpers;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ValidateRedisPropertyConditional implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        final Environment contextEnvironment = context.getEnvironment();
        final boolean timeOut = contextEnvironment.containsProperty("spring.redis.timeout");
        final boolean host = contextEnvironment.containsProperty("spring.redis.host");
        final boolean port = contextEnvironment.containsProperty("spring.redis.port");
        final boolean cluster = contextEnvironment.containsProperty("spring.redis.cluster");
        final boolean nodes = contextEnvironment.containsProperty("spring.redis.cluster.nodes");

        return timeOut & host & port || timeOut & cluster & nodes;
    }
}
