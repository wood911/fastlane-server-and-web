FROM gcr.io/distroless/java11
MAINTAINER woodtengfei@gmail.com
#ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.21.0.9-2.el8.x86_64
#ENV JRE_HOME=$JAVA_HOME/jre
#ENV CLASS_PATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar:$JRE_HOME/lib
#ENV PATH=$PATH:$JAVA_HOME/bin:$JRE_HOME/bin
#VOLUME /usr/lib/jvm
ARG JAR_FILE=*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8095
ENTRYPOINT ["java","${JAVA_OPTS}","-jar","/app.jar"]


