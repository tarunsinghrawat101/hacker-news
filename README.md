# Hacker News Clone

A Spring Boot web application for sharing text-based posts, browsing community stories, interacting with posts, and managing member-submitted content.

This README describes the supplied project snapshot, how to run it locally, its current member/admin capabilities, and possible future enhancements. It does not change application behavior.

## Table of contents

- [Project overview](#project-overview)
- [Technology stack](#technology-stack)
- [Current functionality](#current-functionality)
- [Post content requirements](#post-content-requirements)
- [Member profile and hidden posts](#member-profile-and-hidden-posts)
- [Admin capabilities](#admin-capabilities)
- [Run locally](#run-locally)
- [Configuration](#configuration)
- [Database notes](#database-notes)
- [Tests](#tests)
- [Future roadmap](#future-roadmap)
- [Existing-flow protection](#existing-flow-protection)

## Project overview

The application provides a community news/story experience. Members can create and manage their posts, browse published content, and use features such as likes, comments, favorites, and hiding posts. Administrators have a separate area for reviewing member activity and managing posts.

The application is built as a server-rendered Spring Boot application using Thymeleaf templates and Spring Data JPA.

## Technology stack

- Java (the Maven project currently targets Java 11)
- Spring Boot 2.5.5
- Spring MVC
- Spring Security
- Spring Data JPA / Hibernate
- Thymeleaf
- Maven Wrapper
- H2 database by default
- PostgreSQL runtime driver available for external database configuration

## Current functionality

The supplied project includes the following application areas:

### Member features

- Member registration, login, and logout.
- Member profile page.
- Create text-based posts.
- Optional URL field when creating or editing a post.
- Minimum text length of 1,292 words for a post submission.
- Save a post as a draft and return to edit it later.
- View a member's own posts from the profile page.
- Open post details from the profile by selecting the post heading.
- View the post's current state/status in the profile.
- Hide one post or select multiple posts to hide them from the member's normal view.
- Access hidden posts from the Hidden News area and unhide them.
- Like, comment on, and favorite posts.
- Browse and search published posts.

### Admin features

- Admin portal for viewing members and their post counts.
- View posts associated with a selected member.
- View legacy/unassigned posts that do not have an author email.
- Change post states using the admin moderation controls.
- Select multiple posts for bulk moderation.
- Mark posts as admin-deleted while retaining a record for the author's profile.
- Admin-deleted posts are excluded from public feeds.
- Members cannot edit or change the state of a post marked as deleted by an administrator.

### Post deletion and visibility

An administrator-deleted post is retained as a record rather than being physically removed from the database. This allows the original author to see that the post was removed while preventing further member edits or state changes.

A member hiding a post is a personal visibility preference. It does not delete the post and does not change its moderation state.

## Post content requirements

- **URL:** Optional. A member may submit a text-only post without a URL.
- **Text:** A submitted post must contain at least 1,292 words.
- **Drafts:** A draft can be saved without satisfying the submission word-count requirement. The word-count requirement is checked when the member submits the post.
- **Validation:** Submission validation is performed in the service layer, not only in the browser form.

## Member profile and hidden posts

The member profile provides a consolidated view of the member's own posts. Post headings can be opened to inspect the post and its status.

The profile also supports hiding posts:

1. Hide an individual post using its hide action.
2. Select multiple posts and hide them together.
3. Open the Hidden News page to view hidden posts.
4. Unhide a post when it should appear in the member's normal view again.

Hiding a post is separate from admin moderation. A hidden post may retain its existing post state.

## Admin capabilities

The Admin Portal provides member-level post management. Administrators can open a member's post list, inspect individual posts, and apply moderation actions. Bulk actions are available for selected posts.

Admin deletion is represented by a retained deletion marker (`admin_deleted`) so that the author's profile can continue to display the post's removal status. This is intentionally different from a member hiding a post.

The application bootstraps a demo administrator by default when the configured admin email is not already registered:

- Default email: `admin@gmail.com`
- Default password: `admin@123`

These are development defaults only. Change them before deploying the application anywhere accessible to other people. See [Configuration](#configuration).

## Run locally

### Prerequisites

- JDK installed.
- A terminal.
- Internet access on the first Maven build if dependencies are not already cached.

### Start the application

From the project root:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

On Windows, use:

```bat
mvnw.cmd clean test
mvnw.cmd spring-boot:run
```

By default, the application listens on:

```text
http://localhost:8085
```

Register a member account through the application. For local admin testing, use the configured bootstrap admin credentials.

## Configuration

The application reads configuration from environment variables. Defaults are shown below.

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8085` | HTTP server port |
| `DB_URL` | `jdbc:h2:file:./data/hackernewsclone;DB_CLOSE_ON_EXIT=FALSE` | JDBC database URL |
| `DB_DRIVER` | `org.h2.Driver` | JDBC driver class |
| `DB_USERNAME` | `sa` | Database username |
| `DB_PASSWORD` | empty | Database password |
| `ADMIN_BOOTSTRAP_ENABLED` | `true` | Enable/disable demo admin creation |
| `ADMIN_BOOTSTRAP_EMAIL` | `admin@gmail.com` | Bootstrap admin email |
| `ADMIN_BOOTSTRAP_PASSWORD` | `admin@123` | Bootstrap admin password |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP host |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | empty | SMTP account username |
| `MAIL_PASSWORD` | empty | SMTP account password |

Example for a local shell:

```bash
export ADMIN_BOOTSTRAP_ENABLED=true
export ADMIN_BOOTSTRAP_EMAIL=admin@example.com
export ADMIN_BOOTSTRAP_PASSWORD='replace-with-a-strong-password'
./mvnw spring-boot:run
```

For production, disable demo admin bootstrap after creating and securing the intended administrator account:

```bash
export ADMIN_BOOTSTRAP_ENABLED=false
```

Do not commit real database or email credentials to source control.

## Database notes

The default database is file-backed H2. Its files are stored under the project's `data/` directory.

**Keep the `data/` directory when replacing application source files if you need to preserve existing local records.** Removing the H2 database files will remove the local database contents.

The project has Hibernate schema update enabled (`spring.jpa.hibernate.ddl-auto=update`). This is convenient for local development, but production deployments should use controlled database migrations and backups rather than relying on automatic schema updates.

To use PostgreSQL, configure `DB_URL`, `DB_DRIVER`, `DB_USERNAME`, and `DB_PASSWORD` for the target PostgreSQL instance.

## Tests

Run the automated tests with:

```bash
./mvnw test
```

Run a clean build with:

```bash
./mvnw clean verify
```

If dependency downloads fail, check Maven Central connectivity and retry after network access is restored.

## Future roadmap

The following is a **proposed future workflow**, documented for planning only. It should not be treated as a new requirement for the current release or as a feature that this README enables.

### Proposed post-submission lifecycle

A future version could formalize the following review lifecycle:

```text
Member creates post
        |
        v
      DRAFT
        |
        | Member submits
        v
  PENDING_REVIEW
        |
        | Admin reviews
        v
   +----+----+
   |         |
   v         v
PUBLISHED  REJECTED
```

#### Proposed behavior

1. **Member creates post — DRAFT**
   - The member can create a post and save it without publishing it.
   - The member can return later to edit the draft.

2. **Member submits post — PENDING_REVIEW**
   - The member explicitly submits the completed post.
   - The post enters an admin review queue.
   - It is not shown in public feeds while awaiting review.

3. **Admin reviews**
   - The administrator reads the submitted post.
   - The administrator decides whether it meets the platform's publishing requirements.

4. **Admin approves — PUBLISHED**
   - The post becomes available in public discovery and feeds, subject to the application's visibility rules.

5. **Admin rejects — REJECTED**
   - The post is not published.
   - A later version could provide a rejection reason and a defined resubmission process.

### Other possible future enhancements

- Rejection reasons and member-facing moderation notes.
- Notifications when a post is approved or rejected.
- Draft autosave and last-edited timestamps.
- Moderation history/audit log showing who changed a post and when.
- Search, filters, and pagination in the member's post history.
- More granular admin permissions.
- Configurable content guidelines and moderation checks.
- A controlled migration framework for database schema changes.

**Implementation-status note:** This section is a roadmap description, not an instruction to activate or alter the current application. The supplied source snapshot already contains story-state enum/service/controller code (including `DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, and `REJECTED`). Therefore, if the intention is that the lifecycle above must be entirely inactive in a particular deployment, verify that deployment's branch and runtime behavior before relying on the roadmap label. This README itself does not change or disable that code.

## Existing-flow protection

When extending the application, preserve the existing authentication, member registration, post browsing, search/discovery, likes, comments, favorites, hidden-post handling, profile, and admin workflows unless a change is explicitly requested.

Before deploying changes:

1. Back up the database.
2. Run the automated test suite.
3. Verify member and admin permissions.
4. Test with both a new database and a copy of the existing database.
5. Confirm that existing published posts and member data remain accessible.
