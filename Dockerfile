#######################################################
# Build the Spring Boot Maven Project
#######################################################
FROM maven:3.9.5-amazoncorretto-21 AS mvn-build-env
LABEL maintainer="Thanasis Karampatsis <tkarabatsis@athenarc.gr>"

ENV CODE_PATH="/opt/code"
WORKDIR $CODE_PATH

COPY pom.xml $CODE_PATH
RUN mvn clean compile test

COPY src/ $CODE_PATH/src
RUN mvn clean package

#######################################################
# Setup the Running Container
#######################################################
FROM amazoncorretto:21-alpine3.18

#######################################################
# Setting up timezone
#######################################################
ENV TZ=Etc/GMT
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

#######################################################
# Setting up environment
#######################################################
ENV APP_CONFIG_LOCATION="/opt/config/application.yml"
ENV SPRING_CONFIG_LOCATION="file:/opt/config/application.yml"

ENV SERVICE="portal-backend"
ENV FEDERATION="default"
ENV LOG_LEVEL="INFO"
ENV FRAMEWORK_LOG_LEVEL="INFO"

WORKDIR /opt

RUN apk add --no-cache curl

#######################################################
# Install Dockerize (Fixed)
#######################################################
ENV DOCKERIZE_VERSION=v0.6.1
RUN curl -L -o /usr/local/bin/dockerize "https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64" \
    && chmod +x /usr/local/bin/dockerize

#######################################################
# Prepare the Spring Boot Application Files
#######################################################
COPY --from=mvn-build-env /opt/code/target/classes/application.yml $APP_CONFIG_LOCATION
COPY --from=mvn-build-env /opt/code/target/portal-backend.jar /usr/share/jars/

#######################################################
# Configuration for the Backend Config Files
#######################################################
VOLUME /opt/portal/api

#######################################################
# Define Entrypoint (Fixed)
#######################################################
ENTRYPOINT ["sh", "-c", "dockerize -template $APP_CONFIG_LOCATION:$APP_CONFIG_LOCATION -- java -jar /usr/share/jars/portal-backend.jar"]

# Expose the application port
EXPOSE 8080

# Healthcheck
HEALTHCHECK --start-period=60s CMD curl -f http://localhost:8080/services/actuator/health || exit 1
