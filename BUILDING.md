## Building and Packaging

This is a standard Maven project.  If you wish, you can build the binaries from this source, see [Building a production version](#building-a-production-version) below.

### Development environment

You can checkout this repository (or fork it if you intend to make changes.)

- Development uses the JetBrains Runtime (JBR), which includes DCEVM so code changes are hot-swapped without restarting.
  1. Get a JBR 25 with JCEF from [JetBrains/JetBrainsRuntime](https://github.com/JetBrains/JetBrainsRuntime) and unzip or install it.
  2. Get the agent jar from [HotswapAgent releases](https://github.com/HotswapProjects/HotswapAgent/releases), create a `lib/hotswap` directory in the JBR home, and copy the jar there *without the version number* -- the file must be `lib/hotswap/hotswap-agent.jar`
  3. Copy the example workspace for your platform (`owlcms-windows.code-workspace`, `owlcms-mac.code-workspace` or `owlcms-linux.code-workspace`) to `owlcms.code-workspace` (ignored by git), set the path to your JBR home, and open it.
  - You will be prompted to install the typical Java extensions, accept them.

- For local development the repository uses platform-specific `.env` files stored under the `.vscode/` folder to provide environment variables to the VS Code launch configurations. 
  - Copy `.vscode/.env.example` to a platform-specific file for your system and edit the values you want to override (for example `OWLCMS_UPDATEKEY`): 
    - `.vscode/.env.windows` (Windows), 
    - `.vscode/.env.linux` (Linux), or
    - `.vscode/.env.mac` (macOS).
  - These platform-specific files are ignored by git ; do not commit them.
  - JVM options are set by `owlcms.vmArgs` in the workspace file, not in the `.env` files.
  

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
