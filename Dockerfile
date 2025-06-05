# REQUIRED FILES TO BUILD THIS IMAGE
# ----------------------------------
# (1) target/*
#     See the main README.md file for instructions on compiling.  The compiled version is placed
#     into the target directory by default.
#
# HOW TO BUILD THIS IMAGE
# -----------------------
# Compile code:
#      $ mvn clean package
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

RUN mvn clean package -DskipTests


FROM ${BASE_REGISTRY}/${BASE_IMAGE}
ARG JAVA_OPT

USER 0

RUN mkdir /opt/pgcompare \
    && chown -R 1001:1001 /opt/pgcompare

COPY --from=builder /app/target/pgcompare.jar /opt/pgcompare/
COPY --from=builder /app/pgcompare.properties /opt/pgcompare/

USER 1001

WORKDIR /opt/pgcompare

# 暴露Spring Boot应用端口
EXPOSE 8080

# 设置健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/pgcompare/api/health || exit 1

# Spring Boot启动命令
ENTRYPOINT ["java", "-jar", "pgcompare.jar"]
