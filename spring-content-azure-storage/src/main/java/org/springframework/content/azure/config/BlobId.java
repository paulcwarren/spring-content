package org.springframework.content.azure.config;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BlobId implements Serializable {

    private static final long serialVersionUID = -8732799459908612678L;

    private String bucket;
    private String name;

}
