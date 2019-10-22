FROM python:3.5

RUN pip install wes-service

RUN pip install cwltool

RUN pip install boto
RUN pip install pandas
RUN pip install docker
RUN pip install synapseclient

# Put the below exports in a .bash_profile/rc
# export WES_API_HOST=localhost:8082
# export WES_API_AUTH='Header: value'  
# export WES_API_PROTO=http
RUN echo -e "\nWES_API_HOST=localhost:8082\nWES_API_AUTH='Header: value'\nWES_API_PROTO=http\n" >> ~/.profile

#install nodejs, to provide in line javascript evaluation
# RUN apt-get install -y nodejs npm
RUN curl -sL https://deb.nodesource.com/setup_6.x | bash -
RUN apt-get install -y nodejs
RUN apt-get install -y npm

CMD wes-server --backend=wes_service.cwl_runner --opt runner=cwltool --opt extra=--cachedir=/root/ --port 8082

# Notes:
#
# to build:
# docker build -f Dockerfile.WES -t sagebionetworks/synapse-workflow-orchestrator-wes .
#
# to run:
# docker run --name wes --rm -it -p 8082:8082 -v $(pwd)/shared:/shared:ro sagebionetworks/synapse-workflow-orchestrator-wes
#
#
