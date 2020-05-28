package org.springframework.content.s3.config;

import com.amazonaws.services.s3.AmazonS3;

/**
 * When configured to do so, the S3Store will provide the AmazonS3 client object returned by this function to any
 * `Resource`s that it is asked to load.
 */
public interface MultiTenantAmazonS3Provider {

    /**
     * The AmazonS3 client to use, or null
     *
     * @return the AmazonS3 client to use, or null
     */
    AmazonS3 getAmazonS3();
}
