# Build Stage

FROM maven:3.9.5-eclipse-temurin-17 as build
WORKDIR usr/src/app
COPY . ./
RUN mvn clean package

# Package stage
FROM azul/zulu-openjdk:17
COPY --from=build /usr/src/app/target/*.jar /usr/app/app.jar
EXPOSE 9000
ENTRYPOINT ["java","-jar","/usr/app/app.jar"]


