FROM eclipse-temurin:21-jre-alpine

WORKDIR /home-dash
COPY . /home-dash

RUN echo "Asia/Shanghai" > /etc/timezone

EXPOSE 8989
VOLUME /home-dash/data
ENTRYPOINT ["bin/docker-startup.sh"]
