# Shared Context Merge Decisions

This document captures the decisions made when deciding what to consolidate
from the three chatbot system prompts into `_shared_context.md`. It replaces
the PW-4 baseline diff snapshot the original plan called for — after the
Release A/B changes landed, most cross-prompt duplication was already gone,
so the merge surface is narrower than the plan anticipated.

Scope reviewed (at HEAD):
- `guardrails_system.md`
- `sql_generator_system.md`
- `error_recovery_system.md`

## Block-by-block decisions

### Enum values table

- Lives in: `sql_generator_system.md` only (lines 38-62).
- Not referenced by: `guardrails_system.md` (classifier doesn't emit SQL),
  `error_recovery_system.md` (error_recovery has access to
  `{schema_context}` which carries the same data).
- **KARAR: Keep inline in `sql_generator_system.md`.** No duplication
  exists, so consolidation would add a placeholder for zero gain.

### Role definitions

- `guardrails_system.md` lines 63-91: defines role-boundary violations
  (classifier purpose — reject what's outside each role).
- `sql_generator_system.md` lines 14-30: defines ownership-word semantics
  (query purpose — what does "my" mean for each role).
- **Wording differs because purpose differs.** Boundary-check prose
  (ADMIN has no boundaries / INDIVIDUAL can't query all users) vs.
  ownership interpretation (ADMIN's "my" means platform-wide / INDIVIDUAL's
  "my" means user_id-filtered) are distinct concerns.
- **KARAR: Keep both inline, do not consolidate.** Merging would force a
  compromise that's weaker than either purpose-specific version.

### Anaphora / demonstrative vocabulary

- `guardrails_system.md` lines 39-41 (OVERRIDING RULE 0) and lines 176-181
  ("Recent conversation context"): same vocabulary repeated twice in the
  same prompt, with slight wording drift between the two occurrences.
- `sql_generator_system.md` lines 134-135: prose mentions a handful of
  sample tokens for illustration; the authoritative detection is in
  `graph/nodes/sql_generator.py::_ANAPHORA_TOKENS` (deterministic regex
  runs before prompt render).
- **KARAR: Consolidate the vocabulary into `_shared_context.md`.**
  The guardrails prompt can reference `{shared_context}` once instead
  of duplicating the lexicon twice. `sql_generator_system.md` stays
  untouched because its prose is illustrative, not normative — the real
  gate is the Python regex.

### Status lifecycle / "successful order" filter

- `sql_generator_system.md` glossary: `CONFIRMED, PROCESSING, SHIPPED, DELIVERED`.
- `error_recovery_system.md`: doesn't restate — inherits `{schema_context}`.
- `guardrails_system.md`: doesn't care about specific status values.
- **KARAR: Keep in glossary only.** No duplication to fold.

## What ships in `_shared_context.md`

Just the anaphora vocabulary. The file is loaded by `guardrails.py` and
substituted into the `{shared_context}` placeholder in
`guardrails_system.md`. No other prompt references it today.

## What stays inline

- sql_generator's Business Term Canonical Definitions
- sql_generator's Enum values table
- sql_generator's Canonical Examples
- All three prompts' role/purpose-specific prose
- error_recovery's Error Type Guidance branches

## Known risk

If a future contributor edits the anaphora vocabulary in
`_shared_context.md` without updating `_ANAPHORA_TOKENS` /
`_ANAPHORA_STEM_RE` in `sql_generator.py`, the classifier (prompt-side)
and the history gate (code-side) can drift. Both sites should be kept
in sync manually until a single source of truth is wired.
