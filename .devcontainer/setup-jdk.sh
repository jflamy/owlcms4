#!/bin/bash

# JetBrains Runtime JDK with DCEVM setup script for devcontainer

set -e

echo "Setting up JetBrains JDK with DCEVM and Java 21 for VS Code Extension..."

# Update package lists
sudo apt-get update

# Install Java 21 for VS Code Java Extension (required)
echo "Installing Java 21 for VS Code Java Extension..."
sudo apt-get install -y openjdk-21-jdk

# Find the actual Java 21 installation path
JAVA21_PATH=$(find /usr/lib/jvm -name "*java-21-openjdk*" -type d | head -1)
echo "Java 21 installed at: $JAVA21_PATH"

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
export JAVA_HOME=$JAVA21_PATH
echo "export JAVA_HOME=$JAVA21_PATH" | sudo tee -a /etc/environment
echo 'export PATH=$JAVA_HOME/bin:$PATH' | sudo tee -a /etc/environment

# Update current session
export PATH=$JAVA_HOME/bin:$PATH

# Add to bash profile for future sessions
echo "export JAVA_HOME=$JAVA21_PATH" >> ~/.bashrc
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

# Update VS Code settings with actual Java paths
echo "Updating VS Code settings with discovered Java paths..."
cat > /tmp/vscode-settings.json << EOF
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "$JAVA21_PATH",
      "default": false
    },
    {
      "name": "JavaSE-17",
      "path": "/usr/local/jdk-17-dcevm",
      "default": true
    }
  ],
  "java.jdt.ls.java.home": "$JAVA21_PATH",
  "java.compile.nullAnalysis.mode": "automatic",
  "java.format.settings.url": "https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml",
  "maven.executable.path": "/opt/maven/bin/mvn",
  "terminal.integrated.defaultProfile.linux": "bash",
  "java.configuration.workspaceFolder": "/workspaces/owlcms_v23",
  "java.autobuild.enabled": true,
  "java.import.gradle.enabled": false,
  "java.configuration.updateBuildConfiguration": "interactive",
  "java.annotation.processing.enabled": false,
  "java.autobuild.annotation.processing.enabled": false,
  "debug.allowBreakpointsEverywhere": true,
  "debug.toolBarLocation": "docked"
}
EOF

# Copy the updated settings
mkdir -p .vscode
cp /tmp/vscode-settings.json .vscode/settings.json

echo "Setup complete!"
echo "- Java 21 (default): $JAVA21_PATH - For VS Code Java Extension"
echo "- JDK 17 DCEVM: /usr/local/jdk-17-dcevm - For project runtime"
