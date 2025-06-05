# REQUIRED FILES TO BUILD THIS IMAGE
# ----------------------------------
# (1) target/*
#     See the main README.md file for instructions on compiling.  The compiled version is placed
#     into the target directory by default.
#
# HOW TO BUILD THIS IMAGE
# -----------------------
# Compile code:
#      $ mvn install
# Build Docker Image:
#      $ docker build -t {tag} .
#
# Pull base image
# ---------------
ARG MAVEN_VERSION=3.9.9
ARG BASE_REGISTRY=docker.io/library
ARG BASE_IMAGE=openjdk:8-jre-alpine
ARG JAVA_OPT="-XX:UseSVE=0"

FROM docker.io/library/maven:${MAVEN_VERSION} AS builder
LABEL stage=pgcomparebuilder
ARG JAVA_OPT

ENV _JAVA_OPTIONS=${JAVA_OPT}

WORKDIR /app
COPY . ./

RUN mvn clean install


FROM ${BASE_REGISTRY}/${BASE_IMAGE}
ARG JAVA_OPT

USER 0

RUN mkdir /opt/pgcompare \
    && chown -R 1001:1001 /opt/pgcompare

COPY --from=builder /app/docker/start.sh /opt/pgcompare

COPY --from=builder /app/docker/pgcompare.properties /etc/pgcompare/

COPY --from=builder /app/target/* /opt/pgcompare/

RUN chmod 770 /opt/pgcompare/start.sh

USER 1001

ENV PGCOMPARE_HOME=/opt/pgcompare \
    PGCOMPARE_CONFIG=/etc/pgcompare/pgcompare.properties \
    PATH=/opt/pgcompare:$PATH \
    _JAVA_OPTIONS=${JAVA_OPT}

CMD ["start.sh"]

WORKDIR "/opt/pgcompare"
