package org.springframework.content.rest.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SetContentParams {

    @Builder.Default
    private ContentDisposition disposition = ContentDisposition.Overwrite;

    private enum ContentDisposition {
        Overwrite, CreateNew
    }
}
