## Building and Packaging

This is a standard Maven project.  If you wish, you can build the binaries from this source, see [Building a production version](#building-a-production-version) below.

### Development environment

You can checkout this repository (or fork it if you intend to make changes.)

On every platform, we provide configurations for Visual Studio Code (VS Code).

- Development uses the JetBrains Runtime (JBR), which includes DCEVM so code changes are hot-swapped without restarting.
- You will launch `owlcms.code-workspace` as a local copy for your platform after following the instructions below.
  - JVM options are set by `owlcms.vmArgs` in the workspace file, not in `.vscode/.env`.
  - The workspace templates enable enhanced class redefinition with `-XX:+AllowEnhancedClassRedefinition` and load `lib/hotswap/hotswap-agent.jar` from the
    selected JBR with `-XX:HotswapAgent=fatjar`. They also disable the Vaadin development tools with `-Dvaadin.devmode.devTools.enabled=false`.
- There is a local `.vscode/.env` file for optional per-developer settings.

#### macOS

1. Download the JBRSDK from the [25.0.5.1 release](https://github.com/JetBrains/JetBrainsRuntime/releases/tag/jbr-release-25.0.5.1b635.102), under **Binaries for developers**. Use:
   - Apple silicon: `jbrsdk-25.0.5.1-osx-aarch64-b635.102.tar.gz`
   - Intel: `jbrsdk-25.0.5.1-osx-x64-b635.102.tar.gz`
2. Extract the downloaded archive under `/Library/Java/JavaVirtualMachines`. The Java home paths are:
   - Apple silicon: `/Library/Java/JavaVirtualMachines/jbrsdk-25.0.5.1-osx-aarch64-b635.102/Contents/Home`
   - Intel: `/Library/Java/JavaVirtualMachines/jbrsdk-25.0.5.1-osx-x64-b635.102/Contents/Home`
3. Download `hotswap-agent-2.0.5.jar` from the [HotswapAgent 2.0.5 release](https://github.com/HotswapProjects/HotswapAgent/releases/tag/RELEASE-2.0.5). Create the `lib/hotswap` directory and copy the jar as `hotswap-agent.jar`:
   - Apple silicon: `/Library/Java/JavaVirtualMachines/jbrsdk-25.0.5.1-osx-aarch64-b635.102/Contents/Home/lib/hotswap/hotswap-agent.jar`
   - Intel: `/Library/Java/JavaVirtualMachines/jbrsdk-25.0.5.1-osx-x64-b635.102/Contents/Home/lib/hotswap/hotswap-agent.jar`
4. Copy `owlcms-mac.code-workspace` to `owlcms.code-workspace`. Apple silicon is preconfigured; on Intel, replace `osx-aarch64` with `osx-x64` in `java.configuration.runtimes[].path` and `terminal.integrated.env.osx.JAVA_HOME`.
5. Copy `.vscode/.env.example` to `.vscode/.env` and edit the values to override, such as `OWLCMS_PORT`.
6. Open `owlcms.code-workspace` and accept the recommended Java extensions.

#### Windows

1. Download the JBRSDK from the [25.0.5.1 release](https://github.com/JetBrains/JetBrainsRuntime/releases/tag/jbr-release-25.0.5.1b635.102), under **Binaries for developers**. Use:
   - x64: `jbrsdk-25.0.5.1-windows-x64-b635.102.zip`
   - ARM64: `jbrsdk-25.0.5.1-windows-aarch64-b635.102.zip`
2. Extract the downloaded archive under `C:\Dev\Java`. The Java home paths are:
   - x64: `C:\Dev\Java\jbrsdk-25.0.5.1-windows-x64-b635.102`
   - ARM64: `C:\Dev\Java\jbrsdk-25.0.5.1-windows-aarch64-b635.102`
3. Download `hotswap-agent-2.0.5.jar` from the [HotswapAgent 2.0.5 release](https://github.com/HotswapProjects/HotswapAgent/releases/tag/RELEASE-2.0.5). Create the `lib\hotswap` directory and copy the jar as `hotswap-agent.jar`:
   - x64: `C:\Dev\Java\jbrsdk-25.0.5.1-windows-x64-b635.102\lib\hotswap\hotswap-agent.jar`
   - ARM64: `C:\Dev\Java\jbrsdk-25.0.5.1-windows-aarch64-b635.102\lib\hotswap\hotswap-agent.jar`
4. Copy `owlcms-windows.code-workspace` to `owlcms.code-workspace`. x64 is preconfigured; on ARM64, replace `windows-x64` with `windows-aarch64` in `java.configuration.runtimes[].path` and `terminal.integrated.env.windows.JAVA_HOME`.
5. Copy `.vscode/.env.example` to `.vscode/.env` and edit the values to override, such as `OWLCMS_PORT`.
6. Open `owlcms.code-workspace` and accept the recommended Java extensions.

#### Linux x64

1. Download the JBRSDK from the [25.0.5.1 release](https://github.com/JetBrains/JetBrainsRuntime/releases/tag/jbr-release-25.0.5.1b635.102), under **Binaries for developers**. Use:
   `jbrsdk-25.0.5.1-linux-x64-b635.102.tar.gz`
2. Extract the downloaded archive under `/opt/java`. The Java home path is:
   `/opt/java/jbrsdk-25.0.5.1-linux-x64-b635.102`
3. Download `hotswap-agent-2.0.5.jar` from the [HotswapAgent 2.0.5 release](https://github.com/HotswapProjects/HotswapAgent/releases/tag/RELEASE-2.0.5). Create the `lib/hotswap` directory and copy the jar to:
   `/opt/java/jbrsdk-25.0.5.1-linux-x64-b635.102/lib/hotswap/hotswap-agent.jar`
4. Copy `owlcms-linux.code-workspace` to `owlcms.code-workspace`.
5. Copy `.vscode/.env.example` to `.vscode/.env` and edit the values to override, such as `OWLCMS_PORT`.
6. Open `owlcms.code-workspace` and accept the recommended Java extensions.

#### Raspberry Pi/Linux ARM64

1. Download the JBRSDK from the [25.0.5.1 release](https://github.com/JetBrains/JetBrainsRuntime/releases/tag/jbr-release-25.0.5.1b635.102), under **Binaries for developers**. Use:
   `jbrsdk-25.0.5.1-linux-aarch64-b635.102.tar.gz`
2. Extract the downloaded archive under `/opt/java`. The Java home path is:
   `/opt/java/jbrsdk-25.0.5.1-linux-aarch64-b635.102`
3. Download `hotswap-agent-2.0.5.jar` from the [HotswapAgent 2.0.5 release](https://github.com/HotswapProjects/HotswapAgent/releases/tag/RELEASE-2.0.5). Create the `lib/hotswap` directory and copy the jar to:
   `/opt/java/jbrsdk-25.0.5.1-linux-aarch64-b635.102/lib/hotswap/hotswap-agent.jar`
4. Copy `owlcms-rpi.code-workspace` to `owlcms.code-workspace`.
5. Copy `.vscode/.env.example` to `.vscode/.env` and edit the values to override, such as `OWLCMS_PORT`.
6. Open `owlcms.code-workspace` and accept the recommended Java extensions.
  

#### Cloud development 

You can avoid these steps by developing in the cloud, using Github Codespaces.   You can start a codespace from the github page for jflamy/owlcms4.  You will be prompted to use a workspace, and should use the "devcontainer" workspace definition.

## Building a production version

The actual build chain is a Github Actions workflow, in `.github/workflows/release.yaml`.   This workflow is complicated by the fact that the releases are not in the main repository, but in two separate repositories, in order to have separate binaries and documentation for stable and pre- releases.

But you can use maven to create a production build manually, as follows

### Pre-requisites

- Install git : Installing [GitHub Desktop](https://desktop.github.com/) is the easiest way to install Git on a Windows system. Select the options to add the programs to the execution path.
- Clone this repository
- Install Java 25
- Install Maven

### Building and testing

- From the owlcms4 directory, running ``mvn -P production -am -pl clean owlcms package `` should give you 
  - `owlcms/target/owlcms.jar` a working  "uberjar" (that is, a .jar file that contains all the dependencies together in a single file).  This file can then be run using `java -jar owlcms.jar app.owlcms.Main` 
  - `owlcms/target/owlcms.zip` contains a copy of the local files required.  This is what the owlcms installers use.

### Building a Docker container

There is a Dockerfile in owlcms4top to build owlcms for quick testing to the fly.io cloud (see deploy.sh).

The `owlcms-docker` project prepares the shaded application jar and logging configuration for the root Dockerfile. Both Fly.io testing and release builds use this same Dockerfile after Maven prepares the context.
