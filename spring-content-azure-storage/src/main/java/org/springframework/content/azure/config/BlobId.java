package org.springframework.content.azure.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BlobId {

    private String bucket;
    private String name;

}
