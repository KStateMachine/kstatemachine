# Publishing to maven central

## Current process (fully automated)
Publishing is done by [vanniktech gradle-maven-publish-plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/central/)
via the [Central Portal Publisher API](https://central.sonatype.org/publish/publish-portal-api/).

1. Bump `library` version in `gradle/libs.versions.toml`.
2. Push a `v*` tag. `Gradle Publish` GitHub workflow runs `./gradlew publishAndReleaseToMavenCentral`.
3. The plugin collects publications of all modules into a **single bundle** (zip), uploads it with **one** request as
   one deployment, waits until the Portal validates it and releases it automatically.
   If validation fails the workflow fails, nothing is released.
4. After successful publishing the workflow creates GitHub release `KStateMachine v*` with auto-generated notes
   (they can be edited later in GitHub UI).

No manual steps in https://central.sonatype.com/publishing/deployments are required.
A deployment contains 66 components in the Portal UI (3 modules × 12 publications: root + 11 KMP targets,
the Portal additionally counts type variants like `klib` separately).

Workflow secrets: `MAVEN_USERNAME` / `MAVEN_PASSWORD` (Central Portal user token),
`SIGNING_KEY` (armored private key) / `SIGNING_PASSWORD`.

### Publishing from a local machine
Signing uses gpg configured in `local.properties` (see comment in root `build.gradle.kts`).
Portal user token is passed as gradle properties, for example in `~/.gradle/gradle.properties`:
```properties
mavenCentralUsername=...
mavenCentralPassword=...
```
* `./gradlew publishToMavenCentral` uploads a deployment without releasing it (release or drop it in the UI).
* `./gradlew publishAndReleaseToMavenCentral` uploads and releases.

## History: why the old process was manual
Previously the library was published by plain `maven-publish` to the
[OSSRH Staging API compatibility service](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/)
followed by `POST /manual/upload/defaultRepository/io.github.nsk90`.
Publications had a random count of components in the UI, and publishing such a version caused dependency resolution
errors on client side, without any errors in logs or UI.

The likely cause: that service differentiates staging repositories **by client IP**, and the upload call must be done
from the same IP that uploaded the files. Gradle uploads every file with a separate request, so when the GitHub runner's
outgoing IP changed, files were split into several repositories. Only one of them was promoted, others stayed in
`open` state (not visible in UI) and had to be found and promoted manually.

### Staging API commands (cleanup of leftover open repositories)
Beware that APIs respond with a timeout error very often.

list open repos (open means uploaded and not visible in ui)
```bash
curl -X GET -u "{MAVEN_USER}:{MAVEN_PASSWORD}" "https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?ip=any&state=open"
```

manually trigger processing of open repo (would be visible in UI with some status)
```bash
curl -X POST -u "{MAVEN_USER}:{MAVEN_PASSWORD}" "https://ossrh-staging-api.central.sonatype.com/manual/upload/repository/{MAVEN_USER}/{IP}/io.github.nsk90--default-repository"
```

drops the repository
```bash
curl -X DELETE -u "{MAVEN_USER}:{MAVEN_PASSWORD}" "https://ossrh-staging-api.central.sonatype.com/manual/drop/repository/{MAVEN_USER}/{IP}/io.github.nsk90--default-repository"
```
