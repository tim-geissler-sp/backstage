FROM 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/jdk11_ubuntu_20_04:latest

WORKDIR /ets-server

COPY /ets-server/build/libs/ets-server.jar ./app.jar

EXPOSE 7100
EXPOSE 7200

ENV SERVER_PORT="7100"
ENV SPRING_PROFILES_ACTIVE="production"
ENV ATLAS_PRODUCTION="true"

ENV DEFAULT_JAVA_OPTS="-Xmx2g -Xms2g -Dfile.encoding=UTF-8"
ENV JAVA_OPTS=""
ENV ETS_SERVER_OPTS=""

RUN groupadd -r javauser && useradd -r -s /bin/false -g javauser javauser
RUN chown -R javauser:javauser /ets-server
USER javauser

ENTRYPOINT exec java $DEFAULT_JAVA_OPTS $ETS_SERVER_OPTS $JAVA_OPTS -jar app.jar
