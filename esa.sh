#!/bin/bash
java -Djdbc.drivers=org.postgresql.Driver -DtotalEntitySizeLimit=2147480000 -Djdk.xml.totalEntitySizeLimit=2147480000 -jar target/esa-1.0-SNAPSHOT-jar-with-dependencies.jar "$@"