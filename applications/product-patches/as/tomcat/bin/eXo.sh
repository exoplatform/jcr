#!/bin/sh

PRG="$0"

PRGDIR=`dirname "$PRG"`
LOG_OPTS="-Dorg.exoplatform.services.log.Log=org.apache.commons.logging.impl.SimpleLog"
SECURITY_OPTS="-Djava.security.auth.login.config=$PRGDIR/../conf/jaas.conf"
EXO_OPTS="-Dexo.product.developing=true -Dexo.profiles=$1 -Djava.net.preferIPv4Stack=true"
if [ "$1" = "ispn" ]; then
    shift
fi

JAVA_OPTS="$JAVA_OPTS $LOG_OPTS $SECURITY_OPTS $EXO_OPTS"
export JAVA_OPTS
exec "$PRGDIR"/catalina.sh "$@"
