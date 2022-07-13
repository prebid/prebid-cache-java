package org.prebid.cache.repository.redis;

import lombok.Value;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class RedisConfigurationValidator implements Condition {

    private static final String CLUSTER_PREFIX = "spring.redis.cluster.";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        final Environment environment = context.getEnvironment();
        final ValidationResult clusterResult = validateClusterConfiguration(environment);
        final ValidationResult instanceResult = validateInstanceConfiguration(environment);

        if (clusterResult.isDefined() && instanceResult.isDefined()) {
            throw new IllegalArgumentException("Defining both instance and cluster redis is prohibited");
        } else if (clusterResult.isDefined() && !clusterResult.isValid()) {
            throw new IllegalArgumentException("Cluster redis configuration is invalid");
        } else if (instanceResult.isDefined() && !instanceResult.isValid()) {
            throw new IllegalArgumentException("Redis instance configuration is invalid");
        }

        return instanceResult.isDefined() || clusterResult.isDefined();
    }

    private static ValidationResult validateInstanceConfiguration(Environment environment) {
        final boolean timeOut = environment.containsProperty("spring.redis.timeout");
        final boolean host = environment.containsProperty("spring.redis.host");
        final boolean port = environment.containsProperty("spring.redis.port");

        final boolean instanceDefined = host || port;
        final boolean instanceValid = timeOut && host && port;

        return ValidationResult.of(instanceDefined, instanceValid);
    }

    private static ValidationResult validateClusterConfiguration(Environment environment) {
        final boolean timeOut = environment.containsProperty("spring.redis.timeout");

        final boolean clusterNodes = environment.containsProperty(CLUSTER_PREFIX + "nodes")
            || environment.containsProperty(CLUSTER_PREFIX + "nodes[0]");
        final boolean clusterRefresh = environment.containsProperty(CLUSTER_PREFIX + "enable-topology-refresh");
        final boolean clusterTopology = environment.containsProperty(CLUSTER_PREFIX
                + "topology-periodic-refresh-period");

        boolean refreshAbsent = !clusterRefresh && !clusterTopology;
        boolean refreshValid = clusterRefresh && clusterTopology;

        boolean clusterDefined = clusterNodes || clusterRefresh || clusterTopology;
        boolean clusterValid = timeOut && clusterNodes && (refreshValid || refreshAbsent);

        return ValidationResult.of(clusterDefined, clusterValid);
    }

    @Value(staticConstructor = "of")
    private static class ValidationResult {

        boolean defined;

        boolean valid;
    }
}
