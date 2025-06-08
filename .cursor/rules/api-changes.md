## API Changes

When making changes to the public API:
- Always run `./gradlew apiDump` before committing
- This updates the API dump files in the `api/` directory
- Include the updated API dump files in your commit
- This ensures binary compatibility tracking is maintained