package org.prebid.cache.util;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.awaitility.Awaitility.await;

public class WiremockAwaitility {

    public static void awaitAndVerify(RequestPatternBuilder requestPatternBuilder, long timeoutMillis) {
        awaitAndVerify(1, requestPatternBuilder, timeoutMillis);
    }

    public static void awaitAndVerify(int count, RequestPatternBuilder requestPatternBuilder, long timeoutMillis) {
        await()
            .atMost(timeoutMillis, TimeUnit.MILLISECONDS)
            .until(() -> findAll(requestPatternBuilder).size() >= count);

        verify(requestPatternBuilder);
    }
}
