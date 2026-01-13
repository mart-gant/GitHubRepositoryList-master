# GitHub Repository Lister

A Spring Boot application that lists GitHub repositories for a given user, excluding forks.

## Description

This application provides a REST API endpoint to retrieve information about GitHub repositories for a specific user. The response includes:
- Repository name
- Owner login
- List of branches with their names and last commit SHA

## Technology Stack

- Java 25
- Spring Boot 4.0.1
- Gradle (Kotlin DSL)
- Spring Web MVC
- Spring REST Client
- WireMock (for integration tests)

## Requirements

- Java 25
- Gradle

## Build

```bash
./gradlew build
```

## Run

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## API Endpoint

### List User Repositories

**GET** `/users/{username}/repositories`

Returns all non-fork repositories for the specified GitHub user.

#### Response Format (Success)

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

#### Response Format (User Not Found - 404)

```json
{
  "status": 404,
  "message": "Github user not found"
}
```

## Example Usage

```bash
curl http://localhost:8080/users/octocat/repositories
```

## Testing

Run integration tests:

```bash
./gradlew test
```

The application includes integration tests using WireMock to emulate the GitHub API.

## Configuration

The application uses GitHub API v3 (`https://api.github.com`) as the backing service.

## Notes

- The application filters out forked repositories
- No pagination support
- No authentication required for public repositories
- Designed as a simple proxy with Controller/Service/Client architecture
