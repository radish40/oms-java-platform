#!/usr/bin/env bash
set -euo pipefail

echo "Running public-release sanitization checks..."

FAILED=0
ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT"

release_pathspec=(
  --
  .
  ':(exclude)target/**'
  ':(exclude)**/__pycache__/**'
  ':(exclude)**/*.class'
  ':(exclude)**/*.jar'
  ':(exclude)**/*.war'
  ':(exclude)**/*.zip'
)

# Generic regexes are excluded from this script to avoid self-referential
# pattern matches. The private local denylist below still scans this script.
generic_regex_pathspec=(
  --
  .
  ':(exclude)pre-push-check.sh'
  ':(exclude)target/**'
  ':(exclude)**/__pycache__/**'
  ':(exclude)**/*.class'
  ':(exclude)**/*.jar'
  ':(exclude)**/*.war'
  ':(exclude)**/*.zip'
)

fail() {
  echo "FAIL: $1"
  FAILED=1
}

pass() {
  echo "OK: $1"
}

scan_current_regex() {
  local label="$1"
  local pattern="$2"
  if git grep -q -I -E "$pattern" "${generic_regex_pathspec[@]}" 2>/dev/null; then
    fail "$label"
  else
    pass "$label"
  fi
}

scan_history_regex() {
  local label="$1"
  local pattern="$2"
  while read -r commit; do
    [ -z "$commit" ] && continue
    if git grep -q -I -E "$pattern" "$commit" "${generic_regex_pathspec[@]}" 2>/dev/null; then
      fail "$label"
      return
    fi
  done < <(git rev-list --all)
  pass "$label"
}

scan_current_fixed_private() {
  local ordinal="$1"
  local term="$2"
  local label="local denylist current tree entry #${ordinal}"
  if git grep -q -I -F "$term" "${release_pathspec[@]}" 2>/dev/null; then
    fail "$label"
  else
    pass "$label"
  fi
}

scan_history_fixed_private() {
  local ordinal="$1"
  local term="$2"
  local label="local denylist history entry #${ordinal}"
  while read -r commit; do
    [ -z "$commit" ] && continue
    if git grep -q -I -F "$term" "$commit" "${release_pathspec[@]}" 2>/dev/null; then
      fail "$label"
      return
    fi
  done < <(git rev-list --all)
  pass "$label"
}

scan_current_regex "high-confidence credential formats" 'AKIA[0-9A-Z]{16}|ASIA[0-9A-Z]{16}|ghp_[A-Za-z0-9_]{30,}|glpat-[A-Za-z0-9_-]{20,}|xox[baprs]-[A-Za-z0-9-]{20,}|sk-[A-Za-z0-9_-]{20,}|BEGIN (RSA|DSA|EC|OPENSSH|PRIVATE) KEY'
scan_current_regex "private network literals" '(^|[^0-9])(10\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}|192\.168\.[0-9]{1,3}\.[0-9]{1,3}|172\.(1[6-9]|2[0-9]|3[0-1])\.[0-9]{1,3}\.[0-9]{1,3})([^0-9]|$)'
scan_current_regex "private or local-only domains" 'https?://[^[:space:]"'\'']*\.(corp|internal|intranet|lan)([/:[:space:]"'\'']|$)'
scan_current_regex "developer home paths" '([A-Za-z]:\\Users\\[^\\]+|/Users/[^/]+|/home/[^/]+)'
scan_current_regex "main config deployable credential defaults" 'src/main/resources/.*(password|secret|token|api[_-]?key)[[:space:]]*:[[:space:]]*\$\{[A-Z0-9_]+:[^}]+'
scan_current_regex "docs with deployable fake credentials" '(password|secret|token|api[_-]?key)[[:space:]]*=[[:space:]]*(password|secret|changeme|admin|root)([[:space:]]|$)'
scan_history_regex "history high-confidence credential formats" 'AKIA[0-9A-Z]{16}|ASIA[0-9A-Z]{16}|ghp_[A-Za-z0-9_]{30,}|glpat-[A-Za-z0-9_-]{20,}|xox[baprs]-[A-Za-z0-9-]{20,}|sk-[A-Za-z0-9_-]{20,}|BEGIN (RSA|DSA|EC|OPENSSH|PRIVATE) KEY'
scan_history_regex "history private network literals" '(^|[^0-9])(10\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}|192\.168\.[0-9]{1,3}\.[0-9]{1,3}|172\.(1[6-9]|2[0-9]|3[0-1])\.[0-9]{1,3}\.[0-9]{1,3})([^0-9]|$)'
scan_history_regex "history private or local-only domains" 'https?://[^[:space:]"'\'']*\.(corp|internal|intranet|lan)([/:[:space:]"'\'']|$)'
scan_history_regex "history developer home paths" '([A-Za-z]:\\Users\\[^\\]+|/Users/[^/]+|/home/[^/]+)'

tracked_cache=$(git ls-files | grep -E '(^|/)__pycache__/|\.pyc$' || true)
if [ -n "$tracked_cache" ]; then
  fail "tracked generated cache files"
  echo "$tracked_cache"
else
  pass "no tracked generated cache files"
fi

unexpected_authors=$(git log --all --format='%an <%ae>' | sort -u | grep -Ev '^(codex <codex@example\.com>|.* <.*@users\.noreply\.github\.com>)$' || true)
if [ -n "$unexpected_authors" ]; then
  fail "unexpected public git author metadata"
  echo "$unexpected_authors"
else
  pass "git author metadata"
fi

denylist_file="${SANITIZATION_DENYLIST_FILE:-.git/info/sensitive-denylist}"
if [ -f "$denylist_file" ]; then
  ordinal=0
  while IFS= read -r term || [ -n "$term" ]; do
    case "$term" in
      ""|\#*) continue ;;
    esac
    ordinal=$((ordinal + 1))
    scan_current_fixed_private "$ordinal" "$term"
    scan_history_fixed_private "$ordinal" "$term"
  done < "$denylist_file"
else
  pass "local sensitive denylist not configured"
fi

if [ "$FAILED" -ne 0 ]; then
  echo "Sanitization checks failed."
  exit 1
fi

echo "Sanitization checks passed."
