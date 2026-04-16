#!/usr/bin/env bash

pid=`ps aux | grep "home-dash-web.jar" | grep -v grep|awk '{print $2}'`
if [ -z $pid ]; then
    echo "no home-dash server running..."
    exit -1
fi

echo "kill home-dash server..."
kill $pid
echo "success kill server at pid:${pid}"
