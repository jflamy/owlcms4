## Docker Deployment

- For Docker, you may use the `owlcms/owlcms` and `owlcms/publicresults` images on hub.docker.com.  `latest` is the tag for the latest stable image, `prerelease` is used for the latest prerelease.  

- The hostname or IP number for the database will be interpreted from inside the container. 

- When testing locally, you will need to edit your `data/pg_hba.conf` file in the Postgres installation directory. Add the IP address that you will use to reach the database server from within your container.

  ```
  host    all             all             192.168.1.254/32            scram-sha-256
  ```

- In the environment variables for owlcms, provide a standard DATABASE_URL to a running postgres instance or container. For example
   `postgres://{user}:{password}@{hostname}:{port}/{database-name}` (all parameters are required). 

- The database is initially empty. owlcms will create/alter the required tables so the account used requires the privileges to do so. See [Postgres database creation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/PostgreSQL?id=initial-configuration-of-postgresql) for additional info.

- Assuming a database name `owlcms_db`  is in use, a sample `run` command could therefore be (with the correct password and host address, of course).   The address is the same as was added to 

  ```bash
  docker run -p 8080:8080 \
  --env=DATABASE_URL=postgres://postgres:actual_password@192.168.1.254:5432/owlcms_db \
  owlcms/owlcms:latest
  ```
  
  