# OSV IntelliJ Plugin - GitHub Actions Workflow

This workflow builds, tests, and releases the OSV IntelliJ Plugin.

## Usage

### Manual Trigger

1. Go to GitHub Actions tab
2. Select "Build and Release OSV Plugin"
3. Click "Run workflow"
4. Select branch and version tag

### Automatic Trigger

- Push to `main` branch (creates draft release)
- Push tag `v*` (creates official release)
- Pull requests to `main` or `master`

## Configuration

### Version Format

Use semantic versioning: `v1.0.0`, `v1.2.3`

### Custom Version

For manual trigger, specify version like:
- `v1.0.0`
- `v1.0.0-beta.1`
- `v1.0.0-rc.1`

## Build Steps

1. Checkout repository
2. Setup JDK 17
3. Setup Gradle 8.5
4. Build plugin
5. Run tests
6. Verify plugin
7. Create release (if tag)

## Output

- Plugin JAR in `build/libs/`
- Test results in `build/reports/tests`
- GitHub release with artifacts
