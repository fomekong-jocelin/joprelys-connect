package com.joprelys.backend.eco.api;

import com.joprelys.backend.eco.application.EcoBusinessException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erreurs de l'API écosystème en ProblemDetail, avec un code stable et sans détail technique ni donnée personnelle. */
@RestControllerAdvice(basePackageClasses = EcoExceptionHandler.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EcoExceptionHandler {

    @ExceptionHandler(EcoBusinessException.class)
    ProblemDetail business(EcoBusinessException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.error().status(), exception.error().message());
        problem.setProperty("code", exception.error().name());
        return problem;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ProblemDetail invalidRequest(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Requête invalide");
        problem.setProperty("code", "INVALID_REQUEST");
        return problem;
    }
}
