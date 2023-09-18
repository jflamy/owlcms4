## Testing Locally with Docker Desktop

First, you need to build the jars required to build a docker image.   We want the same jars as will be used in production.  Then we build the docker images and install them to the local docker image.  This does not push them to the remote docker repository.

The command below does these steps.

You need to have Docker desktop configured, and DOCKER_REG environment variable set to `owlcms`. The docker desktop daemon should be running in insecure mode, listening to port 2375 without TLS.

```bash
mvn -Dmaven.test.skip=true -P production -pl owlcms-docker -am package
```

### Testing on Docker

```
docker run -p 8080:8080 -i owlcms/owlcms:latest
```


