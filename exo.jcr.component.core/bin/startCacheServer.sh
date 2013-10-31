#!/bin/sh
for i in lib/*.jar;do CACHE_SERVER_CP=$CACHE_SERVER_CP:$i;done
java -Djava.net.preferIPv4Stack=true -Xms128m -Xmx512m -cp .$CACHE_SERVER_CP org.exoplatform.services.jcr.infinispan.CacheServer $*
