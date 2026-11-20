# Contenedor del servidor Java de Biblioteca San Lucas.
# Render debe usar el repositorio como contexto de build (Root Directory vacio).
FROM eclipse-temurin:17-jdk AS compilacion
WORKDIR /codigo
COPY ["CONEXIÓN JAVA/Biblioteca_San_Lucas_Fase2_PostgreSQL/biblioteca/src/", "./src/"]
RUN mkdir -p out && \
    javac -encoding UTF-8 --release 17 -sourcepath src -d out \
      src/biblioteca/Main.java src/biblioteca/ConsolaMain.java src/biblioteca/PruebasBasicas.java && \
    jar --create --file biblioteca-web.jar --main-class biblioteca.Main -C out .

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=compilacion /codigo/biblioteca-web.jar ./biblioteca-web.jar
COPY ["CONEXIÓN JAVA/Biblioteca_San_Lucas_Fase2_PostgreSQL/biblioteca/web/", "./web/"]
COPY ["CONEXIÓN JAVA/Biblioteca_San_Lucas_Fase2_PostgreSQL/biblioteca/lib/postgresql-42.7.14.jar", "./lib/postgresql-42.7.14.jar"]
ENV BIBLIOTECA_MODO=postgresql \
    BIBLIOTECA_HOST=0.0.0.0 \
    BIBLIOTECA_PUBLICO=true \
    PORT=10000
EXPOSE 10000
CMD ["java", "-Dfile.encoding=UTF-8", "-cp", "biblioteca-web.jar:lib/*", "biblioteca.Main"]
