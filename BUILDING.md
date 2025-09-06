## Building and Packaging

This is a standard Maven project.  If you wish, you can build the binaries from this source, see [Building a production version](#building-a-production-version) below.

### Development environment

You can checkout this repository (or fork it if you intend to make changes.)

- Typical Use: You can start vscode using the owlcmsJDK.code-workspace file to get correct defaults
  - You will be prompted to install the typical Java extensions, accept them.
- Advanced Use: If instead you want to use HotSwap with DCEVM, you can copy and edit `owlcmsHotswap.code-workspace` from the .vscode directory
  - Get a JDK from JetBrains [JetBrains/JetBrainsRuntime: Runtime environment based on OpenJDK for running IntelliJ Platform-based products on Windows, macOS, and Linux](https://github.com/JetBrains/JetBrainsRuntime) and unzip it.
  - Get the Hotswap agent from [HotswapAgent releases](https://github.com/HotswapProjects/HotswapAgent/releases)
  - Create a `lib/hotswap` in the JDK installation directory. Copy the agent jar, *and remove the version number* -- the file should be `lib/hotswap/hotswap-agent.jar`
- For local development the repository uses platform-specific `.env` files stored under the `.vscode/` folder to provide environment variables to the VS Code launch configurations. 
  - Copy `.vscode/.env.example` to a platform-specific file for your system under and edit the values you want to override (for example `OWLCMS_UPDATEKEY`): 
    - use `.vscode/.env.windows` (Windows), 
    - `.vscode/.env.linux` (Linux), or
    -  `.vscode/.env.mac` (macOS).
  - These platform-specific files are ignored by git ; do not commit them.

#### Cloud development 

You can avoid these steps by developing in the cloud, using Github Codespaces.   You can start a codespace from the github page for jflamy/owlcms4.  You will be prompted to use a workspace, and should use the "devcontainer" workspace definition.

## Building a production version

The actual build chain is a Github Actions workflow, in `.github/workflows/release.yaml`. 
But you can use maven to create a production build manually.

### Pre-requisites

- Install git : Installing [GitHub Desktop](https://desktop.github.com/) is the easiest way to install Git on a Windows system. Select the options to add the programs to the execution path.
- Clone this repository
- Install Java 17
- Install Maven

### Building and testing

- From the owlcms4 directory, running ``mvn -P production -am -pl clean owlcms package `` should give you 
  - `owlcms/target/owlcms.jar` a working  "uberjar" (that is, a .jar file that contains all the dependencies together in a single file).  This file can then be run using `java -jar owlcms.jar app.owlcms.Main` 
  - `owlcms/target/owlcms.zip` contains a copy of the local files required.  This is what the owlcms installers use.
- The installers are in their own repositories under https://github.com/owlcms and are are built separately.

### Building a Docker container

There is a Dockerfile in owlcms4top to build owlcms for quick testing to the fly.io cloud (see deploy.sh).  Building publicresults would be similar.

The current process for actual production builds uses the `owlcms-docker` project to build Docker containers using  `mvn package`, once the production build has been done.
