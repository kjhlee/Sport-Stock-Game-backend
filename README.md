
# Backend Development Commands

This project uses Maven Wrapper (./mvnw), Spotless for formatting, and JaCoCo for coverage. All commands can be run from the repository root.

Build the project

Compile and package all backend modules:

```./mvnw package -DskipTests```

This verifies that all services compile and produces build artifacts without running tests.

⸻

Run the full verification lifecycle

```./mvnw verify```

This runs the full Maven lifecycle:
•	compile all modules
•	run tests
•	generate coverage reports via JaCoCo

Coverage reports will appear in:

services/<module>/target/site/jacoco/index.html


⸻

Run tests only

```./mvnw test```

Runs all unit tests across modules.

⸻

Check code formatting (Spotless)

Verify that the code follows the project formatting rules:

```./mvnw spotless:check```

This does not modify files. It fails if formatting issues exist.

⸻

Automatically fix formatting

```./mvnw spotless:apply```

This rewrites files to match the project’s formatting rules.

Formatting includes:
•	Google Java Format
•	removing unused imports
•	consistent import ordering
•	trimming trailing whitespace
•	ensuring newline at end of files

⸻

Typical developer workflow

Before committing:

```./mvnw spotless:apply```
```./mvnw verify```

This ensures formatting is correct and the build passes.

⸻

CI pipeline behavior

The CI workflow runs:
1.	formatting checks (spotless:check)
2.	project build
3.	tests
4.	coverage generation

Pull requests will fail if formatting or builds are incorrect.

⸻

Maven wrapper

Always use the wrapper:

./mvnw

Do not rely on a locally installed Maven version. The wrapper ensures consistent builds across environments.

⸻

If you want, I can also add a very short “Quick Start for new contributors” section (about 5 lines) that makes onboarding much smoother for teammates.