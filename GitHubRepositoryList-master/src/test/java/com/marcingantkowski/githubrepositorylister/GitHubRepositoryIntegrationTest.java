package com.marcingantkowski.githubrepositorylister;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GitHubRepositoryIntegrationTest {

    private static WireMockServer wireMockServer;

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(8089); // port WireMock
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

    @Test
    void testListRepositoriesFiltersForksAndReturnsBranches() {
        // Initialize RestClient
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        // Mock GitHub repositories
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

        // Mock branches for repo1
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

        ResponseEntity<String> response = restClient.get()
                .uri("/users/testuser/repositories")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("repo1")
                .doesNotContain("forked-repo")
                .contains("main")
                .contains("abcd1234");
    }

    @Test
    void testNonExistingUserReturns404() {
        // Initialize RestClient
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        stubFor(get(urlEqualTo("/users/nonexistent/repos"))
                .willReturn(aResponse().withStatus(404)));

        ResponseEntity<String> response = restClient.get()
                .uri("/users/nonexistent/repositories")
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                          (request, clientResponse) -> {
                              // Suppress default error handler to get ResponseEntity with error status
                          })
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .contains("\"status\":404")
                .contains("Github user not found");
    }

    @Test
    void testParallelBranchFetchingWithFixedDelay() {
        // Initialize RestClient
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        // Mock GitHub repositories - 3 repos, 1 is a fork
        stubFor(get(urlEqualTo("/users/testuser/repos"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(1000)  // 1 second delay
                        .withBody("""
                                [
                                  { "name": "repo1", "fork": false, "owner": { "login": "testuser" } },
                                  { "name": "repo2", "fork": false, "owner": { "login": "testuser" } },
                                  { "name": "forked-repo", "fork": true, "owner": { "login": "testuser" } }
                                ]
                                """)));

        // Mock branches for repo1 with delay
        stubFor(get(urlEqualTo("/repos/testuser/repo1/branches"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(1000)  // 1 second delay
                        .withBody("""
                                [
                                  { "name": "main", "commit": { "sha": "abc123" } }
                                ]
                                """)));

        // Mock branches for repo2 with delay
        stubFor(get(urlEqualTo("/repos/testuser/repo2/branches"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(1000)  // 1 second delay
                        .withBody("""
                                [
                                  { "name": "develop", "commit": { "sha": "def456" } }
                                ]
                                """)));

        // Measure execution time
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        ResponseEntity<String> response = restClient.get()
                .uri("/users/testuser/repositories")
                .retrieve()
                .toEntity(String.class);

        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTime();

        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("repo1")
                .contains("repo2")
                .doesNotContain("forked-repo");

        // Verify 3 total requests (1 for repos + 2 for branches)
        verify(3, getRequestedFor(urlMatching(".*")));

        // Verify timing: should be around 2000ms (first request 1s, then 2 parallel requests taking 1s)
        // Allow range of 2000-3000ms to account for processing overhead
        assertThat(totalTimeMillis)
                .as("Total execution time should be between 2000ms and 3000ms due to parallel execution")
                .isBetween(2000L, 3000L);
    }
}
