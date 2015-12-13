FROM java:8

MAINTAINER Deniss Gubanov <deniss@gubanov.ee>

RUN apt-get update  && \
    apt-get install haveged git -y && \
    git clone git@github.com:drewoko/sc2tvnotificator.git && \
    cd sc2tvnotificator

ENTRYPOINT ["mvn spring-boot:run"]