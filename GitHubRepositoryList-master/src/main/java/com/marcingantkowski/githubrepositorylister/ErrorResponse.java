package com.marcingantkowski.githubrepositorylister;

record ErrorResponse(
        int status,
        String message
) {}
