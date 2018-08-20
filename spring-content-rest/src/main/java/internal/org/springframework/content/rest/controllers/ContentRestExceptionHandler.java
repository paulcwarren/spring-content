package internal.org.springframework.content.rest.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.rest.webmvc.support.ExceptionMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(basePackageClasses = ContentRestExceptionHandler.class)
public class ContentRestExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ContentRestExceptionHandler.class);

    /**
	 * Send a {@code 409 Conflict} in case of concurrent modification.
	 *
     * @param e the exception to handle.
	 * @return
     */
    @ExceptionHandler({ OptimisticLockingFailureException.class })
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
}
