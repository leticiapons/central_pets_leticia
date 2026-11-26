# Receita para montar o site completo (telas + back-end) em um único serviço, usada na hospedagem na nuvem.
# Uso local não precisa dela: veja o README e o arquivo iniciar-site.bat.

# ---------- 1) Compilação ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
# As telas (pasta frontend) vão para dentro do back-end, que as entrega no mesmo endereço da API.
COPY frontend backend/src/main/resources/static

RUN mvn -q -f backend/pom.xml -DskipTests package

# ---------- 2) Execução ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/backend/target/*.jar app.jar

# Memória enxuta, para caber nos planos gratuitos (512 MB).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -Xss512k"

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
