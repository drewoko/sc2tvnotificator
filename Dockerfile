FROM java:8

MAINTAINER Deniss Gubanov <deniss@gubanov.ee>

RUN apt-get update  && \
    apt-get install haveged -y;

COPY target/sc2tvnotificator-exec.jar sc2tvnotificator-exec.jar

EXPOSE 8012/tcp

ENTRYPOINT /bin/bash -c 'java -jar sc2tvnotificator-exec.jar'