#!/usr/bin/env sh

HomeDash_HOME=`cd $(dirname $0)/..; pwd`

JAVA_OPTS="${JAVA_OPTS} -DHomeDash.homeDir=${HomeDash_HOME}"

#############################################################
# Mysql configuration
#############################################################
if [[ $USE_MYSQL == true ]]; then
    JAVA_OPTS="${JAVA_OPTS} -DHomeDash.useMysql=true"
    JAVA_OPTS="${JAVA_OPTS} -DHomeDash.mysql.url=jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB_NAME}"
    JAVA_OPTS="${JAVA_OPTS} -DHomeDash.mysql.username=${MYSQL_USERNAME}"
    JAVA_OPTS="${JAVA_OPTS} -DHomeDash.mysql.password=${MYSQL_PASSWORD}"
fi

JAVA_OPTS="${JAVA_OPTS} -jar ${HomeDash_HOME}/lib/home-dash.jar"
JAVA_OPTS="${JAVA_OPTS} --spring.config.location=${HomeDash_HOME}/conf/application.yml"

if [ ! -d "${HomeDash_HOME}/data/logs" ]; then
    mkdir -p ${HomeDash_HOME}/data/logs
fi

java ${JAVA_OPTS}
