FROM gcr.io/distroless/java21-debian12

WORKDIR /app
COPY ./.run/Frost-1.0-SNAPSHOT.jar /app

USER nonroot

CMD ["Frost-1.0-SNAPSHOT.jar"]