#!/usr/bin/env sh

HomeDash_HOME=`cd $(dirname $0)/..; pwd`

JAVA_OPTS="${JAVA_OPTS} -DHomeDash.homeDir=${HomeDash_HOME}"
JAVA_OPTS="${JAVA_OPTS} -jar ${HomeDash_HOME}/lib/home-dash.jar"
JAVA_OPTS="${JAVA_OPTS} --spring.config.location=${HomeDash_HOME}/conf/application.yml"

if [[ ! -d "${HomeDash_HOME}/data/logs" ]]; then
    mkdir -p ${HomeDash_HOME}/data/logs
fi

nohup java ${JAVA_OPTS} >> ${HomeDash_HOME}/data/logs/home-dash.log 2>&1 &
echo "started home-dash!!!"
#java ${JAVA_OPTS}
