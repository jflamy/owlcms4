# Account Tokens

Status: agreed 2026-10-25; not implemented. Implement this before token-protected
URLs, and after the self-service account page in [005](005-PEM_ACCOUNT_MODE.md).
Existing roles and account administration remain unchanged.

## Account UI

- The self-service **Account** page is specified and built in
  [005](005-PEM_ACCOUNT_MODE.md). This item adds the user's own token section
  to it; users still cannot manage other users' tokens.
- Administrators reach **Accounts** through Admin and manage all users.
  A token generated for a selected user belongs to that user, not the admin.
- Enforce ownership on the server. PIN/backdoor identities are not token owners.

## Token

- Exactly 12 case-sensitive characters from `A-Z`, `a-z`, `0-9`, `-`, `_`,
  generated uniformly with a cryptographically secure random source: 72 bits.
- OWLCMS stores only SHA-256(token), the owner, creation time and a non-secret
  identifier. Hashing does not add entropy.
- One active token per user initially, shared by any number of clients.
  Use a separate token record to allow multiple named tokens later.
- **Generate**, **Regenerate**, **Revoke**. Show the full token only when
  generated. Regeneration requires confirmation and atomically replaces it;
  revocation removes access without replacement.
- Persist across restarts; no automatic expiry initially.
- The token represents the account's current permissions, not a saved copy.
  Revoked/replaced tokens and disabled/deleted accounts cannot authenticate.
- Never log raw tokens. Audit lifecycle actions by account and token identifier.
  Use the normal translation process for UI text.

## Acceptance

Verify self-only versus admin access, secure generation, hash-only storage,
restart persistence and one active token even with concurrent generation.
Verify replacement, revocation, account disabling/deletion and role changes.

## Still to decide

Whether password changes revoke tokens, and whether token verifier records
are restored from recovery exports. OWLCMS must never export the raw secret.
