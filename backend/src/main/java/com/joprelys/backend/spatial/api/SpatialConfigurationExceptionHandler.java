package com.joprelys.backend.spatial.api;

import java.net.URI;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        HospitalLocationConfigurationController.class,
        HospitalBedConfigurationController.class,
        InpatientSpaceProfileController.class
})
public class SpatialConfigurationExceptionHandler {

    @ExceptionHandler({DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class})
    public ProblemDetail handleConcurrentConfigurationUpdate(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "La configuration spatiale vient d'être modifiée par une autre opération.");
        problem.setTitle("Conflit de mise à jour de la configuration spatiale");
        problem.setType(URI.create("https://joprelys.com/problems/spatial-configuration-conflict"));
        return problem;
    }
}
