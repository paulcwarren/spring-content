package org.springframework.content.commons.store;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnsetContentParams {
    @Builder.Default

    private Disposition disposition = Disposition.Remove;

    public enum Disposition {
        Keep, Remove
    }
}
