# echoStorage

## Agent skills

### Issue tracker

Issues live as GitHub issues in `HefKer/echoStorage`, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles, each label string equal to its name. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context — one `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### Workflow

Solo repo, no pull requests. Work on a short-lived branch, merge it into `main`
locally (`git merge --no-ff`), and push. Small changes may be committed directly
on `main`.

Do not run `gh pr create` or `gh pr merge` — issues are closed by a closing
keyword (`Closes #12`) in a commit message, which GitHub honours as soon as that
commit reaches `main`.
