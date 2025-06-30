# Configuration

Configuration is handled by [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html),
which supports properties files, YAML files, environment variables and command-line arguments for setting config values.

Both `_` and `-` can be used as a delimiters. As an example, both `api.cache_path` and `api.cache-path` refer to the same parameter.

As a general rule, Prebid Cache will immediately fail on startup if any of required properties is missing or invalid.

The next sections describes how to set up project configuration.

## Application properties

### API
- `api.path` - (Deprecated) set path for cache endpoint.
- `api.cache_path` - set path for cache endpoint.
- `api.module_storage_path` - set path for storage endpoint.
- `api.api_key` - set API key.
- `api.cache_write_secured` - if `true`, POST requests to `/cache` will require a valid API key.
- `api.external_UUID_secured` - if `true`, providing external UUID will additionally require a valid API key.

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
- `sampling.rate` - logging sampling rate

### Storage
- `storage.default-ttl-seconds` - set the default ttl for the data

#### Redis Module Storage
- `storage.redis.{application-name}.port` - redis port.
- `storage.redis.{application-name}.host` - redis host.
- `storage.redis.{application-name}.password` - redis password, leave empty if no password required.
- `storage.redis.{application-name}.timeout` - timeout in ms.
- `storage.redis.{application-name}.cluster.nodes` - list of node uris, set when using clustered redis.
- `storage.redis.{application-name}.cluster.enable_topology_refresh` - toggle for topology refresh support, set when using clustered redis.
- `storage.redis.{application-name}.cluster.topology_periodic_refresh_period` - refresh period of clustered redis topology, used when `storage.redis.{application-name}.cluster.enable_topology_refresh` is set to true.

#### Redis Storage
- `spring.redis.port` - redis port.
- `spring.redis.host` - redis host.
- `spring.redis.password` - redis password, leave empty if no password required.
- `spring.redis.timeout` - timeout in ms.
- `spring.redis.cluster.nodes` - list of node uris, set when using clustered redis.
- `spring.redis.cluster.enable_topology_refresh` - toggle for topology refresh support, set when using clustered redis.
- `spring.redis.cluster.topology_periodic_refresh_period` - refresh period of clustered redis topology, used when `spring.redis.cluster.enable_topology_refresh` is set to true.

#### Aerospike Storage
- `spring.aerospike.host` - a host or comma-separated hosts
- `spring.aerospike.port` - a port (in case the `spring.aerospike.host` has a single host)
- `spring.aerospike.password` - an aerospike password
- `spring.aerospike.cores` - the number of threads an aerospike event loop will use
- `spring.aerospike.first-backoff` - the minimum duration in ms for the first backoff
- `spring.aerospike.max-backoff` - a hard maximum duration for exponential backoffs.
- `spring.aerospike.max-retry` - the maximum number of retry attempts to allow
- `spring.aerospike.namespace` - an aerospike namespace
- `spring.aerospike.prevent-u-u-i-d-duplication"` - if equals to `true` doesn't allow to cache requests with the same UUID

#### Apache Ignite Storage
- `spring.ignite.host` - a host or comma-separated hosts
- `spring.ignite.port` - a port (in case the `spring.ignite.host` has a single host)
- `spring.ignite.cache-name` - an ignite cache name
- `spring.ignite.secure` - if equals to `true` requires SSL connection
