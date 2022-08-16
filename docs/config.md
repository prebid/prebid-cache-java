# Configuration

Configuration is handled by [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html),
which supports properties files, YAML files, environment variables and command-line arguments for setting config values.

As a general rule, Prebid Cache will immediately fails on startup if any of required properties is missing or invalid.

The next sections describes how to set up project configuration.

## Application properties

### Server
- `cache.allowed_proxy_host` - set the allowed proxy host for request with `ch` parameter.


[//]: # (TODO: Add all server config proprties)
