FROM vulhub/fastjson:1.2.24

WORKDIR /app

COPY target/web-security-platform-1.0.0.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
