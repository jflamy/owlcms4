# Building the images

- `docker` must be running

- `mvn clean package` will pull previously compiled uber jars from bintray and create docker images.

- `mvn clean deploy` to deploy the containers to the `owlcms-docker-containers.bintray.io/owlcms` repository.  The repository is public, no credentials are needed to pull the image.

  - In theory, the credentials are in the MAVEN settings, but in practice, it may be required to run the following beforehand

    ```
    docker login -u jflamy -p <API_KEY> owlcms-docker-containers.bintray.io
    ```

  - The pushed images are given a tag according to what is found in `target/docker-tag/docker-tag.properties` and a `latest` tag.

# Docker Deployment

```bash
docker run -p 8081:8080 owlcms:latest
```

will run the image that can then be accessed as http://localhost:8081
Using `-p` is necessary to forward the 8080 port of the container to the outside world.
When running the image in this way an ephemeral H2 database is created inside the container, and vanishes when the container is deleted.