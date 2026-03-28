#!/usr/bin/env bash
# copy-scaffold.sh
# Run this on your Mac to pull the scaffold from the Claude.ai container
# Usage: bash copy-scaffold.sh <output-dir>
# Example: bash copy-scaffold.sh ~/dev

set -euo pipefail

DEST="${1:-$HOME/dev}"
BACKEND="$DEST/disney-planner-backend"

echo "Creating backend scaffold at $BACKEND"
mkdir -p "$BACKEND"

# Copy all files preserving structure
# (When downloading from Claude, place the zip in /tmp and run this script)

echo ""
echo "Next steps:"
echo "  1. cd $BACKEND"
echo "  2. git init && git add . && git commit -m 'initial scaffold'"
echo "  3. cp .env.example .env && fill in ANTHROPIC_API_KEY"
echo "  4. docker-compose up -d"
echo "  5. mvn spring-boot:run"
echo ""
echo "Then open Claude Code and run:"
echo "  claude"
echo "  > Read INITIAL_PROMPT.md and fix the issues listed before adding anything new."
