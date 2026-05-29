package com.marcingantkowski.githubrepositorylister;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public final class GitHubService {

    private final GitHubClient client;

    public GitHubService(final GitHubClient client) {
        this.client = client;
    }

    public List<RepositoryResponse> getRepositories(final String username) {
        final var repositories = client.getRepositories(username);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var futures = repositories.stream()
                    .filter(repo -> !repo.fork())
                    .map(repo -> executor.submit(() -> toRepositoryResponse(repo)))
                    .toList();

            return futures.stream()
                    .map(this::getCompleted)
                    .toList();
        }
    }

    private RepositoryResponse getCompleted(final Future<RepositoryResponse> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching GitHub repository branches", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Failed to fetch GitHub repository branches", e.getCause());
        }
    }

    private RepositoryResponse toRepositoryResponse(final GithubRepo repo) {
        return new RepositoryResponse(
                repo.name(),
                repo.owner().login(),
                client.getBranches(repo.owner().login(), repo.name()).stream()
                        .map(branch -> new BranchResponse(
                                branch.name(),
                                branch.commit().sha()
                        ))
                        .toList()
        );
    }
}
