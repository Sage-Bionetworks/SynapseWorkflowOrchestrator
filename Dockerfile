FROM maven:3-jdk-11
COPY pom.xml *.sh /
COPY src /src
RUN mvn clean install
RUN echo -e "\numask 000\n" >> ~/.profile
CMD exec mvn exec:java -DentryPoint=org.sagebionetworks.WorkflowOrchestrator
