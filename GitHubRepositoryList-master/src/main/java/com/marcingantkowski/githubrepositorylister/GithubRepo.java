package com.marcingantkowski.githubrepositorylister;

public record GithubRepo(
        String name,
        boolean fork,
        Owner owner
) {
    public record Owner(String login) {}
}
