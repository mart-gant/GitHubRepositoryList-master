package com.marcingantkowski.githubrepositorylister;

public sealed record GithubBranch(
        String name,
        Commit commit
) permits Commit {
    public sealed record Commit(String sha) permits GithubBranch {}
}
