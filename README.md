# GitHub Repository Lister

A Spring Boot application that lists public GitHub repositories for a given user, excluding forks.

## Description

The application exposes a REST endpoint that returns:

- Repository name
- Owner login
- Branch names
- Last commit SHA for each branch

## Technology Stack

- Java 25
- Spring Boot 4.0.1
- Gradle with Kotlin DSL
- Spring Web MVC
- Spring RestClient
- Spring RestTestClient
- WireMock Spring Boot integration

## Requirements

- Java 25

## Build

```bash
./gradlew build
```

## Run

```bash
./gradlew bootRun
```

The application starts on `http://localhost:8080`.

## API

### List User Repositories

`GET /users/{username}/repositories`

Returns all non-fork repositories for the specified GitHub user.

Successful response:

```json
[
  {
    "repositoryName": "example-repo",
    "ownerLogin": "username",
    "branches": [
      {
        "name": "main",
        "lastCommitSha": "abc123def456"
      }
    ]
  }
]
```

User not found response:

```json
{
  "status": 404,
  "message": "Github user not found"
}
```

## Example

```bash
curl http://localhost:8080/users/octocat/repositories
```

## Testing

```bash
./gradlew test
```

Integration tests start the application with `@SpringBootTest(webEnvironment = RANDOM_PORT)`, call it through `RestTestClient`, and emulate GitHub API responses with WireMock.

## Configuration

The backing API base URL is configured with:

```properties
github.api.base-url=https://api.github.com
```

The application does not implement pagination or authentication.
