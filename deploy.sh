#!/bin/bash
REPO="${1}"
TAG="${2}"
NAMESPACE="${3}"

[[ $# -ne 3 ]] && echo "err: Expected 3 parameters received $#" && exit 1
[[ -z $REPO ]] && echo "err: Missing repo" && exit 1
[[ -z $TAG ]] && echo "err: Missing tag" && exit 1
[[ -z $NAMESPACE ]] && echo "err: Missing namespace" && exit 1

IMAGE_NAME=flink-workload-generator
IMAGE="${REPO}/${IMAGE_NAME}"

#mvn package
#docker build -t "${IMAGE_NAME}:${TAG}" --build-arg "JAR=${IMAGE_NAME}.jar" .
#docker tag "${IMAGE_NAME}:${TAG}" "${IMAGE}:${TAG}"
#docker push "${IMAGE}:${TAG}"

cd deployment
cp kustomization.template.yml kustomization.yml
yq -i "
  .namespace = ${NAMESPACE} |
  .images += {"newName": \"${IMAGE}\", "newTag" : \"${TAG}\"}
" kustomization.yaml

oc apply -k .
cd - || exit