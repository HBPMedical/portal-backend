[![CHUV](https://img.shields.io/badge/CHUV-LREN-AF4C64.svg)](https://www.unil.ch/lren/en/home.html) [![License](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0.html)
[![DockerHub](https://img.shields.io/badge/docker-hbpmip%2Fportal--backend-008bb8.svg)](https://hub.docker.com/r/hbpmip/portal-backend/)

# Backend for the MIP portal

## DEV Deployment
To run the backend using an IDE for development, such as IntelliJ, you need a running instance of PostgreSQL.

## Deployment (using a Docker image)
Build the image: ` docker build -t hbpmip/portal-backend:latest .`

To use this image, you need a running instance of PostgreSQL and to configure the software using the following environment variables.

#### LOG LEVELS ###
* LOG_LEVEL: log level for the developer added logs. Default is "ERROR".
* LOG_LEVEL_FRAMEWORK: log level for all the framework logs. Default is "ERROR".

#### AUTHENTICATION ###
* AUTHENTICATION: true for production, false for development.

#### DATABASE CONFIGURATION ###
* PORTAL_DB_URL: JDBC URL to connect to the portal database, default value is "jdbc:postgresql://127.0.0.1:5432/portal".
* PORTAL_DB_SCHEMA: Database schema, default value is "public".
* PORTAL_DB_USER: User to use when connecting to the portal database, default value is "postgres".
* PORTAL_DB_PASSWORD: Password to use when connecting to the portal database.

#### EXTERNAL SERVICES ###
* EXAREME_URL: URL to Exareme server. Default is "http://localhost:9090" .
* EXAREME2_URL: URL to Exareme2 server. Default is "http://localhost:5000" .
* GALAXY_URL: URL to Workflow server. Default is "http://localhost:8090/" .
* GALAXY_API_KEY: The api key to authorize galaxy requests.
* GALAXY_USERNAME: The username of galaxy user to be able to embed the frame.
* GALAXY_PASSWORD: The password of galaxy user.

#### KEYCLOAK ###
* KEYCLOAK_AUTH_URL: Keycloak authentication URL.
* KEYCLOAK_REALM: Keycloak realm user for authentication.
* KEYCLOAK_CLIENT_ID: The keycloak client id.
* KEYCLOAK_CLIENT_SECRET: The keycloak secret to be able to authenticate.

# Acknowledgement
This project/research received funding from the European Union’s Horizon 2020 Framework Programme for Research and Innovation under the Framework Partnership Agreement No. 650003 (HBP FPA).
