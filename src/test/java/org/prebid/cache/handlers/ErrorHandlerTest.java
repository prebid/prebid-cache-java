package org.prebid.cache.handlers;

import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.metrics.MetricsRecorder;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ErrorHandlerTest {

    @Mock(strictness = Mock.Strictness.LENIENT)
    private PrebidServerResponseBuilder builder;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private MetricsRecorder metricsRecorder;

    private ErrorHandler target;

    @BeforeEach
    public void setUp() {
        target = new ErrorHandler(metricsRecorder, builder);
    }

    @Test
    public void createResourceNotFoundShouldReturnExpectedError() {
        // when
        final Mono<ServerResponse> result = ErrorHandler.createResourceNotFound("uuid");

        // then
        StepVerifier.create(result).verifyErrorMessage("Resource Not Found: uuid uuid");
    }

    @Test
    public void createInvalidParametersShouldReturnExpectedError() {
        // when
        final Mono<ServerResponse> result = ErrorHandler.createInvalidParameters();

        // then
        StepVerifier.create(result).verifyErrorMessage("Invalid Parameter(s): uuid not found.");
    }

    @Test
    public void createNoElementsFoundShouldReturnExpectedError() {
        // when
        final Mono<ServerResponse> result = ErrorHandler.createNoElementsFound();

        // then
        StepVerifier.create(result).verifyErrorMessage("No Elements Found.");
    }

    @Test
    public void invalidRequestShouldReturnExpectedError() {
        // given
        final Counter counter = mock(Counter.class);
        given(metricsRecorder.getInvalidRequestMeter()).willReturn(counter);
        given(builder.error(any(), any())).willReturn(Mono.error(new Throwable("error")));

        final ServerRequest request = mock(ServerRequest.class);

        // when
        final Mono<ServerResponse> result = target.invalidRequest(request);

        // then
        StepVerifier.create(result).verifyErrorMessage("error");
        verify(counter).increment();
    }
}
