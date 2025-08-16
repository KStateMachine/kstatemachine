# Publishing to maven central
The process of publishing to maven central is absolutely non-intuitive, not visualizable and confusing.
I faced unexpectable behaviour when my publications had a random count of components in the UI
https://central.sonatype.com/publishing/deployments
Publishing such a library version causes error on client's side, when he tries to resolve all required dependencies.
There are absolutely no errors nor in publishing logs nor in web UI.

## APIs
[Official docs](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#post-to-manualuploadrepositoryrepository-key)

Calling the fallowing api's helped at least close all the repositories in open status (open status is not visible in UI)
Beware that APIs response with a timeout error very often.

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