package com.joprelys.backend.spatial.api;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.net.URI;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = SpatialController.class)
public class SpatialConcurrencyExceptionHandler {

    @ExceptionHandler({
            CannotAcquireLockException.class,
            PessimisticLockingFailureException.class,
            PessimisticLockException.class,
            LockTimeoutException.class
    })
    public ProblemDetail handleConcurrentBedClaim(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Le lit demandé vient d'être attribué par une autre opération.");
        problem.setTitle("Conflit d'affectation de lit");
        problem.setType(URI.create("https://joprelys.com/problems/bed-allocation-conflict"));
        return problem;
    }
}
