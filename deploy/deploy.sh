#!/usr/bin/env bash
source "$(cd $(dirname $0) && pwd)/env.sh"

BUCKET_SUFFIX="-development"
if [[ "$BP_MODE" = "production" ]]; then
    BUCKET_SUFFIX=""
fi

export BP_MODE="development"
if [ "$GITHUB_EVENT_NAME" = "create" ]; then
  if [[ "${GITHUB_REF}" =~ "tags" ]]; then
    BP_MODE="production"
  fi
fi

echo "BP_MODE=${BP_MODE}"

mvn -Dspring.profiles.active=ci verify deploy || die "could not build and deploy the artifact to Artifactory."

ROUTE_HOSTNAME=bootiful-podcast-search-api
APP_NAME=api
if [[ "$BP_MODE" = "development" ]]; then
    APP_NAME=${APP_NAME}-${BP_MODE}
    ROUTE_HOSTNAME=${ROUTE_HOSTNAME}-development
fi


cf push -k 2GB -m 2GB -b java_buildpack --no-start --no-route -p target/api-0.0.1-SNAPSHOT.jar ${APP_NAME}
cf set-env $APP_NAME JBP_CONFIG_OPEN_JDK_JRE '{ jre: { version: 11.+}}'
cf set-env $APP_NAME BP_MODE $BP_MODE
cf set-env $APP_NAME SPRING_PROFILES_ACTIVE cloud
cf routes | grep ${ROUTE_HOSTNAME} || cf create-route bootiful-podcast cfapps.io --hostname ${ROUTE_HOSTNAME}
cf map-route $APP_NAME cfapps.io --hostname $ROUTE_HOSTNAME



## We need to correctly bind either the DEV or the PROD PWS services
SVC_SUFFIX=""
if [[  "$BP_MODE" = "development"  ]]; then
 SVC_SUFFIX="-dev"
fi
DB_SVC_NAME=bootiful-podcast-db${SVC_SUFFIX}
MQ_SVC_NAME=bootiful-podcast-mq${SVC_SUFFIX}
cf bs ${APP_NAME} ${MQ_SVC_NAME}
cf bs ${APP_NAME} ${DB_SVC_NAME}

cf restart $APP_NAME

