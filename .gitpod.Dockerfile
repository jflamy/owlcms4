FROM gitpod/workspace-full
             
RUN mkdir -p /home/gitpod/lib/java && \
    cd /home/gitpod/lib/java && \
    curl -L -O https://github.com/TravaOpenJDK/trava-jdk-11-dcevm/releases/download/dcevm-11.0.5%2B5/java11-openjdk-dcevm-linux.tar.gz  && \
    tar xzf java11-openjdk-dcevm-linux.tar.gz 

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh && sdk install java 11.0.5.hs-dcevm /home/gitpod/lib/java/dcevm-11.0.5+5 && sdk default java 11.0.5.hs-dcevm"
             
USER gitpod


# Install custom tools, runtime, etc. using apt-get
# For example, the command below would install "bastet" - a command line tetris clone:
#
# RUN sudo apt-get -q update && \
#     sudo apt-get install -yq bastet && \
#     sudo rm -rf /var/lib/apt/lists/*
#
# More information: https://www.gitpod.io/docs/42_config_docker/
