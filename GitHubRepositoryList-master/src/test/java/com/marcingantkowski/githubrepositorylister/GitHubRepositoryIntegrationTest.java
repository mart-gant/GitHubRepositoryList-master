package com.marcingantkowski.githubrepositorylister;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public final class GitHubRepositoryIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        configureFor("localhost", 8089);
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    /**
     * Test case 1: Filtruje forki i zwraca branches
     */
    @Test
    void testListRepositoriesFiltersForksAndReturnsBranches() throws Exception {
        // Arrange
        stubFor(get(urlEqualTo("/users/testuser/repos"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  { "name": "repo1", "fork": false, "owner": { "login": "testuser" } },
                                  { "name": "forked-repo", "fork": true, "owner": { "login": "testuser" } }
                                ]
                                """)));

        stubFor(get(urlEqualTo("/repos/testuser/repo1/branches"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  { "name": "main", "commit": { "sha": "abcd1234" } },
                                  { "name": "dev", "commit": { "sha": "efgh5678" } }
                                ]
                                """)));

        // Act
        final var response = testRestTemplate.getForEntity("/users/testuser/repositories", String.class);

        // Assert - deserializacja do obiektu
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        final var repositories = objectMapper.readValue(response.getBody(), RepositoryResponse[].class);
        assertThat(repositories)
                .hasSize(1)
                .allSatisfy(repo -> {
                    assertThat(repo.repositoryName()).isEqualTo("repo1");
                    assertThat(repo.ownerLogin()).isEqualTo("testuser");
                    assertThat(repo.branches()).hasSize(2);
                    assertThat(repo.branches())
                            .extracting(BranchResponse::name)
                            .containsExactlyInAnyOrder("main", "dev");
                });
    }

    /**
     * Test case 2: Użytkownik nie istnieje - 404
     */
    @Test
    void testNonExistingUserReturns404() throws Exception {
        // Arrange
        stubFor(get(urlEqualTo("/users/nonexistent/repos"))
                .willReturn(aResponse().withStatus(404)));

        // Act
        final var response = testRestTemplate.getForEntity(
                "/users/nonexistent/repositories",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        
        final var errorResponse = objectMapper.readValue(response.getBody(), ErrorResponse.class);
        assertThat(errorResponse.status()).isEqualTo(404);
        assertThat(errorResponse.message()).isEqualTo("Github user not found");
    }

    /**
     * Test case 3: Użytkownik istnieje ale ma 0 repozytoriów
     */
    @Test
    void testUserWithZeroRepositoriesReturnsEmptyList() throws Exception {
        // Arrange
        stubFor(get(urlEqualTo("/users/emptyuser/repos"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // Act
        final var response = testRestTemplate.getForEntity(
                "/users/emptyuser/repositories",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        final var repositories = objectMapper.readValue(response.getBody(), RepositoryResponse[].class);
        assertThat(repositories).isEmpty();
    }

    /**
     * Test case 4: Wszystkie repozytoria są forkami
     */
    @Test
    void testAllRepositoriesAreForksReturnsEmptyList() throws Exception {
        // Arrange
        stubFor(get(urlEqualTo("/users/forkuser/repos"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  { "name": "fork1", "fork": true, "owner": { "login": "forkuser" } },
                                  { "name": "fork2", "fork": true, "owner": { "login": "forkuser" } }
                                ]
                                """)));

        // Act
        final var response = testRestTemplate.getForEntity(
                "/users/forkuser/repositories",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        final var repositories = objectMapper.readValue(response.getBody(), RepositoryResponse[].class);
        assertThat(repositories).isEmpty();
    }

    /**
     * Test case 5: Repozytoria z 0 branchami
     */
    @Test
    void testRepositoryWithZeroBranchesReturnsRepository() throws Exception {
        // Arrange
        stubFor(get(urlEqualTo("/users/testuser/repos"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  { "name": "repo-no-branches", "fork": false, "owner": { "login": "testuser" } }
                                ]
                                """)));

        stubFor(get(urlEqualTo("/repos/testuser/repo-no-branches/branches"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // Act
        final var response = testRestTemplate.getForEntity(
                "/users/testuser/repositories",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        final var repositories = objectMapper.readValue(response.getBody(), RepositoryResponse[].class);
        assertThat(repositories)
                .hasSize(1)
                .anySatisfy(repo -> {
                    assertThat(repo.repositoryName()).isEqualTo("repo-no-branches");
                    assertThat(repo.branches()).isEmpty();
                });
    }

    /**
     * Test case 6: Testowanie parallelizacji z timeingiem
     * - 3 repozytoria (2 non-fork, 1 fork)
     * - 1 request dla repo list (1000ms)
     * - 2 równoległe requesty dla branches (1000ms każdy)
     * - Całkowity czas powinien być ~2000-2500ms (parallelizacja działa)
     */
    @Test
    void testParallelBranchFetchingWithCorrectTiming() throws Exception {
        // Arrange
        stubFor(get(urlEqualTo("/users/testuser/repos"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(1000)
                        .withBody("""
                                [
                                  { "name": "repo1", "fork": false, "owner": { "login": "testuser" } },
                                  { "name": "repo2", "fork": false, "owner": { "login": "testuser" } },
                                  { "name": "forked-repo", "fork": true, "owner": { "login": "testuser" } }
                                ]
                                """)));

        stubFor(get(urlEqualTo("/repos/testuser/repo1/branches"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(1000)
                        .withBody("""
                                [
                                  { "name": "main", "commit": { "sha": "abc123" } },
                                  { "name": "develop", "commit": { "sha": "abc124" } },
                                  { "name": "feature", "commit": { "sha": "abc125" } }
                                ]
                                """)));

        stubFor(get(urlEqualTo("/repos/testuser/repo2/branches"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(1000)
                        .withBody("""
                                [
                                  { "name": "main", "commit": { "sha": "def456" } }
                                ]
                                """)));

        // Act
        final var stopWatch = new StopWatch();
        stopWatch.start();

        final var response = testRestTemplate.getForEntity(
                "/users/testuser/repositories",
                String.class
        );

        stopWatch.stop();
        final var totalTimeMillis = stopWatch.getTime();

        // Assert - response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        final var repositories = objectMapper.readValue(response.getBody(), RepositoryResponse[].class);
        assertThat(repositories)
                .hasSize(2)
                .extracting(RepositoryResponse::repositoryName)
                .containsExactlyInAnyOrder("repo1", "repo2");

        // Assert - HTTP requests count (1 repos + 2 branches calls in parallel)
        verify(3, getRequestedFor(urlMatching(".*")));

        // Assert - timing: 1s (repo list) + 1s (parallel branches) = ~2s, with headroom to 2500ms
        assertThat(totalTimeMillis)
                .as("Total execution time should be between 2000ms and 2500ms due to parallel execution. " +
                    "Got: " + totalTimeMillis + "ms")
                .isBetween(2000L, 2500L);
    }
}
