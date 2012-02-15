#!/bin/sh
export JPDA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=18000,server=y,suspend=y"

PRGDIR=`dirname "$PRG"`

if [ -f eXo-jbc.sh ]; then
  exec "$PRGDIR"/eXo-jbc.sh "$@"
else if [ -f eXo-ispn.sh ]; then
  exec "$PRGDIR"/eXo-ispn.sh "$@"
     fi
fi

