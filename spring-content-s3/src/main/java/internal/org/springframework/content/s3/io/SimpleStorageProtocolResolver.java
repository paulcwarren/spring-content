package internal.org.springframework.content.s3.io;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleStorageProtocolResolver implements ProtocolResolver, InitializingBean {

    private final S3Client amazonS3;

    /**
     * <b>IMPORTANT:</b> If a task executor is set with an unbounded queue there will be a
     * huge memory consumption. The reason is that each multipart of 5MB will be put in
     * the queue to be uploaded. Therefore a bounded queue is recommended.
     */
    private TaskExecutor taskExecutor;

    public SimpleStorageProtocolResolver(S3Client amazonS3) {
//        this.amazonS3 = AmazonS3ProxyFactory.createProxy(amazonS3);
        this.amazonS3 = amazonS3;
    }

    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.taskExecutor == null) {
            this.taskExecutor = new SyncTaskExecutor();
        }
    }

    @Override
    public Resource resolve(String location, ResourceLoader resourceLoader) {
        if (SimpleStorageNameUtils.isSimpleStorageResource(location)) {
            return new SimpleStorageResource(this.amazonS3,
                    SimpleStorageNameUtils.getBucketNameFromLocation(location),
                    SimpleStorageNameUtils.getObjectNameFromLocation(location),
                    this.taskExecutor,
                    SimpleStorageNameUtils.getVersionIdFromLocation(location));
        }
        else {
            return null;
        }
    }

    public S3Client getAmazonS3() {
        return this.amazonS3;
    }

}