# Configuration

Configuration is handled by [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html),
which supports properties files, YAML files, environment variables and command-line arguments for setting config values.

As a general rule, Prebid Cache will immediately fails on startup if any of required properties is missing or invalid.

The next sections describes how to set up project configuration.

## Application properties

### API
- `api.path` - (Deprecated) set path for cache endpoint.
- `api.cache_path` - set path for cache endpoint.
- `api.module_storage_path` - set path for storage endpoint.
- `api.api_key` - set API key.

#### Cors
- `cors.enabled` - toggle for cors.
- `cors.mapping` - set path pattern for cors.
- `cors.allowed_origin_patterns` - set allowed origin patterns.
- `cors.allowed_methods` - set allowed methods.
- `cors.allow_credentials` - sets `Access-Control-Allow-Credentials` response header value (`true` or `false`).  

### Cache 
- `cache.prefix` - set prefix for all saved cache entries.
- `cache.expiry_sec` - set default expiration time (in seconds) for cache entries.
- `cache.min_expiry` - set minimum expiration time (in seconds) for cache entries.
- `cache.max_expiry` - set maximum expiration time (in seconds) for cache entries.
- `cache.timeout_ms` - set timeout for persistence provider.
- `cache.allow_external_UUID` - toggle for accepting externally provided UUID. If set to `false`, error will be returned on external UUID. When set to `true` externally provided UUID will be accepted. 
- `cache.secondary_uris` - uris of secondary caches.
- `cache.secondary_cache_path` - path of secondary cache.
- `cache.clients_cache_duration` - expiration time (in seconds) for internal web clients cache.
- `cache.clients_cache_size` - maximum amount of cached web clients.
- `cache.allowed_proxy_host` - set the allowed proxy host for request with `ch` parameter.
- `cache.host_param_protocol` - set protocol for secondary cache requests.
- `circuitbreaker.failure_rate_threshold` - failure rate threshold for circuit breaker.
- `circuitbreaker.open_state_duration` - duration (in millis) of circuit breaker sitting in open state.
- `circuitbreaker.closed_state_calls_number` - size of circuit breaker sliding window.
- `circuitbreaker.half_open_state_calls_number` - number of calls in half open state.

### Storage
- `storage.default-ttl-seconds` - set the default ttl for the data

#### Redis
- `storage.redis.{application-name}.port` - redis port.
- `storage.redis.{application-name}.host` - redis host.
- `storage.redis.{application-name}.password` - redis password, leave empty if no password required.
- `storage.redis.{application-name}.cluster.nodes` - list of node uris, set when using clustered redis.
- `storage.redis.{application-name}.cluster.enable_topology_refresh` - toggle for topology refresh support, set when using clustered redis.
- `storage.redis.{application-name}.cluster.topology_periodic_refresh_period` - refresh period of clustered redis topology, used when `storage.redis.{application-name}.cluster.enable_topology_refresh` is set to true.
