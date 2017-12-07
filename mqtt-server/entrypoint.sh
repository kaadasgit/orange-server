#!/bin/sh

if [ -z "${JMX_HOST}" ]
then
    java -jar -XX:+ExitOnOutOfMemoryError -XX:+UseG1GC mqtt-broker.jar $@
else
    java -jar -XX:+ExitOnOutOfMemoryError -XX:+UseG1GC \
        -Dcom.sun.management.jmxremote \
        -Dcom.sun.management.jmxremote.port=${JMX_PORT:-9007} \
        -Dcom.sun.management.jmxremote.local.only=false \
        -Dcom.sun.management.jmxremote.authenticate=false \
        -Dcom.sun.management.jmxremote.ssl=false \
        -Djava.rmi.server.hostname=${JMX_HOST} \
        mqtt-broker.jar $@
fi