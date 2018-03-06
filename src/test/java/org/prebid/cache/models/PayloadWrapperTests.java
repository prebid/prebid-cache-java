package org.prebid.cache.models;

import org.prebid.cache.exceptions.ExpiryOutOfRangeException;
import org.prebid.cache.helpers.Json;
import org.prebid.cache.model.PayloadWrapper;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private static String jsonWithExpiryOutOfRangeMin = "{\n" +
            "  \"id\": \"2be04ba5-8f9b-4a1e-8100-d573c40312f8\",\n" +
            "  \"prefix\": \"prebid_\",\n" +
            "  \"payload\": {\n" +
            "    \"type\": \"json\",\n" +
            "    \"value\": \"{\\r\\n  \\\"creativeCode\\\" : \\\"<html></html>\\\"\\r\\n}\",\n" +
            "    \"key\": \"2be04ba5-8f9b-4a1e-8100-d573c40312f8\"\n" +
            "  },\n" +
            "  \"expiry\": 299,\n" +
            "  \"lastModified\": \"Dec 9, 2017 11:24:44 AM\",\n" +
            "  \"success\": true\n" +
            "}";

    private static String jsonWithExpiryOutOfRangeMax = "{\n" +
            "  \"id\": \"2be04ba5-8f9b-4a1e-8100-d573c40312f8\",\n" +
            "  \"prefix\": \"prebid_\",\n" +
            "  \"payload\": {\n" +
            "    \"type\": \"json\",\n" +
            "    \"value\": \"{\\r\\n  \\\"creativeCode\\\" : \\\"<html></html>\\\"\\r\\n}\",\n" +
            "    \"key\": \"2be04ba5-8f9b-4a1e-8100-d573c40312f8\"\n" +
            "  },\n" +
            "  \"expiry\": 28801,\n" +
            "  \"lastModified\": \"Dec 9, 2017 11:24:44 AM\",\n" +
            "  \"success\": true\n" +
            "}";

    @Test
    void testExpiry() {
        val wrapper = Json.createPayloadFromJson(jsonWithExpiry, PayloadWrapper.class);
        assertEquals(1800L, wrapper.getExpiry().longValue());
    }

    @Test
    void testOutOfRangeMinExpiry() {
        val wrapper = Json.createPayloadFromJson(jsonWithExpiryOutOfRangeMin, PayloadWrapper.class);
        assertThrows(ExpiryOutOfRangeException.class, wrapper::getExpiry);
    }

    @Test
    void testOutOfRangeMaxExpiry() {
        val wrapper = Json.createPayloadFromJson(jsonWithExpiryOutOfRangeMax, PayloadWrapper.class);
        assertThrows(ExpiryOutOfRangeException.class, wrapper::getExpiry);
    }

}
