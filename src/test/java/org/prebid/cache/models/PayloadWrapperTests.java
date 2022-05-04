package org.prebid.cache.models;

import org.prebid.cache.exceptions.PayloadWrapperPropertyException;
import org.prebid.cache.helpers.Json;
import org.prebid.cache.model.PayloadWrapper;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("FieldCanBeLocal")
class PayloadWrapperTests {

    private static String jsonWithExpiry = "{\n" +
            "  \"id\": \"2be04ba5-8f9b-4a1e-8100-d573c40312f8\",\n" +
            "  \"prefix\": \"prebid_\",\n" +
            "  \"payload\": {\n" +
            "    \"type\": \"json\",\n" +
            "    \"value\": \"{\\r\\n  \\\"creativeCode\\\" : \\\"<html></html>\\\"\\r\\n}\",\n" +
            "    \"key\": \"2be04ba5-8f9b-4a1e-8100-d573c40312f8\"\n" +
            "  },\n" +
            "  \"expiry\": 1800,\n" +
            "  \"lastModified\": \"Dec 9, 2017 11:24:44 AM\",\n" +
            "  \"success\": true\n" +
            "}";


    @Test
    void testExpiry() {
        final var wrapper = Json.createPayloadFromJson(jsonWithExpiry, PayloadWrapper.class);
        assertEquals(1800L, wrapper.getExpiry().longValue());
    }

    @Test
    void testGetNormalizedId() throws PayloadWrapperPropertyException {
        final var wrapper = Json.createPayloadFromJson(jsonWithExpiry, PayloadWrapper.class);
        assertEquals("prebid_2be04ba5-8f9b-4a1e-8100-d573c40312f8", wrapper.getNormalizedId());
    }

}
