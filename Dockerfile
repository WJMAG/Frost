FROM gcr.io/distroless/java21-debian12

WORKDIR /app
COPY ./.run/Frost.jar /app/

CMD ["Frost.jar"]