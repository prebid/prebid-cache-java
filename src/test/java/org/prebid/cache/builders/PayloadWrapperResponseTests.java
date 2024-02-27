package org.prebid.cache.builders;

import org.junit.jupiter.api.BeforeAll;
import org.prebid.cache.model.Payload;
import org.prebid.cache.model.PayloadWrapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.http.MediaType.APPLICATION_XML;

@SpringBootTest
public abstract class PayloadWrapperResponseTests {

    static final String XML_RESPONSE = """
            <?xml version="1.0"?>
            <xml>
                <creativeCode>
                    <![CDATA[      <html>
                </html>      ]]>
            </creativeCode>
            </xml>
            """;
    static final String JSON_RESPONSE = """
            {
              "creativeCode" : "<html></html>"
            }""";
    static PayloadWrapper jsonPayloadWrapper;
    static PayloadWrapper jsonUTF8PayloadWrapper;
    static PayloadWrapper xmlPayloadWrapper;

    @BeforeAll
    static void init() {
        jsonPayloadWrapper = createJsonPayloadWrapper();
        jsonUTF8PayloadWrapper = createJsonUTF8PayloadWrapper();
        xmlPayloadWrapper = createXmlPayloadWrapper();
    }

    static boolean isXml(MediaType mediaType) {
        return mediaType.equals(APPLICATION_XML);
    }
    static boolean isJson(MediaType mediaType) {
        return mediaType.equals(APPLICATION_JSON);
    }
    static boolean isJsonUTF8(MediaType mediaType) {
        return mediaType.equals(APPLICATION_JSON_UTF8);
    }

    private static PayloadWrapper createJsonPayloadWrapper() {
        return createPayloadWrapper(APPLICATION_JSON);
    }
    private static PayloadWrapper createJsonUTF8PayloadWrapper() {
        return createPayloadWrapper(APPLICATION_JSON_UTF8);
    }
    private static PayloadWrapper createXmlPayloadWrapper() {
        return createPayloadWrapper(APPLICATION_XML);
    }

    private static PayloadWrapper createPayloadWrapper(MediaType mediaType) {
        String payloadValue = null;
        if (isJson(mediaType) || isJsonUTF8(mediaType)) {
            payloadValue = JSON_RESPONSE;
        } else if (isXml(mediaType)) {
            payloadValue = XML_RESPONSE;
        }

        final var payload = Payload.of("json", "1234567890", payloadValue);
        return PayloadWrapper.builder()
                .id("")
                .prefix("prefix")
                .payload(payload)
                .expiry(200L)
                .lastModified(new Date())
                .isExternalId(false)
                .build();
    }
}
