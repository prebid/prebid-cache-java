package org.prebid.cache.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.prebid.cache.builders.PrebidServerResponseBuilder;
import org.prebid.cache.exceptions.BadRequestException;
import org.prebid.cache.exceptions.DuplicateKeyException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.exceptions.RequestParsingException;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.prebid.cache.handlers.cache.CacheHandler;
import org.prebid.cache.metrics.MetricsRecorder;
import org.prebid.cache.metrics.MetricsRecorderTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MetricsRecorderTest.class})
@EnableConfigurationProperties
@SpringBootTest
public class CacheHandlerTest {

    @MockBean
    private PrebidServerResponseBuilder builder;

    @SpyBean
    private MetricsRecorder metricsRecorder;

    private CacheHandler target;

    @BeforeEach
    public void setUp() {
        target = new CacheHandler(builder, metricsRecorder, 0);
    }

    @Test
    public void timerContextShouldReturnExpectedTimerContext() {
        // when
        target.timerContext(ServiceType.FETCH, "prefix");

        // then
        verify(metricsRecorder).markMeterForTag(eq("prefix"), eq(MetricsRecorder.MeasurementTag.REQUEST));
        verify(metricsRecorder).createRequestTimerForServiceType(eq(ServiceType.FETCH));
    }

    @Test
    public void validateErrorResultShouldReturnBadRequestExceptionOnDuplicateKeyException() {
        // given
        final Mono<Void> error = Mono.error(new DuplicateKeyException("error"));

        // when
        final Mono<Void> result = target.validateErrorResult(ServiceType.FETCH, error);

        // then
        StepVerifier.create(result)
                .expectError(BadRequestException.class)
                .verify();
        verify(metricsRecorder).getExistingKeyError();
    }

    @Test
    public void validateErrorResultShouldReturnRequestParsingExceptionOnDecodingException() {
        // given
        final Mono<Void> error = Mono.error(new DecodingException("error"));

        // when
        final Mono<Void> result = target.validateErrorResult(ServiceType.FETCH, error);

        // then
        StepVerifier.create(result)
                .expectError(RequestParsingException.class)
                .verify();
    }

    @Test
    public void validateErrorResultShouldReturnUnsupportedMediaTypeOnUnsupportedMediaTypeStatus() {
        // given
        final Mono<Void> error = Mono.error(new UnsupportedMediaTypeStatusException("error"));

        // when
        final Mono<Void> result = target.validateErrorResult(ServiceType.FETCH, error);

        // then
        StepVerifier.create(result)
                .expectError(UnsupportedMediaTypeException.class)
                .verify();
    }

    @Test
    public void finalizeResultShouldStopTimer() {
        // given
        final ServerResponse serverResponse = mock(ServerResponse.class);
        final Mono<ServerResponse> mono = Mono.just(serverResponse);
        final MetricsRecorder.MetricsRecorderTimer timerContext = mock(MetricsRecorder.MetricsRecorderTimer.class);

        // when
        final Mono<ServerResponse> result = target.finalizeResult(mono, null, timerContext, null);

        // then
        StepVerifier.create(result)
                .assertNext(emptyConsumer())
                .expectComplete()
                .verify();
        verify(timerContext).stop();
    }

    @Test
    public void finalizeResultShouldHandleRepositoryException() {
        // given
        final ServerResponse serverResponse = mock(ServerResponse.class);
        given(builder.error(any(), any())).willReturn(Mono.just(serverResponse));

        final Mono<ServerResponse> mono = Mono.error(new RepositoryException("error"));
        final MetricsRecorder.MetricsRecorderTimer timerContext = mock(MetricsRecorder.MetricsRecorderTimer.class);

        // when
        final Mono<ServerResponse> result = target.finalizeResult(mono, null, timerContext, "prefix");

        // then
        StepVerifier.create(result)
                .assertNext(emptyConsumer())
                .expectComplete()
                .verify();
        verify(metricsRecorder).markMeterForTag(eq("prefix"), eq(MetricsRecorder.MeasurementTag.ERROR_DB));
    }

