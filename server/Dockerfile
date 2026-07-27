# Runtime image for :server. Deliberately SINGLE-stage — it packages an already-built distribution
# and compiles nothing.
#
#   ./gradlew :server:installDist
#   docker build -f server/Dockerfile -t mileway-server .
#
# Why there is no in-image Gradle build stage, which is the obvious first instinct:
#
#  1. It cannot work without dragging in the Android SDK. `:server` declares
#     `api(project(":contract"))`, and `:contract` applies `mileway.kmp.library.watchos` with an
#     `android { compileSdk = 37 }` block — so configuring it needs an Android SDK even though the
#     server itself needs nothing Android. A JDK base image has none, and AGP fails at configuration
#     with "SDK location not found". Installing cmdline-tools + platform-37 would turn a ~200 MB
#     image into a multi-GB one purely to compile a module that is pure JVM.
#  2. `COPY . .` would ingest the whole monorepo — including `.ralph/`, which holds REFERENCE.md
#     (the proprietary reference app's name and local path). Docker obeys `.dockerignore`, NOT
#     `.gitignore`, so gitignoring it does not help: the name would land in a published image layer,
#     violating CLAUDE.md's "no proprietary references, even by accident" guardrail. Copying only
#     the built distribution makes that structurally impossible rather than merely discouraged.
#
# A .dockerignore is committed alongside this as defence in depth, so a future edit that
# reintroduces a broad COPY still cannot pick up .ralph/ or local.properties.
FROM eclipse-temurin:21-jre

# Non-root. The temurin images are Ubuntu-based, so useradd is available.
RUN useradd --system --create-home --shell /usr/sbin/nologin mileway

WORKDIR /app

# `installDist` writes build/install/<applicationName>/{bin,lib}; applicationName defaults to the
# Gradle project name, which is "server" (settings.gradle.kts's include(":server")). Verified by
# running the task: server/build/install/server/bin/server exists.
COPY server/build/install/server/ ./

RUN chown -R mileway:mileway /app
USER mileway

# Matches envPort()'s default in Application.kt; Netty binds 0.0.0.0.
EXPOSE 8080
ENTRYPOINT ["./bin/server"]
