# Webdemo Improvement Plan

Three problems, tackled in this order. Each gets its own branch.

---

## Problem 1 — Spring Boot update (`spring_update` branch)

**Current:** Spring Boot 3.3.6 — OSS support ended ~November 2025.

**Goal:** Bump to the latest Spring Boot 3.4.x (same Spring Framework 6 baseline, minimal breakage).

**Steps:**
1. Update `<parent>` version in `de.mkbauer.tinyscript.webdemo/pom.xml`
2. Run full test suite; fix any deprecation warnings
3. Verify the fat JAR still starts and `/execute` works

Spring Boot 4.0 (Spring Framework 7) is the next major upgrade; defer that.

---

## Problem 2 — Frontend modernisation (`frontend_rewrite` branch)

**Current:** AngularJS 1.x (EOL) + Bootstrap 3.3.5 (CDN) + vendored/patched CodeMirror + Font Awesome 4 (CDN).

**Goal:** Replace with a modern, well-structured stack. Keep the same look and feel.

**Framework choice:** Vue 3 + Vite (or React + Vite — same effort at this scale).
Build via `frontend-maven-plugin`; output lands in `src/main/resources/public/` (served by Spring Boot as-is).

**Library upgrades:**
- Bootstrap 3 → Bootstrap 5 (visual change is minimal)
- CodeMirror old → CodeMirror 6 (`@codemirror/...`); rewrite `tinyscript.js` using the `StreamLanguage` adapter
- Font Awesome 4 → Bootstrap Icons (spinner is the only icon used)
- Remove `ui-bootstrap` (Bootstrap 5 needs no jQuery bridge)

**Component structure:**
- `App` — root state (editor content, result, error, running flag)
- `Toolbar` — Examples dropdown + Execute button
- `Editor` — CodeMirror 6 wrapper
- `Output` — result panel (value, stdout, stats, error highlight)

---

## Problem 3 — CI/CD pipeline + multi-arch Docker (`cicd` branch)

**Current:** `.drone.yml` builds Maven but never builds or pushes a Docker image; uses `maven:3-jdk-11` (wrong Java version).

**Goal:** Self-hosted build pipeline that produces a Linux x86_64 Docker image automatically.

**Stack:** Gitea + Gitea Actions + `act_runner` (all running in Docker on Linux x86).

**Workflow — two jobs:**
1. `build-jar`: `eclipse-temurin:21` → `mvn clean install -DskipTests` + `mvn package -pl de.mkbauer.tinyscript.webdemo` → uploads fat JAR as artifact
2. `build-image`: depends on `build-jar` → `docker buildx build --platform linux/amd64 --push -t <registry>/tinyscript-webdemo:latest .`

**Registry:** Gitea built-in container registry (OCI-compatible, no extra service).

**Dockerfile improvements:**
- Replace deprecated `openjdk:21-jre-slim` with `eclipse-temurin:21-jre-jammy`
- Remove the `/dev/urandom` hack (unnecessary since Java 8u162)
- Consider two-stage build (Maven inside Docker) with `--mount=type=cache` for the Maven repo
