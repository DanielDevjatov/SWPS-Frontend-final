# Summary (FinalFinal): Added code to *.\r\n# Purpose: document changes and explain behavior.\r\nFROM gradle:latest

SHELL ["/bin/bash", "-o", "pipefail", "-c"]
ENV NVM_DIR=/root/.nvm

RUN apt-get update
RUN apt-get install jq npm python3-pip -y
RUN ln -sf python3 /usr/bin/python
RUN pip install anybadge --break-system-packages
RUN wget -O- https://carvel.dev/install.sh > install.sh
RUN bash install.sh

ENV NODE_VERSION=22.4.0
RUN curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.40.2/install.sh | bash
RUN source $NVM_DIR/nvm.sh && nvm install $NODE_VERSION && nvm use $NODE_VERSION

ENTRYPOINT ["bash", "-c", "source $NVM_DIR/nvm.sh && exec \"$@\"", "--"]

CMD ["/bin/bash"]

