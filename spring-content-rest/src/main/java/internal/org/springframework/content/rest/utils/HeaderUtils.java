package internal.org.springframework.content.rest.utils;

import static java.lang.String.format;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

public class HeaderUtils {

    /**
     * Pattern matching ETag multiple field values in headers such as "If-Match", "If-None-Match".
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">Section 2.3 of RFC 7232</a>
     */
    private static final Pattern ETAG_HEADER_VALUE_PATTERN = Pattern.compile("\\*|\\s*((W\\/)?(\"[^\"]*\"))\\s*,?");

    /**
     * Date formats as specified in the HTTP RFC.
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
     */
    private static final String[] DATE_FORMATS = new String[] {
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd-MMM-yy HH:mm:ss zzz",
            "EEE MMM dd HH:mm:ss yyyy"
    };

    private static final ConfigurableConversionService conversionService = new DefaultConversionService();

    {
        Jsr310Converters.getConvertersToRegister().forEach(conversionService::addConverter);
    }

    public static void evaluateHeaderConditions(HttpHeaders headers, String resourceETag, Object resourceLastModified) {
		if (ifMatchPresent(headers)) {
			boolean matches = checkIfMatchCondition(headers, resourceETag);
			if (!matches) {
				throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, format("Entity If-Match %s failed", headers.getIfMatch().get(0)));
			}
		} else {
			if (isIfUnmodifiedSincePresent(headers)) {
				if (resourceLastModified != null) {
					Long lastModified = Stream.of(resourceLastModified)
							.filter(it -> it != null)
							.map(it -> conversionService.convert(it, Date.class))//
							.map(it -> conversionService.convert(it, Instant.class))//
							.map(it -> it.toEpochMilli())
							.findFirst().orElse(-1L);
					boolean unmodified = checkIfUnmodifiedSinceCondition(headers, lastModified);
					if (!unmodified) {
						throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, format("Entity modified since %s", headers.get("If-Unmodified-Since").get(0)));
					}
				}
			}
		}

		boolean noneMatch = checkIfNoneMatchCondition(headers, resourceETag);
		if (!noneMatch) {
			throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, format("Entity If-None-Match %s failed", StringUtils.collectionToCommaDelimitedString(headers.get("If-None-Match"))));
		}
	}

    public static boolean ifMatchPresent(HttpHeaders headers) {
        return headers.getIfMatch().size() > 0;
    }

    public static  boolean checkIfMatchCondition(HttpHeaders headers, String resourceETag) {
        if (resourceETag == null) {
            return true;
        }

        Iterator<String> ifMatch = headers.getIfMatch().iterator();
        boolean matches = !ifMatch.hasNext();

        if (StringUtils.hasText(resourceETag)) {
            resourceETag = padEtagIfNecessary(resourceETag);
            while (ifMatch.hasNext()) {
                String clientETags = ifMatch.next();
                Matcher etagMatcher = ETAG_HEADER_VALUE_PATTERN.matcher(clientETags);
                // Compare weak/strong ETags as per https://tools.ietf.org/html/rfc7232#section-2.3
                while (etagMatcher.find()) {
                    if (StringUtils.hasLength(etagMatcher.group()) &&
                            resourceETag.replaceFirst("^W/", "").equals(etagMatcher.group(3))) {
                        matches = true;
                    }
                }
            }
        }
        return matches;
    }

    public static  boolean checkIfNoneMatchCondition(HttpHeaders headers, String resourceETag) {
        boolean matches = false;
        Iterator<String> ifNoneMatch = headers.getIfNoneMatch().iterator();

        if (StringUtils.hasText(resourceETag)) {
            resourceETag = padEtagIfNecessary(resourceETag);
            while (ifNoneMatch.hasNext()) {
                String clientETags = ifNoneMatch.next();
                Matcher etagMatcher = ETAG_HEADER_VALUE_PATTERN.matcher(clientETags);
                // Compare weak/strong ETags as per https://tools.ietf.org/html/rfc7232#section-2.3
                while (etagMatcher.find()) {
                    if (StringUtils.hasLength(etagMatcher.group()) &&
                            resourceETag.replaceFirst("^W/", "").equals(etagMatcher.group(3))) {
                        matches = true;
                    }
                }
            }
        }
        return !matches;
    }

    public static  boolean isIfUnmodifiedSincePresent(HttpHeaders headers) {
        return headers.getIfUnmodifiedSince() != -1;
    }

    public static  boolean checkIfUnmodifiedSinceCondition(HttpHeaders headers, long lastModifiedTimestamp) {
        if (lastModifiedTimestamp < 0) {
            return false;
        }
        long ifUnmodifiedSince = parseDateHeader(headers, "If-Unmodified-Since");
        if (ifUnmodifiedSince == -1) {
            return false;
        }
        // We will perform this validation...
        return !(ifUnmodifiedSince < (lastModifiedTimestamp / 1000 * 1000));
    }

    public static  String padEtagIfNecessary(String etag) {
        if (!StringUtils.hasLength(etag)) {
            return etag;
        }
        if ((etag.startsWith("\"") || etag.startsWith("W/\"")) && etag.endsWith("\"")) {
            return etag;
        }
        return "\"" + etag + "\"";
    }

    public static  long parseDateHeader(HttpHeaders headers, String headerName) {
        long dateValue = -1;
        try {
            dateValue = headers.getIfUnmodifiedSince();
        }
        catch (IllegalArgumentException ex) {
            String headerValue = headers.getFirst(headerName);
            // Possibly an IE 10 style value: "Wed, 09 Apr 2014 09:57:42 GMT; length=13774"
            if (headerValue != null) {
                int separatorIndex = headerValue.indexOf(';');
                if (separatorIndex != -1) {
                    String datePart = headerValue.substring(0, separatorIndex);
                    dateValue = parseDateValue(datePart);
                }
            }
        }
        return dateValue;
    }

    public static  long parseDateValue(@Nullable String headerValue) {
        if (headerValue == null) {
            // No header value sent at all
            return -1;
        }
        if (headerValue.length() >= 3) {
            // Short "0" or "-1" like values are never valid HTTP date headers...
            // Let's only bother with SimpleDateFormat parsing for long enough values.
            for (String dateFormat : DATE_FORMATS) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                try {
                    return simpleDateFormat.parse(headerValue).getTime();
                }
                catch (ParseException ex) {
                    // ignore
                }
            }
        }
        return -1;
    }
}
