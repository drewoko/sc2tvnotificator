FROM java:8

MAINTAINER Deniss Gubanov <deniss@gubanov.ee>

RUN apt-get update  && \
    apt-get install haveged git maven -y && \
    git clone https://github.com/drewoko/sc2tvnotificator.git && \
    cd sc2tvnotificator

EXPOSE 8012/tcp

ENTRYPOINT /bin/bash -c 'cd sc2tvnotificator && mvn clean install && mvn spring-boot:run'