# Metrics

Prebid Cache Java currently supports the following metric reporter through dropwizard

- `graphite`
- `console`

## Graphite

Configure graphite in your `application.yml`

```yaml
metrics:
  graphite:
    enabled: true
    host: localhost
    port: 1234
    interval: 60
```

## Console

This logs metrics directly to stdout and is useful for debugging.

```yaml
metrics:
  graphite:
    enabled: false
  console:
    enabled: true
    interval: 60
```
