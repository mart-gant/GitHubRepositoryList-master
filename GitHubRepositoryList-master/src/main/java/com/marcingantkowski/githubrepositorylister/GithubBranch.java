package com.marcingantkowski.githubrepositorylister;

public record GithubBranch(
        String name,
        Commit commit
) {
    public record Commit(String sha) {}
}
