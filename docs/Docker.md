## Docker Deployment

- For Docker, you may use the `owlcms/owlcms` and `owlcms/publicresults` images on hub.docker.com.  `latest` is the tag for the latest stable image, `prerelease` is used for the latest prerelease.  
- In the environment variables for owlcms, provide a standard DATABASE_URL to a running postgres instance or container. `postgres://{user}:{password}@{hostname}:{port}/{database-name}` (all parameters are required).
- The database is initially empty. owlcms will create/alter the required tables so the account used requires the privileges to do so. See [Postgres database creation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/PostgreSQL?id=initial-configuration-of-postgresql) for additional info.