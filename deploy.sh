#!/bin/bash
REPO="${1:-missing-repo-param}"
TAG="${2:-1.0}"
NAMESPACE="${3:-missing-namespace-param}"
mvn package

IMAGE_NAME=flink-workload-generator
IMAGE="${REPO}/${IMAGE_NAME}"

docker build -t "${IMAGE_NAME}:${TAG}" .
docker tag "${IMAGE_NAME}:${TAG}" "${IMAGE}:${TAG}"
docker push "${IMAGE}:${TAG}"

cd deploy
cp kustomization.template.yml kustomization.yml
yq -i "
  .namespace = ${NAMESPACE} |
  .images += {"newName": "${IMAGE}", "newTag" : "${TAG}"}
" kustomization.yaml

oc apply -k .
cd - || exit