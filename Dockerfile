# Build stage for Java classes
FROM hbpmip/java-base-build:3.6.0-jdk-11-0 as java-build-env

COPY pom.xml /project/

RUN cp /usr/share/maven/ref/settings-docker.xml /root/.m2/settings.xml \
    && mvn clean compile test

COPY src/ /project/src/

# Repeating the file copy works better. I dunno why.
RUN cp /usr/share/maven/ref/settings-docker.xml /root/.m2/settings.xml \
    && mvn clean package

FROM hbpmip/java-base:11.0.1-1

RUN rm -rf /var/lib/apt/lists/* /tmp/*

COPY docker/config/application.tmpl /opt/portal/config/application.tmpl
COPY docker/README.md docker/run.sh /opt/portal/

RUN addgroup portal \
    && adduser --system --disabled-password --uid 1000 --ingroup portal portal \
    && chmod +x /opt/portal/run.sh \
    && ln -s /opt/portal/run.sh /run.sh \
    && chown -R portal:portal /opt/portal

COPY --from=java-build-env /project/target/portal-backend.jar /usr/share/jars/

ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

USER portal
ENV APP_NAME="Portal backend" \
    APP_TYPE="Spring" \
    VERSION=$VERSION \
    BUILD_DATE=$BUILD_DATE \
    CONTEXT_PATH="/services" \
    BUGSNAG_KEY="dff301aa15eb795a6d8b22b600586f77"

WORKDIR /home/portal
ENTRYPOINT ["/run.sh"]

# 8080: Web service API, health checks on http://host:8080$CONTEXT_PATH/health
# 4089: Akka cluster
EXPOSE 4089 8080

HEALTHCHECK --start-period=60s CMD curl -v --silent http://localhost:8080$CONTEXT_PATH/health 2>&1 | grep UP

LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="hbpmip/portal-backend" \
      org.label-schema.description="Java backend for the MIP portal" \
      org.label-schema.url="https://mip.humanbrainproject.eu" \
      org.label-schema.vcs-type="git" \
      org.label-schema.vcs-url="https://github.com/LREN-CHUV/portal-backend" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.version="$VERSION" \
      org.label-schema.vendor="LREN CHUV" \
      org.label-schema.license="AGPLv3" \
      org.label-schema.docker.dockerfile="Dockerfile" \
      org.label-schema.memory-hint="2048" \
      org.label-schema.schema-version="1.0"
