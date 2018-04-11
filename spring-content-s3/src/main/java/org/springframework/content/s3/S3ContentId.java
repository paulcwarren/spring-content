package org.springframework.content.s3;

import org.springframework.util.Assert;

import java.io.Serializable;

public final class S3ContentId implements Serializable {
    private String bucket;
    private String objectId;

    public S3ContentId(String bucket, String objectId) {
        Assert.hasText(bucket, "bucket must be specified");
        Assert.hasText(objectId, "objectId must be specified");
        this.bucket = bucket;
        this.objectId = objectId;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    @Override
    public String toString() {
        return "S3ContentId{" +
                "bucket='" + bucket + '\'' +
                ", objectId='" + objectId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        S3ContentId that = (S3ContentId) o;

        if (bucket != null ? !bucket.equals(that.bucket) : that.bucket != null) return false;
        return objectId != null ? objectId.equals(that.objectId) : that.objectId == null;
    }

    @Override
    public int hashCode() {
        int result = bucket != null ? bucket.hashCode() : 0;
        result = 31 * result + (objectId != null ? objectId.hashCode() : 0);
        return result;
    }
}
