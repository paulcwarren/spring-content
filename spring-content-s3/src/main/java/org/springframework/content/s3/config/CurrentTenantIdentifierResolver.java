package org.springframework.content.s3.config;


/**
 * A callback, used by the S3 default store implementation when returning a Resource, for identifying the current
 * tenant identifier.  Subsequently, used by the {@link MultiTenantAmazonS3Provider} to get a specific AmazonS3 client
 * to use for the Resource being returned.
 */
public interface CurrentTenantIdentifierResolver {

    /**
     * Return the current tenant identifier, or null if one cannot be established
     *
     * @return current tenant identifer, or null
     */
    String resolveCurrentTenantIdentifier();
}
