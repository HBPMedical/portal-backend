# hbpmip/portal-backend

[![License](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0.html) [![](https://images.microbadger.com/badges/version/hbpmip/portal-backend.svg)](https://hub.docker.com/r/hbpmip/portal-backend/tags/ "hbpmip/portal-backend image tags") [![](https://images.microbadger.com/badges/image/hbpmip/portal-backend.svg)](https://microbadger.com/#/images/hbpmip/portal-backend "hbpmip/portal-backend on microbadger")

## Docker image for the MIP portal backend.

To use this image, you need a running instance of PostgreSQL and to configure the software using the following environment variables.


### DATABASES

* PORTAL_DB_URL: JDBC URL to connect to the portal database, default value is "jdbc:postgresql://172.22.0.1:5432/portal".
* PORTAL_DB_SCHEMA: Database schema, default value is "public".
* PORTAL_DB_USER: User to use when connecting to the portal database, default value is "postgres".
* PORTAL_DB_PASSWORD: Password to use when connecting to the portal database.

* META_DB_URL: JDBC URL to connect to the metadata database, default value is "jdbc:postgresql://172.22.0.1:5432/meta".
* META_DB_SCHEMA: Database schema, default value is "public".
* META_DB_USER: User to use when connecting to the metadata database.
* META_DB_PASSWORD: Password to use when connecting to the metadata database.

* FEATURES_DB_URL: JDBC URL to connect to the science database, default value is "jdbc:postgresql://172.22.0.1:5433/features".
* FEATURES_DB_SCHEMA: Database schema, default value is "public".
* FEATURES_DB_USER: User to use when connecting to the science database, default value is "postgres".
* FEATURES_DB_PASSWORD: Password to use when connecting to the science database.
* FEATURES_DB_MAIN_TABLE: Table that contains the scientific data to use, default value is "features".


### OAUTH2 LOGIN

* AUTHENTICATION: "0" to disable authentication or "1" to enable authentication, default value is "1".
* CLIENT_ID: required when authentication is turned on, client ID for the [OpenID server of HBP](https://services.humanbrainproject.eu/oidc/).
* CLIENT_SECRET: required when authentication is turned on, client secret for the [OpenID server of HBP](https://services.humanbrainproject.eu/oidc/).
* TOKEN_URI: default to "https://services.humanbrainproject.eu/oidc/token".
* AUTH_URI: default to "https://services.humanbrainproject.eu/oidc/authorize".
* USER_INFO_URI: default to "https://services.humanbrainproject.eu/oidc/userInfo".
* REVOKE_TOKEN_URI "https://services.humanbrainproject.eu/oidc/slo".


### WEB FRONTEND

* FRONTEND_LOGIN_URL: URL to redirect to when login is required. Default to "http://frontend/services/login/hbp".
* FRONTEND_AFTER_LOGIN_URL: URL to redirect after login. Default to "http://frontend/home".
* FRONTEND_AFTER_LOGOUT_URL: URL to redirect to after logout. Default to "http://frontend/services/login/hbp".


### LOGGING

* LOG_LEVEL: log level for the developer added logs. Default is "ERROR".
* LOG_LEVEL_FRAMEWORK: log level for all the framework logs. Default is "ERROR".


### ENDPOINTS

* EXAREME_URL: URL to Exareme server, default value is "http://hbps2.chuv.ch:9090".
* WORKFLOW_URL: URL to Workflow server
* JWT_SECRET: "secret"

### EMBEDDED SERVER CONFIGURATION

* CONTEXT_PATH:  context path appended to all services running in this container. Default to "/services".
* SESSION_TIMEOUT: Timeout in milliseconds for session expiration. Default to 2592000.

### PROXY

* HTTP_PROXY_HOST: HTTP proxy host
* HTTP_PROXY_PORT: HTTP proxy port
* HTTPS_PROXY_HOST: HTTPS proxy host
* HTTPS_PROXY_PORT: HTTPS proxy port

## ERROR REPORTING

* RELEASE_STAGE: Release stage used when reporting errors to Bugsnag. Values are dev, staging, production
* DATA_CENTER_LOCATION: Location of the datacenter, used when reporting errors to Bugsnag


# TODO Refactor variables