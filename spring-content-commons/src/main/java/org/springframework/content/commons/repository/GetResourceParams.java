package org.springframework.content.commons.repository;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetResourceParams {

    private String range;
}
