#!/bin/bash
# Pre-hook: warn on destructive bash commands before execution.
# Claude Code passes the tool call as JSON on stdin.

INPUT=$(cat)
COMMAND=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('command', ''))
except Exception:
    print('')
" 2>/dev/null)

DANGEROUS_PATTERNS="rm -rf|git reset --hard|git push --force|git push -f|DROP TABLE|TRUNCATE TABLE|git checkout \.|git restore \."

if echo "$COMMAND" | grep -qiE "$DANGEROUS_PATTERNS"; then
    echo "⚠️  PRE-HOOK WARNING: Potentially destructive command detected."
    echo "Command: $COMMAND"
    echo "Verify this is intentional before proceeding."
fi

exit 0
