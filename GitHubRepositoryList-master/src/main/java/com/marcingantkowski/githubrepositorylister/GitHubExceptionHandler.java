package com.marcingantkowski.githubrepositorylister;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GitHubExceptionHandler {

    @ExceptionHandler(GitHubUserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse handleUserNotFound(GitHubUserNotFoundException ex) {
        return new ErrorResponse(404, ex.getMessage());
    }
}
