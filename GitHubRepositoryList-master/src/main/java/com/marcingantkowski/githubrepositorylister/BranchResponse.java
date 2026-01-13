package com.marcingantkowski.githubrepositorylister;

public record BranchResponse(
        String name,
        String lastCommitSha
) {}
