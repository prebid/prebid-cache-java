# Metrics

`{prefix}` can be: 
- `read` or `write` for the `api.cache-path` endpoints (it doesn't automatically mean that metric will appear in both)
- `module_storage.read` or `module_storage.write` for the `api.storage-path` endpoints (it doesn't automatically mean that metric will appear in both)

## The List of Metrics
- `pbc.{prefix}.request.duration` - the time it took to store or retrieve creative in ns.
- `pbc.{prefix}.request` - the count of incoming requests. 
- `pbc.{prefix}.trustedRequest` - the count of incoming requests carrying a matching API key.
- `pbc.request.invalid` - the count of incoming requests using invalid endpoint or method.
- `pbc.{prefix}.err.timedOut` - the count of timed out requests. Usually indicates an issue with the storage backend.
- `pbc.{prefix}.err.missingId` - the count of requests that carried a UUID that was not present in storage backend.
- `pbc.{prefix}.err.badRequest` - the count of malformed or otherwise invalid requests.
- `pbc.{prefix}.err.unauthorized` - the count of unauthorized requests.
- `pbc.{prefix}.err.unknown` - the count of unknown (not falling into the defined categories) errors.
- `pbc.{prefix}.err.db` - the count of storage backend errors.
- `pbc.{prefix}.json` - the count of JSON (banner) creatives.
- `pbc.{prefix}.xml` - the count of XML (video) creatives.
- `pbc.{prefix}.text` - the count of TEXT (module storage entries) creatives.
- `pbc.err.secondaryWrite` - the count of secondary write errors.
- `pbc.err.existingId` - the count of errors due to existing UUID key in the storage backend
- `pbc.${prefix}.err.duplicateId` - the count of errors due to existing UUID key in the storage backend (applicable only for `write` or `module_storage.write`)
- `pbc.err.rejectedExternalId` - the count of rejected writes due to specifying external UUID not being allowed.
- `pbc.proxy.success` - the count of successful proxying requests.
- `pbc.proxy.failure` - the count of failed proxying requests.
