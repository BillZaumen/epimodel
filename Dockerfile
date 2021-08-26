FROM openjdk:11.0.12-jre

COPY . /usr/app

WORKDIR /usr/app

EXPOSE 80/tcp

ENV BACKLOG=30 NTHREADS=50 TRACE=false IPADDR=wildcard PORT=80

CMD ["java", "-jar", "epi.jar"]
