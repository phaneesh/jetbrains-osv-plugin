# GitHub Workflows - OSV IntelliJ Plugin

This directory contains GitHub Actions workflows for building, testing, and releasing the OSV IntelliJ Plugin.

## Workflows

### 1. `build.yml` - Build and Test Workflow

**Triggers:**
- Push to `main` or `master` branches
- Pull requests to `main` or `master`
- Manual dispatch (`workflow_dispatch`)
- Tag pushes (`v*`)

**Jobs:**
- `build` - Compiles the plugin and uploads artifacts
- `test` - Runs all unit tests
- `verify` - Verifies plugin compliance with JetBrains guidelines
- `release` - Creates GitHub releases for version tags
- `release-draft` - Creates draft releases for main branch commits
- `build-snapshot` - Builds snapshot versions for development

**Features:**
- Java 17 JDK setup
- Gradle 8.5 caching
- Plugin verification with `verifyPlugin` task
- Artifact upload for testing and release

### 2. `release.yml` - Release Workflow

**Triggers:**
- Tag pushes matching `v*` pattern (e.g., `v1.0.0`, `v1.2.3`)

**Jobs:**
- `release` - Creates official GitHub releases

**Features:**
- Compiles with `buildPlugin` task
- Verifies plugin compliance
- Creates GitHub release with changelog
- Uploads `.jar` files as release assets

## Usage

### Creating a Release

1. Update version in `build.gradle.kts`
2. Commit and push changes
3. Create a tag: `git tag v1.0.0`
4. Push the tag: `git push origin v1.0.0`
5. GitHub Actions will automatically create a release

### Manual Build

Use `workflow_dispatch` to trigger builds manually:
1. Go to GitHub → Actions
2. Select "Build and Release OSV Plugin"
3. Click "Run workflow"
4. Select branch and version

## Output Artifacts

### Build Artifact
- **Name**: `osv-plugin`
- **Path**: `build/libs/*.jar`
- **Retention**: 7 days

### Test Results
- **Name**: `test-results`
- **Path**: `build/reports/tests`
- **Retention**: 7 days

### Release
- **Format**: JAR
- **Name**: `jetbrains-osv-plugin-{version}.jar`
- **Published**: GitHub Releases

## Configuration

### Secrets
- `GITHUB_TOKEN` - Automatic, provided by GitHub

### Environment Variables
- `VERSION` - Extracted from git tag or manually provided

## Plugin Details

- **Name**: OSV Vulnerability Scanner
- **ID**: `io.dyuti.osvplugin`
- **Vendor**: Dyuti
- **License**: Apache 2.0
- **IntelliJ Platform**: 2019.3+

## Release Notes Format

Releases include:
- Plugin features list
- Installation instructions
- Requirements
- Supported ecosystems
- License information
- Support links
