package org.prebid.cache.models;

import org.junit.jupiter.api.Test;
import org.prebid.cache.exceptions.PayloadWrapperPropertyException;
import org.prebid.cache.helpers.Json;
import org.prebid.cache.model.PayloadWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayloadWrapperTests {

    private static final String JSON_WITH_EXPIRY = """
            {
              "id": "2be04ba5-8f9b-4a1e-8100-d573c40312f8",
              "prefix": "prebid_",
              "payload": {
                "type": "json",
                "value": "{\\r\\n  \\"creativeCode\\" : \\"<html></html>\\"\\r\\n}",
                "key": "2be04ba5-8f9b-4a1e-8100-d573c40312f8"
              },
              "expiry": 1800,
              "lastModified": "Dec 9, 2017 11:24:44 AM",
              "success": true
            }""";


    @Test
    void testExpiry() {
        final PayloadWrapper wrapper = Json.createPayloadFromJson(JSON_WITH_EXPIRY, PayloadWrapper.class);
        assertEquals(1800L, wrapper.getExpiry().longValue());
    }

    @Test
    void testGetNormalizedId() throws PayloadWrapperPropertyException {
        final PayloadWrapper wrapper = Json.createPayloadFromJson(JSON_WITH_EXPIRY, PayloadWrapper.class);
        assertEquals("prebid_2be04ba5-8f9b-4a1e-8100-d573c40312f8", wrapper.getNormalizedId());
    }

}
