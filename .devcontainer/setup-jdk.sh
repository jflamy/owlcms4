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

# Check if we have a nested tar file
if [ -f *.tar ]; then
    echo "Found nested tar file, extracting..."
    tar -xf *.tar
    rm -f *.tar
fi

# Find the actual directory name and move contents
JBR_DIR=$(find . -maxdepth 1 -name "jbr*" -type d | head -n 1)
if [ -n "$JBR_DIR" ]; then
    echo "Found JBR directory: $JBR_DIR"
    sudo mv "$JBR_DIR"/* /usr/local/jdk-17-dcevm/
    sudo chown -R root:root /usr/local/jdk-17-dcevm
    sudo chmod -R 755 /usr/local/jdk-17-dcevm
    sudo chmod +x /usr/local/jdk-17-dcevm/bin/*
else
    echo "JBR directory not found, listing contents for debugging..."
    ls -la
    exit 1
fi

# Install / configure Hotswap Agent (needed for -XX:HotswapAgent=fatjar)
echo "Installing Hotswap Agent..."

# Allow override of version via environment variable HOTSWAP_AGENT_VERSION.
# Use 'latest' (default) to query GitHub Releases API for newest version.
HOTSWAP_AGENT_VERSION=${HOTSWAP_AGENT_VERSION:-latest}
DEST_DIR="/usr/local/jdk-17-dcevm/lib/hotswap"
sudo mkdir -p "$DEST_DIR"

resolve_hotswap_url() {
  local version="$1"
  local url=""
  if [ "$version" = "latest" ]; then
    if command -v curl >/dev/null 2>&1; then
      echo "Querying GitHub API for latest Hotswap Agent release..."
      url=$(curl -s https://api.github.com/repos/HotswapProjects/HotswapAgent/releases/latest \
        | grep -E '"browser_download_url"' \
        | grep -E 'hotswap-agent-[0-9].*\.jar' \
        | head -n1 \
        | cut -d '"' -f 4)
    else
      echo "curl not found; falling back to fixed version 2.0.1"
      version="2.0.1"
    fi
  fi
  if [ -z "$url" ]; then
    # If version is still latest but API call failed or produced no match, fallback
    if [ "$version" = "latest" ]; then
      version="2.0.1"
    fi
    url="https://repo1.maven.org/maven2/org/hotswapagent/hotswap-agent/${version}/hotswap-agent-${version}.jar"
  fi
  echo "$url"
}

HOTSWAP_URL=$(resolve_hotswap_url "$HOTSWAP_AGENT_VERSION")
echo "Downloading Hotswap Agent from: $HOTSWAP_URL"
if ! wget -q -O /tmp/hotswap-agent-dl.jar "$HOTSWAP_URL"; then
  echo "Download failed; aborting Hotswap Agent installation." >&2
else
  sudo mv /tmp/hotswap-agent-dl.jar "$DEST_DIR/hotswap-agent.jar"
  sudo chmod 644 "$DEST_DIR/hotswap-agent.jar"
  echo "Hotswap Agent installed at $DEST_DIR/hotswap-agent.jar"
fi

echo "To pin a specific version, set HOTSWAP_AGENT_VERSION (e.g., HOTSWAP_AGENT_VERSION=2.0.1)."

# Install Maven separately to get latest version
echo "Installing Maven 3.9.6..."
cd /tmp
wget -q --show-progress -O maven.tar.gz "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz"
sudo tar -xzf maven.tar.gz -C /opt
sudo ln -sf /opt/apache-maven-3.9.6 /opt/maven

# Set up Maven environment and configure it to use JDK 17
echo 'export M2_HOME=/opt/maven' | sudo tee -a /etc/environment
echo 'export PATH=$M2_HOME/bin:$PATH' | sudo tee -a /etc/environment
export M2_HOME=/opt/maven
export PATH=$M2_HOME/bin:$PATH
echo 'export M2_HOME=/opt/maven' >> ~/.bashrc
echo 'export PATH=$M2_HOME/bin:$PATH' >> ~/.bashrc

# Create Maven toolchains.xml to specify JDK 17 for compilation
echo "Configuring Maven to use JDK 17 for compilation..."
mkdir -p ~/.m2
cat > ~/.m2/toolchains.xml << 'EOF'
<?xml version="1.0" encoding="UTF8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
      <vendor>jetbrains</vendor>
    </provides>
    <configuration>
      <jdkHome>/usr/local/jdk-17-dcevm</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
EOF

# Also create a Maven settings.xml to ensure consistent Java home for Maven operations
cat > ~/.m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <profiles>
    <profile>
      <id>jdk-17</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <maven.compiler.release>17</maven.compiler.release>
        <java.home>/usr/local/jdk-17-dcevm</java.home>
      </properties>
    </profile>
  </profiles>
</settings>
EOF

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
rm -rf /tmp/jbr*

echo ""
echo "Setup complete!"
echo "- Java 21: Provided by devcontainer base image (for VS Code Java Extension)"
echo "- JDK 17 DCEVM: /usr/local/jdk-17-dcevm (for project compilation and hot-swap debugging)"
echo "- Maven 3.9.6: /opt/maven (latest version)"
