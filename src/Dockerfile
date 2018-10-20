FROM anapsix/alpine-java
ADD build/libs/application.jar application.jar
COPY run.sh run.sh
RUN chmod +x run.sh
ENTRYPOINT ["./run.sh"]

