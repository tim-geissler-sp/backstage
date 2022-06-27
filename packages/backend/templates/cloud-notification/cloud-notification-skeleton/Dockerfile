FROM 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/jdk11_ubuntu_20_04:latest

EXPOSE 7100
EXPOSE 7200

ENV ATLAS_REDIS_HOST localhost
ENV ATLAS_REDIS_PORT 6379
ENV ATLAS_REDIS_POOL_SIZE 32
ENV ATLAS_PRODUCTION true
ENV ATLAS_DATA_PATH /hermes-server/tmp
ENV ATLAS_ENFORCE_TRANSACTION_BOUNDARIES true
ENV IRIS_KAFKA_ROUND_ROBIN true

ENV DEFAULT_JAVA_OPTS=""
ENV JAVA_OPTS=""
ENV HERMES_SERVER_OPTS="-Dlog4j.configurationFile=log4j2-production.xml"

COPY hermes-server /hermes-server
RUN chmod a+x /hermes-server/build/install/hermes-server

WORKDIR /hermes-server/build/install/hermes-server/bin

RUN groupadd -r javauser && useradd -r -s /bin/false -g javauser javauser
RUN chown -R javauser:javauser /hermes-server
USER javauser

CMD ["./hermes-server"]
