package com.marcingantkowski.githubrepositorylister;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public final class GitHubClient {

    private final RestClient restClient;

    public GitHubClient(final RestClient restClient) {
        this.restClient = restClient;
    }

    public List<GithubRepo> getRepositories(final String username) {
        try {
            return restClient.get()
                    .uri("/users/{username}/repos", username)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (HttpClientErrorException.NotFound e) {
            throw new GitHubUserNotFoundException("Github user not found");
        }
    }

    public List<GithubBranch> getBranches(final String owner, final String repo) {
        return restClient.get()
                .uri("/repos/{owner}/{repo}/branches", owner, repo)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
