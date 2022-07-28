FROM workspace-java-17

#RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh && sdk install java 11.0.9-trava"
#RUN echo "JAVA_HOME=/home/gitpod/.sdkman/candidates/java/current" > ~/.mavenrc
ENV JAVA_HOME=/home/gitpod/.sdkman/candidates/java/current/bin/java
             
USER gitpod


# Install custom tools, runtime, etc. using apt-get
# For example, the command below would install "bastet" - a command line tetris clone:
#
# RUN sudo apt-get -q update && \
#     sudo apt-get install -yq bastet && \
#     sudo rm -rf /var/lib/apt/lists/*
#
# More information: https://www.gitpod.io/docs/42_config_docker/
