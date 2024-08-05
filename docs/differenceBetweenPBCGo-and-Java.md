# Differences Between Prebid Cache Go and Java

October 25, 2018

The sister Prebid Cache projects are both busy and moving forward at different paces on different features. Sometimes a feature may exist in one implementation
and not the other for an interim period. This page tracks known differences that may persist for longer than a couple of weeks.

## Differences

1) PBC-Java has different protocol with PBC-Go for populating cache in storage:

##### PBC-Java request example:

```json
{
  "puts": [
    {
      "type": "json",
      "value": {
        "adm": "JSON value of any type can go here."
      },
      "expiry" : 800
    },
    {
    "type" : "xml", 
    "value":"Your XML content goes here."}
  ]
}
```

##### PBC-Go request example:

```json
{
  "puts": [
    {
      "type": "xml",
      "ttlseconds": 60,
      "value": "<tag>Your XML content goes here.</tag>"
    },
    {
      "type": "json",
      "ttlseconds": 300,
      "value": [1, true, "JSON value of any type can go here."]
    }
  ]
}
```
- PBC-Java for `ttl` option use field with name `expiry`;
- PBC-Go for `ttl` option use field with name `ttlseconds`;
- PBC-Java for `JSON` value uses object, that contain `adm` field that stores JSON content;
- PBC-Go for `JSON` value uses an array with JSON content and additional parameters;

2) PBC-Java has different implementations of cache storage with PBC-Go:
- PBC-Java has ability to store data in `Aerospike`, `Redis`, `Apache Ignite`;
- PBC-Go has ability to store data in `Cassandra` , `Memcache`, `Aerospike`, `Apache Ignite`;
