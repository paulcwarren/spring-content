package org.springframework.content.commons.repository;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SetContentParams {
    private long contentLength = -1;
    @Builder.Default
    private boolean overwriteExistingContent = true;
}
