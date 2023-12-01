FROM azul/zulu-openjdk:17
EXPOSE 9000
COPY /target/*.jar manga-harbor-server.jar
ENTRYPOINT ["java","-jar","/manga-harbor-server.jar"]
