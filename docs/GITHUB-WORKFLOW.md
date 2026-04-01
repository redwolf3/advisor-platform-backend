# GitHub Issues Workflow

Every change to this repository — planned feature or reactive fix — is tracked with a GitHub Issue. Issues are written as features or changes with clear scope. Commits reference issues by number. PRs close issues automatically on merge. No code lands on `main` via a direct commit.

---

## Issue Creation

**When to create an issue:**
- Before starting any work session that produces commits
- When a design spec or plan is approved and implementation is about to begin
- When scope expands mid-session (new issue for new scope — don't widen the current one)

**Title format:** `[type] Short imperative phrase describing the outcome`

| Type | Use for |
|---|---|
| `[feat]` | New functionality |
| `[fix]` | Bug or behavioral correction |
| `[test]` | Test-only changes |
| `[docs]` | Documentation, diagrams, ADRs |
| `[chore]` | Build config, tooling, non-production |
| `[refactor]` | Restructuring with no behavior change |

**Labels:** Apply one type label + one phase label to every issue.

| Label | Color | Meaning |
|---|---|---|
| `feature` | `#0075ca` | New capability |
| `fix` | `#d73a4a` | Bug or correction |
| `docs` | `#cfd3d7` | Documentation only |
| `chore` | `#e4e669` | Build, tooling, config |
| `refactor` | `#a2eeef` | Restructuring, no behavior change |
| `phase-1` | `#7057ff` | Phase 1 scope |
| `phase-2` | `#008672` | Phase 2 scope (deferred) |
| `in-progress` | `#fbca04` | Actively being worked |
| `blocked` | `#b60205` | Blocked |

---

## Branch Naming

Pattern: `{branch-prefix}/{issue-number}-{kebab-case-description}`

| Issue type | Branch prefix |
|---|---|
| feat | `feature/` |
| fix | `fix/` |
| docs | `docs/` |
| chore | `chore/` |
| refactor | `refactor/` |
| test | `test/` |

Note: the `feat` issue type maps to the `feature/` branch prefix; commit messages use the shorter `feat:` type token.

Rules:
- Always branch from `main`: `git checkout -b feature/42-description main`
- Never commit directly to `main`
- Delete branch after PR merges

---

## Commit Messages

Format: `type: short description (#issue-number)`

The `(#N)` reference links the commit to the issue on GitHub without closing it — closing happens only via the PR.

```
feat: add openapi-generator-maven-plugin to pom.xml (#1)
refactor: extract VisitorSessionDelegate from ChatController (#2)
docs: add ARCHITECTURE.md with C4 context diagram (#3)
```

Rules:
- Type must match the issue type (feat/fix/docs/chore/refactor/test)
- Lowercase, imperative, under 72 characters including the issue reference
- No `closes` or `fixes` in commit messages — those go only in the PR body

---

## Pull Requests

The PR body's `closes #N` line is the only mechanism that auto-closes the issue on merge.

Use the PR template (`.github/pull_request_template.md`). Fill in:
- Summary of what the PR does
- Test evidence (`mvn test` output)
- `closes #ISSUE_NUMBER`

---

## Issue Lifecycle

| State | Action |
|---|---|
| Open | Issue created, work not started |
| In Progress | Add `in-progress` label; post: "Starting work. Branch: \`feature/N-desc\`" |
| Progress update | Post comment: "Progress: [done]. Next: [remaining]. Commits: [list]" |
| PR opened | Post comment: "PR opened: #PR_NUMBER" |
| Closed | Happens automatically when PR merges — no manual close needed |
| Blocked | Swap `in-progress` for `blocked`; post comment explaining blocker |

Progress comments are the breadcrumb trail. Post one after each logical chunk of work, especially before ending a session without a PR.

---

## Claude Code Session Protocol

**At session start:**
1. `git status` + `git log --oneline -5` — understand current state and issue references
2. `gh issue list --state open` — see active issues
3. State: "Resuming issue #N on branch `feature/N-desc`" or "Starting fresh"

**Before writing code:**
1. Confirm a GitHub issue exists for the work
2. If not: `gh issue create --title "[type] Description" --body "..." --label "feature,phase-1"`
3. Add label: `gh issue edit N --add-label "in-progress"`
4. Create branch: `git checkout -b feature/N-description main`
5. Post start comment on issue

**During work:**
- Commit frequently with `(#N)` references
- Post a progress comment after each logical milestone

**When session ends or feature is complete:**
1. Run `mvn test` — all tests must pass
2. Post final progress comment on issue
3. If feature complete: open PR with `closes #N` in body, post PR link on issue
4. If not complete: commit everything that compiles, push branch, post progress comment

---

## One-Time Setup

Run once to create labels:

```bash
gh label create "feature"     --color "0075ca" --description "New capability"
gh label create "fix"         --color "d73a4a" --description "Bug or correction"
gh label create "docs"        --color "cfd3d7" --description "Documentation only"
gh label create "chore"       --color "e4e669" --description "Build, tooling, config"
gh label create "refactor"    --color "a2eeef" --description "Restructuring, no behavior change"
gh label create "phase-1"     --color "7057ff" --description "Phase 1 scope"
gh label create "phase-2"     --color "008672" --description "Phase 2 scope (deferred)"
gh label create "in-progress" --color "fbca04" --description "Actively being worked"
gh label create "blocked"     --color "b60205" --description "Blocked"
```
