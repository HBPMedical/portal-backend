# hbpmip/portal-backend

[![License](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0.html) [![](https://images.microbadger.com/badges/version/hbpmip/portal-backend.svg)](https://hub.docker.com/r/hbpmip/portal-backend/tags/ "hbpmip/portal-backend image tags") [![](https://images.microbadger.com/badges/image/hbpmip/portal-backend.svg)](https://microbadger.com/#/images/hbpmip/portal-backend "hbpmip/portal-backend on microbadger")

## Docker image for the MIP portal backend.

To use this image, you need a running instance of PostgreSQL and to configure the software using the following environment variables.


### LOG LEVELS ###
* LOG_LEVEL: log level for the developer added logs. Default is "ERROR".
* LOG_LEVEL_FRAMEWORK: log level for all the framework logs. Default is "ERROR".


### AUTHENTICATION ###
* AUTHENTICATION: true for production, false for development.


### RELEASE STAGE ###
* PRODUCTION: Deployed on production? (True/False) Default is True.


### DATABASE CONFIGURATION ###
* PORTAL_DB_URL: JDBC URL to connect to the portal database, default value is "jdbc:postgresql://127.0.0.1:5432/portal".
* PORTAL_DB_SCHEMA: Database schema, default value is "public".
* PORTAL_DB_USER: User to use when connecting to the portal database, default value is "postgres".
* PORTAL_DB_PASSWORD: Password to use when connecting to the portal database.


### EXTERNAL SERVICES ###
* EXAREME_URL: URL to Exareme server. Default is "http://localhost:9090" .

* GALAXY_URL: URL to Workflow server. Default is "http://localhost:8090/" .
* GALAXY_API_KEY: The api key to authorize galaxy requests.
* GALAXY_USERNAME: The username of galaxy user to be able to embed the frame.
* GALAXY_PASSWORD: The password of galaxy user.


### KEYCLOAK ###
* KEYCLOAK_AUTH_URL: Keycloak authentication URL.
* KEYCLOAK_REALM: Keycloak realm user for authentication.
* KEYCLOAK_CLIENT_ID: The keycloak client id.
* KEYCLOAK_CLIENT_SECRET: The keycloak secret to be able to authenticate.