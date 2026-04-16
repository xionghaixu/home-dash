#!/usr/bin/env sh

PND_HOME=`cd $(dirname $0)/..; pwd`

JAVA_OPTS="${JAVA_OPTS} -Dpnd.homeDir=${PND_HOME}"
JAVA_OPTS="${JAVA_OPTS} -jar ${PND_HOME}/lib/home-dash.jar"
JAVA_OPTS="${JAVA_OPTS} --spring.config.location=${PND_HOME}/conf/application.yml"

if [[ ! -d "${PND_HOME}/data/logs" ]]; then
    mkdir -p ${PND_HOME}/data/logs
fi

nohup java ${JAVA_OPTS} >> ${PND_HOME}/data/logs/home-dash.log 2>&1 &
echo "started home-dash!!!"
#java ${JAVA_OPTS}
