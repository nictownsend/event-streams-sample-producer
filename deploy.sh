#!/bin/bash
TAG="${1:-1.0}"
#TODO template in the repo and image tag
mvn clean install
docker build -t "localhost:5000/kafka-sample-producer:${TAG}" .
docker tag "localhost:5000/kafka-sample-producer:${TAG}" "quay.io/nictownsend/kafka-sample-producer:${TAG}"
docker push "quay.io/nictownsend/kafka-sample-producer:${TAG}"
oc delete -f deployment.yml
oc delete kt txns txn_count
oc apply -f deployment.yml