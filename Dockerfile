FROM registry.access.redhat.com/ubi8/openjdk-11:1.13-1.1655306377
WORKDIR /opt/ibm
COPY target/es-producer.jar .
RUN mkdir temp
CMD java -Djava.io.tmpdir=./temp -jar es-producer.jar