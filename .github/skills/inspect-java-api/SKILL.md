---
name: inspect-java-api
description: "Use when: inspecting Java APIs or reverse-engineering package behavior from JARs, classes, modules, or dependencies. Keywords: javap, jar tf, jdeps, jmod, class signature, bytecode, package API, dependency inspection, missing JDK tool."
---

# Inspect Java APIs and Packages

Use this skill to inspect compiled Java dependencies or JDK modules without
changing the OWLCMS runtime configuration.

## Runtime and Tooling Rule

OWLCMS deliberately runs with JetBrains Runtime (JBR), which provides the
Hotswap configuration used by local development. Do not replace `JAVA_HOME`,
alter the workspace Java runtime, or launch OWLCMS with Homebrew OpenJDK merely
to inspect an API.

Some JDK command-line tools may be unavailable in the JBR bundle. Use the
Homebrew OpenJDK tool explicitly as a fallback:

```bash
OPENJDK_HOME="$(brew --prefix openjdk)/libexec/openjdk.jdk/Contents/Home"
"$OPENJDK_HOME/bin/javap" --version
```

The Homebrew JDK does not need to be registered with `/usr/libexec/java_home`.
The explicit path above is the supported inspection-tool location.

## Inspect a Dependency JAR

1. Locate an existing JAR. Prefer a file already present in `~/.m2/repository`,
   the workspace, or an existing `target/` directory. Do not run Maven merely
   to obtain a JAR without the user's consent.

2. List candidate classes and resources:

   ```bash
   "$OPENJDK_HOME/bin/jar" tf "/path/to/dependency.jar" | grep 'PackageOrClass'
   ```

3. Read the public and package-visible API, including JVM descriptors:

   ```bash
   "$OPENJDK_HOME/bin/javap" \
     -classpath "/path/to/dependency.jar" \
     -p -s com.example.PackageClass
   ```

4. When behavior is unclear, inspect bytecode as well:

   ```bash
   "$OPENJDK_HOME/bin/javap" \
     -classpath "/path/to/dependency.jar" \
     -p -c -s com.example.PackageClass
   ```

`-p` includes private and package-visible members. `-s` shows JVM descriptors,
which disambiguate overloads and generic-erased types. `-c` disassembles method
implementations.

## Inspect Modules and Dependencies

Use these read-only commands when their results answer the question more
directly than class disassembly:

```bash
# Display a JAR's module descriptor or its automatic-module name.
"$OPENJDK_HOME/bin/jar" --describe-module --file "/path/to/dependency.jar"

# Report package and module dependencies of a JAR.
"$OPENJDK_HOME/bin/jdeps" -q "/path/to/dependency.jar"

# Inspect a JDK module bundled with the Homebrew OpenJDK.
"$OPENJDK_HOME/bin/jmod" describe "$OPENJDK_HOME/jmods/java.base.jmod"
```

## Report Findings Precisely

- Name the inspected JAR or module and the fully qualified class.
- Distinguish observed signatures and bytecode behavior from assumptions.
- State when source code, runtime configuration, or a Maven build was not
  needed or was not run.

## Do Not

- Do not replace JBR with Homebrew OpenJDK for OWLCMS execution or hotswap.
- Do not permanently export the Homebrew JDK as the workspace's `JAVA_HOME`.
- Do not run Maven or download a new dependency solely for inspection without
  explicit user consent.