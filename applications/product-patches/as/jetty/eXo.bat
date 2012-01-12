set EXO_PROFILES=-Dexo.profiles=
if ""%1"" == ""jbc"" goto profile
if ""%1"" == ""ispn"" goto profile
goto default_profile

:profile
set EXO_PROFILES=%EXO_PROFILES%,%1
shift
goto endif

:default_profile
set EXO_PROFILES=%EXO_PROFILES%,jbc
goto endif

:endif

java -Djava.security.auth.login.config=jaas.conf %EXO_PROFILES% -Djava.net.preferIPv4Stack=true -Dexo.jcr.parent.dir=. -jar start.jar
