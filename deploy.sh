#!/bin/bash
REPO="${1}"
TAG="${2}"
IMAGE_NAME="${3}"
NAMESPACE="${4}"

[[ $# -ne 3 ]] && echo "err: Expected 4 parameters received $#" && exit 1
[[ -z $REPO ]] && echo "err: Missing repo" && exit 1
[[ -z $TAG ]] && echo "err: Missing tag" && exit 1
[[ -z $IMAGE_NAME ]] && echo "err: Missing image name" && exit 1
[[ -z $NAMESPACE ]] && echo "err: Missing namespace" && exit 1

IMAGE="${REPO}/${IMAGE_NAME}"

mvn package
docker build -t "${IMAGE_NAME}:${TAG}" --build-arg "JAR=flink-workload-generator.jar" .
docker tag "${IMAGE_NAME}:${TAG}" "${IMAGE}:${TAG}"
docker push "${IMAGE}:${TAG}"

cd deployment
cp kustomization.template.yml kustomization.yml
yq -i "
  .namespace = ${NAMESPACE} |
  .images += {"newName": \"${IMAGE}\", "newTag" : \"${TAG}\"}
" kustomization.yaml

kubectl apply -k .
cd - || exit