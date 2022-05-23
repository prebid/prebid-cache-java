package org.prebid.cache.models;

import lombok.Builder;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.prebid.cache.model.PayloadTransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PayloadTransferTest {


    @Test
    void testStringValue() {
        final var payloadTransfer = PayloadTransfer.builder()
                .expiry(100L)
                .key("2be04ba5-8f9b-4a1e-8100-d573c40312f8")
                .prefix("prebid_")
                .type("json")
                .ttlseconds(null)
                .value("string test value")
                .build();

        assertEquals("string test value", payloadTransfer.valueAsString());
    }

    @Test
    void testIntegerValue() {
        final var payloadTransfer = PayloadTransfer.builder()
                .expiry(100L)
                .key("2be04ba5-8f9b-4a1e-8100-d573c40312f8")
                .prefix("prebid_")
                .type("json")
                .ttlseconds(null)
                .value(1)
                .build();

        assertEquals("1", payloadTransfer.valueAsString());
    }

    @Test
    void testObjectValue() {
        final var expectedJsonString = "{\"adm\":\"test\",\"width\":200,\"height\":100}";

        final var payloadTransfer = PayloadTransfer.builder()
                .expiry(100L)
                .key("2be04ba5-8f9b-4a1e-8100-d573c40312f8")
                .prefix("prebid_")
                .type("json")
                .ttlseconds(null)
                .value(Value.builder().adm("test").height(100).width(200).build())
                .build();

        assertEquals(expectedJsonString, payloadTransfer.valueAsString());
    }

    @Test
    public void testExpiry() {
        final var payloadTransfer = PayloadTransfer.builder()
                .expiry(100L)
                .key("2be04ba5-8f9b-4a1e-8100-d573c40312f8")
                .prefix("prebid_")
                .type("json")
                .ttlseconds(null)
                .value(Value.builder().adm("test").height(100).width(200).build())
                .build();

        assertEquals(payloadTransfer.getExpiry(), payloadTransfer.compareAndGetExpiry());
    }

    @Test
    public void testTtlSeconds() {
        final var payloadTransfer = PayloadTransfer.builder()
                .expiry(100L)
                .key("2be04ba5-8f9b-4a1e-8100-d573c40312f8")
                .prefix("prebid_")
                .type("json")
                .ttlseconds(400L)
                .value(Value.builder().adm("test").height(100).width(200).build())
                .build();

        assertEquals(payloadTransfer.getTtlseconds(), payloadTransfer.compareAndGetExpiry());
    }

    @Builder
    private static class Value {
        private String adm;
        private int width;
        private int height;
    }

}
