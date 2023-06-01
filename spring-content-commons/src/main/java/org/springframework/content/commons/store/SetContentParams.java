package org.springframework.content.commons.store;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SetContentParams {
    private long contentLength = -1;
    @Builder.Default
    private boolean overwriteExistingContent = true;

    @Builder.Default
    private ContentDisposition disposition = ContentDisposition.Overwrite;

    public enum ContentDisposition {
        Overwrite, CreateNew
    }
}
