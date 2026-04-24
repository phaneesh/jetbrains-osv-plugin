# OSV IntelliJ Plugin - GitHub Actions Quick Reference

## Available Workflows

### 1. Build and Release OSV Plugin (`.github/workflows/build.yml`)

**Triggers:**
- Push to `main` or `master` branches
- Pull requests to `main` or `master`
- Tag pushes (`v*`)
- Manual execution via `workflow_dispatch`

**Jobs:**
| Job | Description |
|-----|-------------|
| `build` | Compiles plugin, uploads artifact |
| `test` | Runs all unit tests |
| `verify` | Verifies plugin compliance |
| `release` | Creates GitHub release for tags |
| `release-draft` | Creates draft release for main |
| `build-snapshot` | Builds snapshot versions |

### 2. Release Plugin (`.github/workflows/release.yml`)

**Triggers:**
- Tag pushes matching `v*` pattern

**Jobs:**
| Job | Description |
|-----|-------------|
| `release` | Creates official GitHub release |

## Usage

### Creating a Release

```bash
# Update version in build.gradle.kts
# Commit changes
git add .
git commit -m "Bump version to 1.0.0"

# Create and push tag
git tag v1.0.0
git push origin v1.0.0

# GitHub Actions will automatically create a release
```

### Manual Build

1. Go to GitHub → Actions
2. Select "Build and Release OSV Plugin"
3. Click "Run workflow"
4. Select branch and version tag

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

## Version Format

Use semantic versioning:
- `v1.0.0` - Stable release
- `v1.0.0-beta.1` - Beta release
- `v1.0.0-rc.1` - Release candidate
- `v0.0.0-snapshot-20240424-abc123` - Snapshot (auto-generated)

## Release Notes

Releases include:
- Plugin features list
- Installation instructions
- Requirements
- Supported ecosystems
- License information
- Support links

## GitHub Actions Setup

The workflows use:
- JDK 17 (Temurin)
- Gradle 8.5
- Ubuntu 22.04
- GitHub Actions v4

## Status Badges

Add to your README:

```markdown
[![Build and Release](https://github.com/dyuti/jetbrains-osv-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/dyuti/jetbrains-osv-plugin/actions/workflows/build.yml)
[![Release](https://github.com/dyuti/jetbrains-osv-plugin/actions/workflows/release.yml/badge.svg)](https://github.com/dyuti/jetbrains-osv-plugin/actions/workflows/release.yml)
```
