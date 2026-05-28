package com.marcingantkowski.githubrepositorylister;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public final class GitHubService {

    private final GitHubClient client;

    public GitHubService(final GitHubClient client) {
        this.client = client;
    }

    public List<RepositoryResponse> getRepositories(final String username) {
        final var repositories = client.getRepositories(username);

        return repositories.stream()
                .filter(repo -> !repo.fork())
                .parallel()
                .map(repo -> new RepositoryResponse(
                        repo.name(),
                        repo.owner().login(),
                        client.getBranches(repo.owner().login(), repo.name()).stream()
                                .map(branch -> new BranchResponse(
                                        branch.name(),
                                        branch.commit().sha()
                                ))
                                .toList()
                ))
                .toList();
    }
}
