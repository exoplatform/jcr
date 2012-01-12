EXO_PROFILES="-Dexo.profiles="
if [ "$1" = "jbc" ] || [ "$1" = "ispn" ]; then
    EXO_PROFILES="$EXO_PROFILES,$1"
    shift 1
else
    EXO_PROFILES="$EXO_PROFILES,jbc"
fi
EXO_OPTS="-Dexo.product.developing=true $EXO_PROFILES -Djava.net.preferIPv4Stack=true"

java -Djava.security.auth.login.config=jaas.conf $EXO_PROFILES -Djava.net.preferIPv4Stack=true -Dexo.jcr.parent.dir=. -jar start.jar
