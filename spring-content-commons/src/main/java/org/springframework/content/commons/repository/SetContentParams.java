package org.springframework.content.commons.repository;

import lombok.Builder;
import lombok.Data;

@Deprecated
@Data
@Builder
public class SetContentParams {
    private long contentLength = -1;
    @Builder.Default
    private boolean overwriteExistingContent = true;
}
