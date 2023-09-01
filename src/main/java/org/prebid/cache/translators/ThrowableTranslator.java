package org.prebid.cache.translators;

import lombok.extern.slf4j.Slf4j;
import org.prebid.cache.exceptions.BadRequestException;
import org.prebid.cache.exceptions.ExpiryOutOfRangeException;
import org.prebid.cache.exceptions.InvalidUUIDException;
import org.prebid.cache.exceptions.RequestParsingException;
import org.prebid.cache.exceptions.ResourceNotFoundException;
import org.prebid.cache.exceptions.UnsupportedMediaTypeException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
public class ThrowableTranslator {
    private final HttpStatus httpStatus;
    private final String message;

    private ThrowableTranslator(final Throwable throwable) {
        this.httpStatus = getStatus(throwable);
        this.message = getMessage(throwable);
    }

    private HttpStatus getStatus(final Throwable error) {
        if (error instanceof ErrorResponse) {
            return Optional.of((ErrorResponse) error)
                .map(ErrorResponse::getStatusCode)
                .map(HttpStatusCode::value)
                .map(HttpStatus::resolve)
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (error instanceof BadRequestException
                || error instanceof RequestParsingException
                || error instanceof InvalidUUIDException
                || error instanceof ExpiryOutOfRangeException
                || error instanceof DataBufferLimitException) {
            return HttpStatus.BAD_REQUEST;
        } else if (error instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        } else if (error instanceof UnsupportedMediaTypeException) {
            return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    private String getMessage(final Throwable error) {
        if (error instanceof ErrorResponse) {
            return Optional.of((ErrorResponse) error)
                .map(ErrorResponse::getBody)
                .map(ProblemDetail::getDetail)
                .orElse(error.getMessage());
        }

        return error.getMessage();
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorMessage() {
        return message;
    }

    public static <T extends Throwable> Mono<ThrowableTranslator> translate(final Mono<T> throwable) {
        return throwable.map(ThrowableTranslator::new);
    }
}
