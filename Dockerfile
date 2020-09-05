FROM openjdk:11.0.8-jre

COPY . /usr/app

WORKDIR /usr/app

EXPOSE 80/tcp

ENV BACKUP=30 NTHREADS=50 TRACE=false PORT=80

CMD ["java", "-jar", "epi.jar"]
