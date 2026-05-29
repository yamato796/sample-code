# Local smoke-test environment

A self-contained Jenkins + local docker registry that runs the take-home
pipeline end-to-end without any cloud credentials or external dependencies.
Use this to verify the pipeline locally.

## What's in here

| File | Purpose |
|------|---------|
| `Dockerfile` | Extends `jenkins/jenkins:lts-jdk17`, bakes plugins and JCasC |
| `docker-compose.yml` | Brings up Jenkins + a local `registry:2` |
| `plugins.txt` | Required Jenkins plugins |
| `casc.yaml` | Jenkins Configuration as Code: auth, library, seed job |
| `bootstrap.sh` | First-run setup of `lib-source` and `app-source` git repos |
| `Jenkinsfile` | Local copy of the consumer Jenkinsfile (points at local registry) |

## Run it

```bash
cd ci/local-jenkins
docker compose up --build
```

First start takes ~3 minutes (plugin install). Subsequent starts are fast.

Then open:

- Jenkins UI: <http://localhost:8080> (login: `admin` / `admin`)
- Registry catalog: <http://localhost:5000/v2/_catalog>

A pipeline job named `simple-go-server` is pre-seeded. Click into it and hit
**Build Now**.

## What you should see

1. **Stage view**: Setup -> Build Test Image -> *parallel* Lint + Unit Tests -> Build Production Image -> Push to Registry
2. **Tags computed**: console log shows
   `Image tags for this build: <sha>,main-<sha>,main-latest,latest`
   (the seed job clones the main branch; ImageTagger applies main-branch rules)
3. **Registry receives push**: after the run, hit
   `curl http://localhost:5000/v2/simple-go-server/tags/list`
   to confirm the four tags are present.

## Resetting

```bash
docker compose down -v   # wipes jenkins_home + registry_data
```

## Caveats and notes

- **Docker socket binding (DinD via socket)** is used for simplicity. The
  Jenkins container shares the host docker daemon via `/var/run/docker.sock`.
  This is the right call for local dev but inappropriate for production:
  any pipeline could control the host docker daemon. Production builders
  should use Kaniko, Buildah, or a properly isolated DinD sidecar.
- **`numExecutors: 3`** on the built-in node represents one of the assignment's
  five build nodes during a busy peak. To see the lockable-resource and
  throttle behaviour interact, launch several builds concurrently.
- **ECR auth is bypassed** because `EcrPublisher.isLocalRegistry()` detects
  `localhost:` and skips the AWS login. The production code path (instance
  profile + AssumeRole) is exercised in real environments.
- The local registry is **insecure HTTP**. Docker treats `localhost` as
  insecure by default, so no daemon config changes are needed.

## How this maps to production

The local setup is a faithful single-node mirror of the production design,
minus the cloud-specific pieces:

| Aspect | Local | Production |
|---|---|---|
| Build agents | 1 built-in node, 3 executors | 5 dedicated agents |
| Registry | local `registry:2` | AWS ECR |
| Registry auth | none (insecure localhost) | IAM instance profile + AssumeRole |
| Library source | derived git repo on disk | dedicated GitHub repo, tag-pinned |
| Job seeding | JCasC seed job | Multibranch pipeline + GitHub webhook |
