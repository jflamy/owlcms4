#!/bin/bash

# JetBrains Runtime JDK 17 with DCEVM setup script for devcontainer
# Java 21 is already provided by the base devcontainer image for VS Code extension
# but project runtime uses JDK 17 DCEVM for enhanced class redefinition.

set -e

echo "Setting up JetBrains JDK 17 with DCEVM for project runtime..."

# Idempotency: if JDK already present, skip download section
if [ -d /usr/local/jdk-17-dcevm/bin ]; then
  echo "JDK 17 DCEVM already installed. Skipping JDK download/extract."
else

# Update package lists
sudo apt-get update -q

# Create directory for JDK 17 DCEVM
sudo mkdir -p /usr/local/jdk-17-dcevm
cd /tmp

  # Allow override of JBR version via env var (exact filename fragment). Default pinned for reproducibility.
  JBR_VERSION_FRAG=${JBR_VERSION_FRAG:-"jbr_jcef-17.0.14-linux-x64-b1367.22"}
  JBR_ARCHIVE_URL="https://cache-redirector.jetbrains.com/intellij-jbr/${JBR_VERSION_FRAG}.tar.gz"
  echo "Downloading JetBrains Runtime JDK 17 with DCEVM from: $JBR_ARCHIVE_URL"
  wget -q --show-progress -O jbr-dcevm.tar.gz "$JBR_ARCHIVE_URL"

  echo "Extracting JDK 17 DCEVM..."
  tar -xzf jbr-dcevm.tar.gz

  # Handle possible nested tar
  inner_tar=$(find . -maxdepth 1 -type f -name "jbr*.tar" | head -n1 || true)
  if [ -n "$inner_tar" ]; then
    echo "Found nested tar: $inner_tar — extracting..."
    tar -xf "$inner_tar"
    rm -f "$inner_tar"
  fi

  # Locate extracted directory
  JBR_DIR=$(find . -maxdepth 1 -type d -name "jbr*" | head -n1)
  if [ -z "$JBR_DIR" ]; then
    echo "ERROR: Could not locate extracted JBR directory. Contents:" >&2
    ls -la
    exit 1
  fi
  echo "Using extracted directory: $JBR_DIR"

  sudo mkdir -p /usr/local/jdk-17-dcevm
  sudo cp -r "$JBR_DIR"/* /usr/local/jdk-17-dcevm/
  sudo chown -R root:root /usr/local/jdk-17-dcevm
  # Set proper permissions: read/traverse for all, write for owner, execute for executables
  sudo chmod -R 755 /usr/local/jdk-17-dcevm
  rm -rf jbr* *.tar.gz
  echo "JDK 17 DCEVM installed to /usr/local/jdk-17-dcevm"
fi

# Ensure executables keep execute bits even on reused volumes (idempotent safety)
if [ -d /usr/local/jdk-17-dcevm/bin ]; then
  MISSING_EXEC=$(find /usr/local/jdk-17-dcevm/bin -maxdepth 1 -type f ! -perm -111 | head -n1 || true)
  if [ -n "$MISSING_EXEC" ]; then
    echo "Repairing execute permissions in /usr/local/jdk-17-dcevm/bin ..."
    sudo find /usr/local/jdk-17-dcevm/bin -type f -exec chmod 755 {} +
  fi
fi

# Install / configure Hotswap Agent (needed for -XX:HotswapAgent=fatjar)
echo "Installing Hotswap Agent (idempotent)..."

# Allow override of version via environment variable HOTSWAP_AGENT_VERSION.
# Use 'latest' (default) to query GitHub Releases API for newest version.
HOTSWAP_AGENT_VERSION=${HOTSWAP_AGENT_VERSION:-latest}
DEST_DIR="/usr/local/jdk-17-dcevm/lib/hotswap"
sudo mkdir -p "$DEST_DIR"

resolve_hotswap_url() {
  local version="$1"
  local DEFAULT_VERSION="2.0.1"
  local url=""

  if [ "$version" = "latest" ]; then
    if command -v curl >/dev/null 2>&1; then
      echo "Querying GitHub Releases API for latest Hotswap Agent..." >&2
      local api_json candidates chosen ver extracted_min
      # Query all releases instead of just /latest to find the truly newest version
      api_json=$(curl -s -H "Accept: application/vnd.github.v3+json" https://api.github.com/repos/HotswapProjects/HotswapAgent/releases 2>/dev/null || true)
      # Validate we got JSON response
      if ! echo "$api_json" | grep -q '"browser_download_url"'; then
        echo "Invalid GitHub API response; falling back to pinned ${DEFAULT_VERSION}" >&2
        version="$DEFAULT_VERSION"
      elif echo "$api_json" | grep -qi "API rate limit exceeded"; then
        echo "GitHub API rate limit exceeded; falling back to pinned ${DEFAULT_VERSION}" >&2
        version="$DEFAULT_VERSION"
      else
        # Get all non-prerelease releases and find assets
        candidates=$(echo "$api_json" | jq -r '.[] | select(.prerelease == false) | .assets[] | select(.name | test("hotswap-agent-.*\\.jar$") and (test("sources|javadoc") | not)) | .browser_download_url' 2>/dev/null || echo "$api_json" | grep -E '"browser_download_url"' | grep 'hotswap-agent-' | grep '.jar"' | grep -v -E '(sources|javadoc)' | cut -d '"' -f 4)
        # Pick the first candidate whose version is >= DEFAULT_VERSION (simple numeric compare stripping non-digits/dots)
        for c in $candidates; do
          ver=$(echo "$c" | sed -E 's#.*/hotswap-agent-([^/]+)\.jar#\1#')
          # Normalize version (strip leading 'RELEASE-' or 'v')
          ver=$(echo "$ver" | sed -E 's/^(RELEASE-|v)//')
          # Compare by removing dots (fallback heuristic)
          if [ -z "$chosen" ]; then
            chosen="$c"; extracted_min="$ver"
          fi
          if [ "$(echo "$ver" | tr -d '.')" -ge "$(echo "$DEFAULT_VERSION" | tr -d '.')" ]; then
            chosen="$c"; extracted_min="$ver"; break
          fi
        done
        if [ -n "$chosen" ]; then
          url="$chosen"
          echo "Selected Hotswap Agent asset: $url (version $extracted_min)" >&2
          # Enforce minimum version
          if [ "$(echo "$extracted_min" | tr -d '.')" -lt "$(echo "$DEFAULT_VERSION" | tr -d '.')" ]; then
            echo "Chosen version $extracted_min is below minimum $DEFAULT_VERSION; using pinned." >&2
            url=""
            version="$DEFAULT_VERSION"
          fi
        else
          echo "No suitable binary asset found; falling back to pinned ${DEFAULT_VERSION}" >&2
          version="$DEFAULT_VERSION"
        fi
      fi
    else
      echo "curl not available; using pinned ${DEFAULT_VERSION}" >&2
      version="$DEFAULT_VERSION"
    fi
  fi

  if [ -z "$url" ]; then
    # Either a specific version was requested or latest fallback selected.
    if [ "$version" = "latest" ]; then
      version="$DEFAULT_VERSION"
    fi
    url="https://github.com/HotswapProjects/HotswapAgent/releases/download/${version}/hotswap-agent-${version}.jar"
  fi

  # Emit only the URL on stdout
  echo "$url"
}

