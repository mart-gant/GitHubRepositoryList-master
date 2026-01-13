package com.marcingantkowski.githubrepositorylister;

class GitHubUserNotFoundException extends RuntimeException {
    GitHubUserNotFoundException(String message) {
        super(message);
    }
}
