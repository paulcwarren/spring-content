package org.springframework.content.s3.config;

import com.amazonaws.services.s3.AmazonS3;

/**
 * A callback, used by the S3 default store implementation when returning a resource, in order to establish which
 * AmazonS3 client to provide to the Resource being returned.
 */
public interface MultiTenantAmazonS3Provider {

    /**
     * The AmazonS3 client to use, or null
     *
     * @return the AmazonS3 client to use, or null
     */
    AmazonS3 getAmazonS3();
}
