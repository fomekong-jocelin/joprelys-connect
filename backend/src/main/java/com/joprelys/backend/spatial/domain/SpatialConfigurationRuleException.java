package com.joprelys.backend.spatial.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SpatialConfigurationRuleException extends RuntimeException {

    public SpatialConfigurationRuleException(String message) {
        super(message);
    }
}
