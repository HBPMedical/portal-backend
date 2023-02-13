#######################################################
# Build the spring boot maven project
#######################################################
FROM maven:3.8.5-openjdk-11 as mvn-build-env

ENV CODE_PATH="/opt/code"
WORKDIR $CODE_PATH

COPY pom.xml $CODE_PATH

RUN mvn clean compile test

COPY src/ $CODE_PATH/src

RUN mvn clean package

#######################################################
# Setup the running container
#######################################################
FROM openjdk:11.0.15-jdk
MAINTAINER Thanasis Karampatsis <tkarabatsis@athenarc.gr>

#######################################################
# Setting up timezone
#######################################################
ENV TZ=Etc/GMT
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

#######################################################
# Setting up env variables and workdir
#######################################################
ENV APP_CONFIG_TEMPLATE="/opt/config/application.tmpl"
ENV APP_CONFIG_LOCATION="/opt/config/application.yml"
ENV SPRING_CONFIG_LOCATION="file:/opt/config/application.yml"

WORKDIR /opt


#######################################################
# Install dockerize
#######################################################
ENV DOCKERIZE_VERSION v0.6.1
RUN wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && rm dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz


#######################################################
# Prepare the spring boot application files
#######################################################
COPY /config/application.tmpl $APP_CONFIG_TEMPLATE
COPY --from=mvn-build-env /opt/code/target/portal-backend.jar /usr/share/jars/


#######################################################
# Configuration for the backend config files
#######################################################
ENV DISABLED_ALGORITHMS_CONFIG_PATH="/opt/portal/algorithms/disabledAlgorithms.json"
COPY /config/disabledAlgorithms.json $DISABLED_ALGORITHMS_CONFIG_PATH
VOLUME /opt/portal/api


ENTRYPOINT ["sh", "-c", "dockerize -template $APP_CONFIG_TEMPLATE:$APP_CONFIG_LOCATION java -Daeron.term.buffer.length -jar /usr/share/jars/portal-backend.jar"]
EXPOSE 8080
HEALTHCHECK --start-period=60s CMD curl -v --silent http://localhost:8080/services/actuator/health 2>&1 | grep UP
