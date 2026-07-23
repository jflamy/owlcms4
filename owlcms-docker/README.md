## Testing Locally with Docker Desktop

First, build the jars required for the Docker image. This produces the same shaded application jar used in production and stages it in `owlcms-docker/target/docker-context/`. Then build the root Dockerfile; this does not push an image to a remote registry.

The command below does these steps.

You need Docker Desktop configured and running.

```bash
mvn -Dmaven.test.skip=true -P production -pl owlcms-docker -am package
docker build --platform linux/amd64 -t owlcms/owlcms:latest .
```

## Deploying the Same Image to Fly.io

`../owlcms/scripts/deploy.sh` first prepares this Docker context using the root POM revision, then asks Fly to build the repository-root `Dockerfile`. The checked-in [fly.toml](fly.toml) is based on the OWLCMS cloud deployment profile; the helper passes the target app and region directly to Fly, so this workflow does not require a sibling `cloud` checkout or a generated configuration file.

The defaults target `owlcms-next` in `yyz`. Override them when needed:

```bash
FLY_APP=my-owlcms REGION=ord ../owlcms/scripts/deploy.sh
```

### Testing on Docker

```
docker run -p 8080:8080 -i owlcms/owlcms:latest
```


