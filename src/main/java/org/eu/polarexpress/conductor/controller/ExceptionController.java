package org.eu.polarexpress.conductor.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionController {
    private final Logger logger = LoggerFactory.getLogger(ExceptionController.class);

    @ExceptionHandler(RuntimeException.class)
    public String exceptionHandler(RuntimeException exception) {
        logger.error(exception.getMessage());
        return "redirect:/";
    }

}
