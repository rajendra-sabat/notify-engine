#!/bin/bash
# Post-hook: compile-check after any Java file edit, then prompt code review.
# Claude Code passes the tool call + result as JSON on stdin.

INPUT=$(cat)
FILE=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    path = d.get('tool_input', {}).get('file_path', '')
    print(path)
except Exception:
    print('')
" 2>/dev/null)

# Only act on Java source files
if [[ "$FILE" != *.java ]]; then
    exit 0
fi

RELATIVE="${FILE#$PWD/}"

# Run compile
COMPILE_OUTPUT=$(./mvnw compile -q 2>&1)
COMPILE_EXIT=$?

if [ $COMPILE_EXIT -ne 0 ]; then
    echo "COMPILE FAILED after editing $RELATIVE"
    echo "$COMPILE_OUTPUT"
    echo ""
    echo "Fix the compilation errors above before continuing."
    exit 0
fi

echo "Compile OK — $RELATIVE"
echo ""
echo "CODE REVIEW REQUESTED: Review the change just made to $RELATIVE for:"
echo "  - Correctness and edge cases"
echo "  - Security issues (SQL injection, key exposure, input validation)"
echo "  - Multi-tenancy safety (TenantContext leaks, cross-tenant data access)"
echo "  - Adherence to flat package structure (config/domain/dto/filter/repository/service/controller)"
echo "  - Spring Boot best practices (constructor injection, no field injection)"
echo "  - Unnecessary complexity or abstraction beyond what the task requires"
