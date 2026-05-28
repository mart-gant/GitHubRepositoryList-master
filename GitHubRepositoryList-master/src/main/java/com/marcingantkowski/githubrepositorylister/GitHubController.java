package com.marcingantkowski.githubrepositorylister;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public final class GitHubController {

    private final GitHubService gitHubService;

    public GitHubController(final GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @GetMapping("/{username}/repositories")
    public List<RepositoryResponse> listRepositories(@PathVariable final String username) {
        return gitHubService.getRepositories(username);
    }
}
