package internal.org.springframework.content.s3.io;

import org.springframework.util.Assert;

/**
 * Utility class that provides utility method to work with s3 storage resources.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
final class SimpleStorageNameUtils {

    private static final String S3_PROTOCOL_PREFIX = "s3://";

    private static final String PATH_DELIMITER = "/";

    private static final String VERSION_DELIMITER = "^";

    private SimpleStorageNameUtils() {
        // Avoid instantiation
    }

    static boolean isSimpleStorageResource(String location) {
        Assert.notNull(location, "Location must not be null");
        return location.toLowerCase().startsWith(S3_PROTOCOL_PREFIX);
    }

    static String getBucketNameFromLocation(String location) {
        Assert.notNull(location, "Location must not be null");
        if (!isSimpleStorageResource(location)) {
            throw new IllegalArgumentException(
                    "The location :'" + location + "' is not a valid S3 location");
        }
        int bucketEndIndex = location.indexOf(PATH_DELIMITER,
                S3_PROTOCOL_PREFIX.length());
        if (bucketEndIndex == -1 || bucketEndIndex == S3_PROTOCOL_PREFIX.length()) {
            throw new IllegalArgumentException("The location :'" + location
                    + "' does not contain a valid bucket name");
        }
        return location.substring(S3_PROTOCOL_PREFIX.length(), bucketEndIndex);
    }

    static String getObjectNameFromLocation(String location) {
        Assert.notNull(location, "Location must not be null");
        if (!isSimpleStorageResource(location)) {
            throw new IllegalArgumentException(
                    "The location :'" + location + "' is not a valid S3 location");
        }
        int bucketEndIndex = location.indexOf(PATH_DELIMITER,
                S3_PROTOCOL_PREFIX.length());
        if (bucketEndIndex == -1 || bucketEndIndex == S3_PROTOCOL_PREFIX.length()) {
            throw new IllegalArgumentException("The location :'" + location
                    + "' does not contain a valid bucket name");
        }

        if (location.contains(VERSION_DELIMITER)) {
            return getObjectNameFromLocation(
                    location.substring(0, location.indexOf(VERSION_DELIMITER)));
        }

        int endIndex = location.length();
        if (location.endsWith(PATH_DELIMITER)) {
            endIndex--;
        }

        if (bucketEndIndex >= endIndex) {
            return "";
        }

        return location.substring(++bucketEndIndex, endIndex);
    }

    static String getVersionIdFromLocation(String location) {
        Assert.notNull(location, "Location must not be null");
        if (!isSimpleStorageResource(location)) {
            throw new IllegalArgumentException(
                    "The location :'" + location + "' is not a valid S3 location");
        }
        int objectNameEndIndex = location.indexOf(VERSION_DELIMITER,
                S3_PROTOCOL_PREFIX.length());
        if (objectNameEndIndex == -1 || location.endsWith(VERSION_DELIMITER)) {
            return null;
        }

        if (objectNameEndIndex == S3_PROTOCOL_PREFIX.length()) {
            throw new IllegalArgumentException("The location :'" + location
                    + "' does not contain a valid bucket name");
        }

        return location.substring(++objectNameEndIndex, location.length());
    }

    static String getLocationForBucketAndObject(String bucketName, String objectName) {
        Assert.notNull(bucketName, "Bucket name must not be null");
        Assert.notNull(objectName, "ObjectName name must not be null");
        StringBuilder location = new StringBuilder(S3_PROTOCOL_PREFIX.length()
                + bucketName.length() + PATH_DELIMITER.length() + objectName.length());
        location.append(S3_PROTOCOL_PREFIX);
        location.append(bucketName);
        location.append(PATH_DELIMITER);
        location.append(objectName);
        return location.toString();
    }

    static String getLocationForBucketAndObjectAndVersionId(String bucketName,
            String objectName, String versionId) {
        String location = getLocationForBucketAndObject(bucketName, objectName);
        return new StringBuffer(location).append(VERSION_DELIMITER).append(versionId)
                .toString();
    }

    static String stripProtocol(String location) {
        Assert.notNull(location, "Location must not be null");
        if (!isSimpleStorageResource(location)) {
            throw new IllegalArgumentException(
                    "The location :'" + location + "' is not a valid S3 location");
        }
        return location.substring(S3_PROTOCOL_PREFIX.length());
    }

}