package internal.org.springframework.content.azure.store;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BlobId {

    private String bucket;
    private String name;

}
