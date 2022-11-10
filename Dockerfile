FROM gradle:4.8.0-jdk8 AS build
COPY --chown=gradle:gradle . /tourguide
WORKDIR /tourguide
RUN gradle build

FROM openjdk:8-jre-slim
COPY --from=build /tourguide/build/libs/*.jar tourguide.jar
EXPOSE 8080
ENTRYPOINT java -jar tourguide.jar
