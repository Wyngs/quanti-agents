# How to Generate JavaDoc Documentation

## Generate JavaDoc

From the project root directory, run:

```bash
./gradlew generateJavadoc
```

This will:

- Automatically include the Android SDK and all dependencies in the classpath
- Generate documentation for all public and protected members
- Create the documentation in the build directory

## Access the JavaDoc

After generation, the documentation is available at:

```
app/build/docs/javadoc/index.html
```

### Open in Browser

**On macOS:**

```bash
open app/build/docs/javadoc/index.html
```

**On Linux:**

```bash
xdg-open app/build/docs/javadoc/index.html
```

**On Windows:**

```bash
start app/build/docs/javadoc/index.html
```

Or simply navigate to the file in your file browser and double-click `index.html`.

## Documentation Details

- **Visibility**: Public and protected members are documented
- **Encoding**: UTF-8
- **Location**: `app/build/docs/javadoc/`
- **Regeneration**: Run the command again to regenerate after code changes

## Troubleshooting

If you encounter errors:

1. Make sure you're running from the project root directory
2. Ensure Gradle is properly configured
3. The task automatically handles Android SDK dependencies, so no manual classpath configuration is needed