if [ -f "$DEST_DIR/hotswap-agent.jar" ]; then
  echo "Hotswap Agent already present at $DEST_DIR/hotswap-agent.jar (skipping download)."
else
  HOTSWAP_URL=$(resolve_hotswap_url "$HOTSWAP_AGENT_VERSION")
  echo "Resolved Hotswap Agent URL: $HOTSWAP_URL"
  if [ -z "$HOTSWAP_URL" ]; then
    echo "ERROR: Could not resolve a Hotswap Agent download URL." >&2
  elif ! wget -q -O /tmp/hotswap-agent-dl.jar "$HOTSWAP_URL"; then
    echo "Download failed for $HOTSWAP_URL; aborting Hotswap Agent installation." >&2
  else
    sudo mv /tmp/hotswap-agent-dl.jar "$DEST_DIR/hotswap-agent.jar"
    sudo chmod 644 "$DEST_DIR/hotswap-agent.jar"
    echo "Hotswap Agent installed at $DEST_DIR/hotswap-agent.jar"
  fi
fi

echo "To pin a specific version, set HOTSWAP_AGENT_VERSION (e.g., HOTSWAP_AGENT_VERSION=2.0.1)."

###############################################
# Maven Installation (fast + idempotent)
###############################################
# Allow MAVEN_VERSION=latest (or unset) to auto-resolve from Maven Central metadata.
# This will find the latest 3.9.x version to avoid Maven 4.x.
if [ -z "${MAVEN_VERSION:-}" ] || [ "${MAVEN_VERSION}" = "latest" ]; then
  META_URL="https://repo1.maven.org/maven2/org/apache/maven/apache-maven/maven-metadata.xml"
  echo "Resolving latest Maven 3.9.x version from Maven Central metadata..."
  if command -v curl >/dev/null 2>&1; then
    # Get the latest 3.9.x version specifically 
    MAVEN_VERSION=$(curl -fsSL "$META_URL" 2>/dev/null | sed -n 's:.*<version>\(3\.9\.[0-9]*\)</version>.*:\1:p' | tail -n1)
    if [ -z "$MAVEN_VERSION" ]; then
      # Broader fallback: any 3.x version
      MAVEN_VERSION=$(curl -fsSL "$META_URL" 2>/dev/null | sed -n 's:.*<version>\(3\.[0-9.]*\)</version>.*:\1:p' | tail -n1)
    fi
  elif command -v wget >/dev/null 2>&1; then
    # Get the latest 3.9.x version specifically
    MAVEN_VERSION=$(wget -q -O - "$META_URL" 2>/dev/null | sed -n 's:.*<version>\(3\.9\.[0-9]*\)</version>.*:\1:p' | tail -n1)
    if [ -z "$MAVEN_VERSION" ]; then
      # Broader fallback: any 3.x version  
      MAVEN_VERSION=$(wget -q -O - "$META_URL" 2>/dev/null | sed -n 's:.*<version>\(3\.[0-9.]*\)</version>.*:\1:p' | tail -n1)
    fi
  fi
  if [ -z "$MAVEN_VERSION" ]; then
    MAVEN_VERSION=3.9.11
    echo "WARNING: Unable to resolve latest Maven 3.9.x version; falling back to $MAVEN_VERSION" >&2
  else
    echo "Resolved latest Maven 3.9.x version: $MAVEN_VERSION"
  fi
