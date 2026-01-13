package com.marcingantkowski.githubrepositorylister;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class GitHubService {

    private final GitHubClient client;

    GitHubService(GitHubClient client) {
        this.client = client;
    }

    List<RepositoryResponse> getRepositories(String username) {
        return client.getRepositories(username).stream()
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
