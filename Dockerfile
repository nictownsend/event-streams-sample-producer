FROM registry.access.redhat.com/ubi8/openjdk-11:1.13-1.1655306377
ARG JAR
USER root
RUN mkdir /temp
WORKDIR /opt/ibm
COPY target/$JAR .
RUN chgrp -R 0 /temp && chmod -R g+rwX /temp
CMD java -Djava.io.tmpdir=/temp -jar $JAR