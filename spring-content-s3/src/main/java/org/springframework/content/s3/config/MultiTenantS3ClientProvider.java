package org.springframework.content.s3.config;

import software.amazon.awssdk.services.s3.S3Client;

/**
 * When configured to do so, the S3Store will provide the AmazonS3 client object returned by this function to any
 * `Resource`s that it is asked to load.
 */
public interface MultiTenantS3ClientProvider {

    /**
     * The S3Client client to use, or null
     *
     * @return the S3Client client to use, or null
     */
    S3Client getS3Client();
}