    @Test
    public void finalizeResultShouldHandleTimeoutException() {
        // given
        final ServerResponse serverResponse = mock(ServerResponse.class);
        given(builder.error(any(), any())).willReturn(Mono.just(serverResponse));

        final Mono<ServerResponse> mono = Mono.error(new TimeoutException("error"));
        final MetricsRecorder.MetricsRecorderTimer timerContext = mock(MetricsRecorder.MetricsRecorderTimer.class);

        // when
        final Mono<ServerResponse> result = target.finalizeResult(mono, null, timerContext, "prefix");

        // then
        StepVerifier.create(result)
                .assertNext(emptyConsumer())
                .expectComplete()
                .verify();
        verify(metricsRecorder).markMeterForTag(eq("prefix"), eq(MetricsRecorder.MeasurementTag.ERROR_TIMEDOUT));
    }

    @Test
    public void finalizeResultShouldHandleInternalServerErrorStatusCode() {
        // given
        final ServerResponse serverResponse = mock(ServerResponse.class);
        given(serverResponse.statusCode()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        given(builder.error(any(), any())).willReturn(Mono.just(serverResponse));

        final Mono<ServerResponse> mono = Mono.error(new BadRequestException("error"));
        final MetricsRecorder.MetricsRecorderTimer timerContext = mock(MetricsRecorder.MetricsRecorderTimer.class);

        // when
        final Mono<ServerResponse> result = target.finalizeResult(mono, null, timerContext, "prefix");

        // then
        StepVerifier.create(result)
                .assertNext(emptyConsumer())
                .expectComplete()
                .verify();
        verify(metricsRecorder).markMeterForTag(eq("prefix"), eq(MetricsRecorder.MeasurementTag.ERROR_UNKNOWN));
    }

    @Test
    public void finalizeResultShouldHandleBadRequestStatusCode() {
        // given
        final ServerResponse serverResponse = mock(ServerResponse.class);
        given(serverResponse.statusCode()).willReturn(HttpStatus.BAD_REQUEST);
        given(builder.error(any(), any())).willReturn(Mono.just(serverResponse));

        final Mono<ServerResponse> mono = Mono.error(new BadRequestException("error"));
        final MetricsRecorder.MetricsRecorderTimer timerContext = mock(MetricsRecorder.MetricsRecorderTimer.class);

        // when
        final Mono<ServerResponse> result = target.finalizeResult(mono, null, timerContext, "prefix");

        // then
        StepVerifier.create(result)
                .assertNext(emptyConsumer())
                .expectComplete()
                .verify();
        verify(metricsRecorder).markMeterForTag(eq("prefix"), eq(MetricsRecorder.MeasurementTag.ERROR_BAD_REQUEST));
    }

    @Test
    public void finalizeResultShouldHandleNotFoundStatusCode() {
        // given
        final ServerResponse serverResponse = mock(ServerResponse.class);
        given(serverResponse.statusCode()).willReturn(HttpStatus.NOT_FOUND);
        given(builder.error(any(), any())).willReturn(Mono.just(serverResponse));

        final Mono<ServerResponse> mono = Mono.error(new BadRequestException("error"));
        final MetricsRecorder.MetricsRecorderTimer timerContext = mock(MetricsRecorder.MetricsRecorderTimer.class);

        // when
        final Mono<ServerResponse> result = target.finalizeResult(mono, null, timerContext, "prefix");

        // then
        StepVerifier.create(result)
                .assertNext(emptyConsumer())
                .expectComplete()
                .verify();
        verify(metricsRecorder).markMeterForTag(eq("prefix"), eq(MetricsRecorder.MeasurementTag.ERROR_MISSINGID));
    }

    private static <T> Consumer<T> emptyConsumer() {
        return t -> {
        };
    }
}
