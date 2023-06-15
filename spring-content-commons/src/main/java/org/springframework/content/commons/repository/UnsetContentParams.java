package org.springframework.content.commons.repository;

import lombok.Builder;
import lombok.Data;

@Deprecated
@Data
@Builder
public class UnsetContentParams {

    @Builder.Default
    private Disposition disposition = Disposition.Remove;

    public enum Disposition {
        Keep, Remove
    }
}
