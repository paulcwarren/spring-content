package org.springframework.content.s3;

import java.io.Serializable;

/**
 * Based on AWS SDK V1 S3ObjectId.
 *
 * Duplicated for backward compatibility.
 */
public class S3ObjectId implements Serializable {

    private static String DEFAULT_INSTRUCTION_FILE_SUFFIX = "instruction";
    private static String DOT = ".";

    private final String bucket;
    private final String key;
    /**
     * Optional and applicable only for get operation.
     */
    private final String versionId;

    public S3ObjectId(String bucket, String key) {
        this(bucket, key, null);
    }

    /**
     * @param bucket
     *            the S3 bucket name which must not be null
     * @param key
     *            the S3 key name which must not be null
     * @param versionId
     *            optional version id
     */
    public S3ObjectId(String bucket, String key, String versionId) {
        if (bucket == null || key == null)
            throw new IllegalArgumentException(
                    "bucket and key must be specified");
        this.bucket = bucket;
        this.key = key;
        this.versionId = versionId;
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

    /**
     * Returns the version id which is optionally applicable for S3 get (but not
     * put) operations.
     */
    public String getVersionId() {
        return versionId;
    }

    @Override
    public String toString() {
        return "bucket: " + bucket + ", key: " + key + ", versionId: "
                + versionId;
    }
}
