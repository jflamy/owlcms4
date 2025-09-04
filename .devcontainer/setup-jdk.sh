#!/bin/bash

# JetBrains Runtime JDK 17 with DCEVM setup script for devcontainer
# Java 21 is already provided by the base devcontainer image for VS Code extension

set -e

echo "Setting up JetBrains JDK 17 with DCEVM for project runtime..."

# Update package lists
sudo apt-get update -q

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

# Install Maven separately to get latest version
echo "Installing Maven 3.9.6..."
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

# Create VS Code settings to use JDK 17 DCEVM for the project
echo "Setting up VS Code Java configuration..."
mkdir -p .vscode
cat > .vscode/settings.json << 'EOF'
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-17",
      "path": "/usr/local/jdk-17-dcevm",
      "default": true
    }
  ],
  "java.compile.nullAnalysis.mode": "automatic",
  "java.format.settings.url": "https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml",
  "maven.executable.path": "/opt/maven/bin/mvn",
  "terminal.integrated.defaultProfile.linux": "bash",
  "java.autobuild.enabled": true,
  "java.import.gradle.enabled": false,
  "java.configuration.updateBuildConfiguration": "interactive",
  "java.annotation.processing.enabled": false,
  "java.autobuild.annotation.processing.enabled": false,
  "debug.allowBreakpointsEverywhere": true,
  "debug.toolBarLocation": "docked"
}
EOF

# Verify installation
echo "Verifying Java installation..."
java -version
echo ""
echo "Verifying JDK 17 DCEVM installation..."
/usr/local/jdk-17-dcevm/bin/java -version
echo ""
echo "Verifying Maven installation..."
/opt/maven/bin/mvn -version

# Clean up
rm -f /tmp/jbr-dcevm.tar.gz /tmp/maven.tar.gz
rm -rf /tmp/jbr_jcef-17.0.14-b1367.22

echo ""
echo "Setup complete!"
echo "- Java 21: Provided by devcontainer base image (for VS Code Java Extension)"
echo "- JDK 17 DCEVM: /usr/local/jdk-17-dcevm (for project compilation and hot-swap debugging)"
echo "- Maven 3.9.6: /opt/maven (latest version)"