fi
# Default to latest known 3.9.x version if not already set
MAVEN_VERSION=${MAVEN_VERSION:-3.9.11}
MAVEN_DIR="apache-maven-${MAVEN_VERSION}"
MAVEN_TARGET="/opt/${MAVEN_DIR}"

if command -v mvn >/dev/null 2>&1 && mvn -v 2>/dev/null | grep -q "${MAVEN_VERSION}"; then
  echo "Maven ${MAVEN_VERSION} already installed. Skipping download."
else
  if command -v mvn >/dev/null 2>&1; then
    echo "A different Maven version is present; installing requested ${MAVEN_VERSION}."
  else
    echo "Installing Maven ${MAVEN_VERSION}..."
  fi
  cd /tmp
  MAVEN_ARCHIVE="apache-maven-${MAVEN_VERSION}-bin.tar.gz"
  # Prefer official Apache CDN first, then Maven Central, then other mirrors/archives.
  MAVEN_URLS=( \
    "https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_ARCHIVE}" \
    "https://repo1.maven.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/${MAVEN_ARCHIVE}" \
    "https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_ARCHIVE}" \
    "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_ARCHIVE}" \
  )
  SUCCESS=0
  for url in "${MAVEN_URLS[@]}"; do
    echo "Attempting Maven download from: $url"
    if wget -q -O maven.tar.gz "$url"; then
      SUCCESS=1
      echo "Downloaded Maven from: $url"
      break
    fi
  done
  if [ "$SUCCESS" -ne 1 ]; then
    echo "All Maven download attempts failed." >&2
    echo "Falling back to apt-get (may install older version)..."
    if sudo apt-get update -q && sudo apt-get install -y -q maven; then
      echo "Installed Maven via apt-get:"; mvn -v
    else
      echo "ERROR: Unable to install Maven." >&2
      exit 2
    fi
  else
    sudo tar -xzf maven.tar.gz -C /opt
    sudo ln -sf "$MAVEN_TARGET" /opt/maven
  fi
