FROM gcr.io/distroless/java21-debian12

WORKDIR /app
COPY ./.run/ServerImpl-1.0-SNAPSHOT.jar /app

CMD ["ServerImpl-1.0-SNAPSHOT.jar"]