FROM maven:3.6-jdk-11-slim AS builder

COPY . /app
WORKDIR /app

RUN mvn install


FROM openjdk:11-jdk-slim AS runner

RUN apt-get update && apt-get install -y curl nano htop

WORKDIR /app
COPY --from=builder /app/bot/bot.jar ./

ENTRYPOINT java -jar /app/bot.jar /app/dataset
