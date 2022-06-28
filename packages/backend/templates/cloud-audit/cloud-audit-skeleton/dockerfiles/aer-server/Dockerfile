FROM 406205545357.dkr.ecr.us-east-1.amazonaws.com/sailpoint/open_jdk8_ubuntu_1604:open-jdk8-latest

EXPOSE 7100
EXPOSE 7200

ENV ATLAS_REDIS_HOST localhost
ENV ATLAS_REDIS_PORT 6379
ENV ATLAS_REDIS_POOL_SIZE 32
ENV ATLAS_PODS dev

COPY aer-server /aer-server
RUN chmod a+x /aer-server/bin/aer-server

ENV ATLAS_PRODUCTION true
ENV ATLAS_DATA_PATH /aer-server/tmp
ENV ATLAS_ENFORCE_TRANSACTION_BOUNDARIES true
ENV AER_S3_AUDIT_PERSISTENCE_BUCKET aer-audit-events-by-time

WORKDIR /aer-server/bin

#  !Important :
#    options env variable need to be [{exe}.toUpper().replace('-','_')]_OPTS
ENV AER_SERVER_OPTS "-Dlog4j.configurationFile=log4j2-production.xml"

CMD ./aer-server
