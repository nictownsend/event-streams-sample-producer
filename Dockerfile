FROM registry.access.redhat.com/ubi8/openjdk-11:1.13-1.1655306377
WORKDIR /opt/ibm
COPY target/kafka-sample-producer.jar .
CMD mkdir /temp
CMD java -Djava.io.tmpdir=/temp -jar kafka-sample-producer.jar