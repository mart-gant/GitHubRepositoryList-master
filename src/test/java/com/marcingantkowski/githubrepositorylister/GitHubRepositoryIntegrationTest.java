package com.marcingantkowski.githubrepositorylister;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@EnableWireMock(@ConfigureWireMock(name = "github", baseUrlProperties = "github.api.base-url"))
public final class GitHubRepositoryIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RestTestClient restTestClient;

    @InjectWireMock("github")
    private WireMockServer gitHubServer;

    @BeforeEach
    void resetWireMock() {
        gitHubServer.resetAll();
    }

    @Test
    void shouldListNonForkRepositoriesWithBranches() throws Exception {
        gitHubServer.stubFor(get(urlEqualTo("/users/testuser/repos"))
                .willReturn(okJson("""
                                [
                                  { "name": "repo1", "fork": false, "owner": { "login": "testuser" } },
                                  { "name": "forked-repo", "fork": true, "owner": { "login": "testuser" } }
                                ]
                                """)));

        gitHubServer.stubFor(get(urlEqualTo("/repos/testuser/repo1/branches"))
                .willReturn(okJson("""
                                [
                                  { "name": "main", "commit": { "sha": "abcd1234" } },
                                  { "name": "dev", "commit": { "sha": "efgh5678" } }
                                ]
                                """)));

        final var responseBody = restTestClient.get()
                .uri("/users/testuser/repositories")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        final var repositories = objectMapper.readValue(responseBody, RepositoryResponse[].class);
        assertThat(repositories)
                .hasSize(1)
                .allSatisfy(repo -> {
                    assertThat(repo.repositoryName()).isEqualTo("repo1");
                    assertThat(repo.ownerLogin()).isEqualTo("testuser");
                    assertThat(repo.branches()).hasSize(2);
                    assertThat(repo.branches())
                            .containsExactlyInAnyOrder(
                                    new BranchResponse("main", "abcd1234"),
                                    new BranchResponse("dev", "efgh5678")
                            );
                });
    }

    @Test
    void shouldReturnEmptyListWhenGithubUserHasNoRepositories() throws Exception {
        gitHubServer.stubFor(get(urlEqualTo("/users/emptyuser/repos"))
                .willReturn(okJson("[]")));

        final var responseBody = restTestClient.get()
                .uri("/users/emptyuser/repositories")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        final var repositories = objectMapper.readValue(responseBody, RepositoryResponse[].class);
        assertThat(repositories).isEmpty();
    }

    @Test
    void shouldReturnNotFoundWhenGithubUserDoesNotExist() throws Exception {
        gitHubServer.stubFor(get(urlEqualTo("/users/nonexistent/repos"))
                .willReturn(aResponse().withStatus(404)));

        final var responseBody = restTestClient.get()
                .uri("/users/nonexistent/repositories")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        final var errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        assertThat(errorResponse.status()).isEqualTo(404);
        assertThat(errorResponse.message()).isEqualTo("Github user not found");
    }
}
