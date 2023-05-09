package org.springframework.content.commons.store;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SetContentParams {
    private long contentLength = -1;
    private boolean overwriteExistingContent;
}
