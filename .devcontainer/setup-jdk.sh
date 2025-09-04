#!/bin/bash

# JetBrains Runtime JDK with DCEVM setup script for devcontainer

set -e

echo "Setting up JetBrains JDK with DCEVM and Java 21 for VS Code Extension..."

# Update package lists
sudo apt-get update

# Install Java 21 for VS Code Java Extension (required)
echo "Installing Java 21 for VS Code Java Extension..."
sudo apt-get install -y openjdk-21-jdk

# Create directory for JDK 17 DCEVM
sudo mkdir -p /usr/local/jdk-17-dcevm
cd /tmp

# Download JetBrains Runtime JDK with DCEVM (Java 17)
echo "Downloading JetBrains Runtime JDK 17 with DCEVM..."
wget -q --show-progress -O jbr-dcevm.tar.gz "https://cache-redirector.jetbrains.com/intellij-jbr/jbr_jcef-17.0.14-linux-x64-b1367.22.tar.gz"

# Extract the JDK
echo "Extracting JDK 17 DCEVM..."
tar -xzf jbr-dcevm.tar.gz

# Move to the correct location (JetBrains archives typically have a jbr directory)
sudo mv jbr_jcef-17.0.14-b1367.22/* /usr/local/jdk-17-dcevm/
sudo chown -R root:root /usr/local/jdk-17-dcevm

# Set up environment variables
echo "Setting up environment variables..."
# Java 21 as default JAVA_HOME for VS Code extension
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' | sudo tee -a /etc/environment
echo 'export PATH=$JAVA_HOME/bin:$PATH' | sudo tee -a /etc/environment

# Update current session
export PATH=$JAVA_HOME/bin:$PATH

# Add to bash profile for future sessions
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc

# Install Maven separately since we're overriding the default JDK
echo "Installing Maven..."
cd /tmp
wget -q --show-progress -O maven.tar.gz "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz"
sudo tar -xzf maven.tar.gz -C /opt
sudo ln -sf /opt/apache-maven-3.9.6 /opt/maven

# Set up Maven environment
echo 'export M2_HOME=/opt/maven' | sudo tee -a /etc/environment
echo 'export PATH=$M2_HOME/bin:$PATH' | sudo tee -a /etc/environment
export M2_HOME=/opt/maven
export PATH=$M2_HOME/bin:$PATH
echo 'export M2_HOME=/opt/maven' >> ~/.bashrc
echo 'export PATH=$M2_HOME/bin:$PATH' >> ~/.bashrc

# Verify installation
echo "Verifying Java 21 installation (for VS Code extension)..."
java -version

echo "Verifying JDK 17 DCEVM installation (for project)..."
/usr/local/jdk-17-dcevm/bin/java -version

echo "Verifying Maven installation..."
/opt/maven/bin/mvn -version

# Clean up
rm -f /tmp/jbr-dcevm.tar.gz /tmp/maven.tar.gz
rm -rf /tmp/jbr_jcef-17.0.14-b1367.22

echo "Setup complete!"
echo "- Java 21 (default): For VS Code Java Extension"
echo "- JDK 17 DCEVM: Available at /usr/local/jdk-17-dcevm for project runtime"
