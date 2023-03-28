package org.springframework.content.commons.store;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetResourceParams {

    private String range;
}
