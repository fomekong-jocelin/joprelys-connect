package com.joprelys.backend.spatial.api;

import java.net.URI;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = SpatialConfigurationController.class)
public class SpatialConfigurationExceptionHandler {

    @ExceptionHandler({DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class})
    public ProblemDetail handleConcurrentConfigurationUpdate(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "La structure hospitalière vient d'être modifiée par une autre opération.");
        problem.setTitle("Conflit de mise à jour de la structure hospitalière");
        problem.setType(URI.create("https://joprelys.com/problems/hospital-structure-conflict"));
        return problem;
    }
}
