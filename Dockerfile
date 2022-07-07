FROM registry.access.redhat.com/ubi8/openjdk-11:1.13-1.1655306377
USER root
RUN mkdir /temp
WORKDIR /opt/ibm
COPY target/kafka-sample-runner.jar .
RUN chgrp -R 0 /temp && chmod -R g+rwX /temp
CMD java -Djava.io.tmpdir=/temp -jar kafka-sample-runner.jar > /opt/ibm/files/messages.json