fi

# Set up Maven environment and configure it to use JDK 17
echo 'export M2_HOME=/opt/maven' | sudo tee -a /etc/environment
echo 'export PATH=$M2_HOME/bin:$PATH' | sudo tee -a /etc/environment

# Ensure Node.js from NVM is available in PATH for Vaadin
if [ -d /usr/local/share/nvm/versions/node ]; then
  NODE_VERSION=$(ls /usr/local/share/nvm/versions/node/ | sort -V | tail -n1)
  if [ -n "$NODE_VERSION" ] && [ -f "/usr/local/share/nvm/versions/node/$NODE_VERSION/bin/node" ]; then
    echo "export PATH=/usr/local/share/nvm/versions/node/$NODE_VERSION/bin:\$PATH" | sudo tee -a /etc/environment
    export PATH=/usr/local/share/nvm/versions/node/$NODE_VERSION/bin:$PATH
    echo "export PATH=/usr/local/share/nvm/versions/node/$NODE_VERSION/bin:\$PATH" >> ~/.bashrc
    echo "Added Node.js $NODE_VERSION to PATH for Vaadin"
  fi
fi

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

# Pre-create Vaadin working directory with proper permissions
echo "Setting up Vaadin working directory..."
mkdir -p /home/vscode/.vaadin
chown -R vscode:vscode /home/vscode/.vaadin
chmod 755 /home/vscode/.vaadin

# Verify installation
echo "Verifying Java installation..."
java -version
echo ""
echo "Verifying JDK 17 DCEVM installation..."
# Always ensure proper permissions for the entire JDK directory (defensive approach)
if sudo test -d /usr/local/jdk-17-dcevm; then
  echo "Ensuring JDK 17 DCEVM permissions (read/traverse for all)..."
  sudo chmod -R 755 /usr/local/jdk-17-dcevm
  # Verify the java binary can run (tests both permissions and shared library access)
  if ! /usr/local/jdk-17-dcevm/bin/java -version 2>/dev/null; then
    echo "ERROR: JDK 17 DCEVM java binary cannot execute" >&2
    echo "Checking directory structure:"
    sudo ls -la /usr/local/jdk-17-dcevm/ | head -10
    echo "Checking bin directory:"
    sudo ls -la /usr/local/jdk-17-dcevm/bin/ | head -5
    echo "Checking lib directory:"
    sudo ls -la /usr/local/jdk-17-dcevm/lib/ | head -5
    exit 1
  fi
else
  echo "ERROR: JDK 17 DCEVM directory does not exist" >&2
  echo "Checking what was installed:"
  sudo ls -la /usr/local/ | grep jdk || echo "No JDK directory found"
  exit 1
fi
/usr/local/jdk-17-dcevm/bin/java -version
echo ""
echo "Verifying Maven installation..."
if command -v mvn >/dev/null 2>&1; then
  mvn -version
elif [ -x /opt/maven/bin/mvn ]; then
  /opt/maven/bin/mvn -version
else
  echo "WARNING: mvn not found in PATH after installation." >&2
fi

# Clean up temporary artifacts if they exist (ignore if already removed)
rm -f /tmp/jbr-dcevm.tar.gz /tmp/maven.tar.gz 2>/dev/null || true
rm -rf /tmp/jbr* 2>/dev/null || true

echo ""
echo "Setup complete!"
echo "- Java 21: Provided by devcontainer base image (for VS Code Java Extension)"
echo "- JDK 17 DCEVM: /usr/local/jdk-17-dcevm (for project compilation and hot-swap debugging)"
echo "- Maven ${MAVEN_VERSION}: /opt/maven"
