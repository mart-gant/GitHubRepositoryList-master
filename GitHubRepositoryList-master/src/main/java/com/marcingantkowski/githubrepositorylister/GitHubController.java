package com.marcingantkowski.githubrepositorylister;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class GitHubController {

    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @GetMapping("/{username}/repositories")
    public List<RepositoryResponse> listRepositories(@PathVariable String username) {
        return gitHubService.getRepositories(username);
    }
}
