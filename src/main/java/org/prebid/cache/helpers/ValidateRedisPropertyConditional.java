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
        final boolean password = contextEnvironment.containsProperty("spring.redis.password");
        final boolean port = contextEnvironment.containsProperty("spring.redis.port");
        final boolean cluster = contextEnvironment.containsProperty("spring.redis.cluster");
        final boolean clusterNodes = contextEnvironment.containsProperty("spring.redis.cluster.nodes");

        if (timeOut & host & port & password & cluster) {
            return false;
        } else if (timeOut & host & port & password) {
            return true;
        } else return timeOut & cluster & clusterNodes;
    }
}
