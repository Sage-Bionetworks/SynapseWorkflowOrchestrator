FROM python:3.6

# install toil
RUN apt-get update && apt-get install -y libssl-dev libffi-dev
RUN pip3 install toil[all]
RUN pip3 install boto
RUN pip3 install pandas
RUN pip3 install docker
RUN pip3 install synapseclient
# now install docker, following the instructions here: https://docs.docker.com/install/linux/docker-ce/ubuntu/#install-docker-ce-1
RUN apt-get update   

RUN apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    software-properties-common

RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg |  apt-key add -

RUN add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/debian \
   $(lsb_release -cs) \
   stable"

RUN apt-get update   

RUN apt-get install -y docker-ce

#install nodejs, to provide in line javascript evaluation
# RUN apt-get install -y nodejs npm
RUN curl -sL https://deb.nodesource.com/setup_6.x | sudo bash -
RUN apt-get install -y nodejs
RUN apt-get install -y npm

RUN npm install js-yaml

RUN echo -e "\numask 000\n" >> ~/.profile
