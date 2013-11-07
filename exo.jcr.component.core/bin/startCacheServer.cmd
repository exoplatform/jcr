 @echo off
 setLocal EnableDelayedExpansion
 set CACHE_SERVER_CP=".
 for /R ./lib %%a in (*.jar) do (
   set CACHE_SERVER_CP=!CACHE_SERVER_CP!;%%a
 )
 set CACHE_SERVER_CP=!CACHE_SERVER_CP!"

java -Djava.net.preferIPv4Stack=true -Xms128m -Xmx512m -cp %CACHE_SERVER_CP% org.exoplatform.services.jcr.infinispan.CacheServer %*
