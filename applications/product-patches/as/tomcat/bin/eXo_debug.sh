#!/bin/sh
export JPDA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=18000,server=y,suspend=y"

PRGDIR=`dirname "$PRG"`
exec "$PRGDIR"/eXo.sh "$@"
