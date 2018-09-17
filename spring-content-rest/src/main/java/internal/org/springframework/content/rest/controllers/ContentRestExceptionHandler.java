package internal.org.springframework.content.rest.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.versions.LockOwnerException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.persistence.OptimisticLockException;
import javax.persistence.PessimisticLockException;

@ControllerAdvice(basePackageClasses = ContentRestExceptionHandler.class)
public class ContentRestExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ContentRestExceptionHandler.class);

    /**
	 * Send a {@code 409 Conflict} in case of concurrent modification.
	 *
     * @param e the exception to handle.
	 * @return
     */
    @ExceptionHandler({ LockOwnerException.class,
                        OptimisticLockException.class,
                        OptimisticLockingFailureException.class,
                        PessimisticLockException.class,
                        PessimisticLockingFailureException.class})
    ResponseEntity<ExceptionMessage> handleConflict(Exception e) {
        return errorResponse(HttpStatus.CONFLICT, new HttpHeaders(), e);
    }

    private static ResponseEntity<ExceptionMessage> errorResponse(HttpStatus status, HttpHeaders headers, Exception exception) {
        if (exception != null) {
            String message = exception.getMessage();
            logger.error(message, exception);

            if (StringUtils.hasText(message)) {
                return response(status, headers, new ExceptionMessage(exception));
            }
        }

        return response(status, headers, null);
    }

    private static <T> ResponseEntity<T> response(HttpStatus status, HttpHeaders headers, T body) {

        Assert.notNull(headers, "Headers must not be null!");
        Assert.notNull(status, "HttpStatus must not be null!");

        return new ResponseEntity<T>(body, headers, status);
    }

    public static  class ExceptionMessage {

        private final Throwable throwable;

        public ExceptionMessage(Throwable throwable) {
            this.throwable = throwable;
        }

        @JsonProperty("message")
        public String getMessage() {
            return throwable.getMessage();
        }

        @JsonProperty("cause")
        public ExceptionMessage getCause() {
            return throwable.getCause() != null ? new ContentRestExceptionHandler.ExceptionMessage(throwable.getCause()) : null;
        }
    }
}
