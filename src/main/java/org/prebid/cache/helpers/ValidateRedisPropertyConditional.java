package org.prebid.cache.helpers;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ValidateRedisPropertyConditional implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        final boolean isSingleNode = context.getEnvironment().containsProperty("spring.redis.single-node.timeout");
        final boolean isCluster = context.getEnvironment().containsProperty("spring.redis.cluster.timeout");

        return BooleanUtils.xor(new Boolean[]{isSingleNode, isCluster});
    }
}
