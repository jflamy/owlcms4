# OWLCMS Devcontainer Usage

This devcontainer sets up a Java development environment for OWLCMS and auto-installs the recommended Java extensions so launch configurations are available.

This README covers two cases: using the local Dev Container (Remote - Containers) and using GitHub Codespaces. Both use the same `.devcontainer` configuration, but the steps to open and verify the environment are shown separately below.

## Dev Container

Quick start

- Open the repository in VS Code.
- Use the Command Palette (`Ctrl+Shift+P`) and select `Dev Containers: Reopen in Container` (or use the green "Open in Container" button if available).
- When the container finishes starting, choose the workspace file `owlcmsHotswap.code-workspace` from the workspace files list if prompted.

What the devcontainer provides

- A Java runtime image (`mcr.microsoft.com/devcontainers/java:21-bullseye`) and a setup script to prepare JDKs.
- Container-level runtime setup (JDK installation, `JAVA_HOME`, Maven location, forwarded ports, cache mounts) that a `.code-workspace` file cannot provide on its own.
- The extensions listed in `.devcontainer/devcontainer.json` under `customizations.vscode.extensions` are auto-installed in the container. This includes:
  - `redhat.java`
  - `vscjava.vscode-java-debug`
  - `vscjava.vscode-maven`
  - and other Java helper extensions
- The workspace `.vscode/launch.json` contains Java launch configurations and will be visible in the Run view once the Java extensions are installed and enabled.

How to verify the Java extensions and launches

1. Open the Extensions view (`Ctrl+Shift+X`) and filter by `@installed` to see which extensions are installed in the container.
2. Confirm `redhat.java` and `vscjava.vscode-java-debug` appear and are enabled.
3. Open the Run view (left Run icon) — the Java launch configurations from `.vscode/launch.json` should be listed.

Troubleshooting

- If extensions are not installed automatically, try `Dev Containers: Rebuild Container` from the Command Palette.
- If you see the launches missing, ensure the Java extensions are enabled for the workspace/profile and that you're using the `owlcmsHotswap.code-workspace` file.

- If you see an error like `Could not create local repository at /home/vscode/.m2/repository`, it is usually a permissions issue on the Maven cache. The devcontainer will attempt to fix this on start by changing ownership of `/home/vscode/.m2` to the `vscode` user. To fix manually, run inside the container:

```bash
sudo chown -R vscode:vscode /home/vscode/.m2
```

Then rebuild the container if necessary.

Notes

- This README is for users who open the repository in a devcontainer. Non-container users should install the recommended extensions (see `.vscode/extensions.json`) or use a Java profile to get the same behavior.
- `owlcmsHotswap.code-workspace` controls editor/workspace settings (launch configs, VM args references, recommendations). The devcontainer controls the actual cloud machine environment used by Codespaces.

## Codespaces

- GitHub Codespaces uses the same devcontainer configuration. When creating a Codespace for this repo, the devcontainer defined in `.devcontainer/devcontainer.json` will be used and the same extensions and setup steps will be applied.
- In Codespaces, the `onCreateCommand` and `postCreateCommand` are executed during provisioning. Choose the `owlcmsHotswap.code-workspace` workspace in the Codespaces workspace picker if prompted.
- Ports forwarded by the devcontainer (`8080`, `8081`, `1883`) will be mapped in Codespaces' forwarded ports view automatically.

Persistent Maven cache (cross-platform)

To avoid different behavior between Windows, WSL and Codespaces, this devcontainer now uses a named Docker volume to persist the Maven cache across container rebuilds and across environments. The volume used is `owlcms_m2_cache` and it is mounted into the container at `/home/vscode/.m2`.

Benefits:

- Works consistently in Codespaces and local devcontainers regardless of the host OS.
- Keeps the cache persistent across container rebuilds without writing directly to the host filesystem.

If you prefer to use your host `.m2` instead (for sharing cache with your host), edit `.devcontainer/devcontainer.json` and replace the `mounts` entry with a host bind mount. Example (Windows host):

```json
"mounts": [
  "source=${localEnv:USERPROFILE}\\.m2,target=/home/vscode/.m2,type=bind,consistency=cached"
]
```

Or to use the WSL home `.m2`, open the devcontainer from WSL so `${localEnv:HOME}` expands to the WSL home, or set the mount accordingly.

