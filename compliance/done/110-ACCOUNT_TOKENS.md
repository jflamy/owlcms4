# Account Tokens

Status: implemented and accepted 2026-10-28 (unit tests and browser, both forms).
Tokens are created and managed here. The first endpoint that accepts them is the
competition export URL; the rest is tracked in
[120](120-TOKEN_PROTECTED_URLS.md).
Existing roles and account administration remain unchanged.

## Account UI

- The self-service **Account** page is specified and built in
  [100](100-PEM_ACCOUNT_MODE.md). This item adds the user's own token section
  to it; users still cannot manage other users' tokens.
- Administrators manage any account's token in the **User Accounts** form
  (Prepare Competition). A token generated for a selected user belongs to that
  user, not the admin.
- Both forms stage the change: the token is shown once when generated and takes
  effect when saved (**Update** in the User Accounts form, **Save token** on
  the Account page); cancelling discards it. The Account page hides the token
  section during a forced password change.
- The built-in admin account gets no token: it is a person's login, not a
  device key.
- Enforce ownership on the server. PIN/backdoor identities are not token owners.

## Token

- Exactly 12 case-sensitive characters from `A-Z`, `a-z`, `0-9`, `-`, `_`,
  generated uniformly with a cryptographically secure random source: 72 bits.
- OWLCMS stores only SHA-256(token), the owner, creation time and a non-secret
  identifier. Hashing does not add entropy.
- One active token per user initially, shared by any number of clients.
  Use a separate token record to allow multiple named tokens later.
- **Generate**, **Replace**, **Revoke**. Show the full token only when
  generated. Replacing atomically replaces it, confirmed by saving the form;
  revocation removes access without replacement.
- Persist across restarts; no automatic expiry initially.
- The token represents the account's current permissions, not a saved copy.
  Revoked/replaced tokens and disabled/deleted accounts cannot authenticate.
- Never log raw tokens. Audit lifecycle actions by account and token identifier.
  Use the normal translation process for UI text.

## Acceptance

Verify self-only versus admin access, secure generation, hash-only storage,
restart persistence and one active token even with concurrent generation.
Verify that a token still works after the database is exported and imported,
and that the export contains no raw token.
Verify replacement, revocation, account disabling/deletion and role changes.

## Decided

- A token is a key to its owner's rights, not a copy of them: a user owns one or
  more tokens (one for now), and a token authorizes exactly what its owner may
  do at the time of use.
- Separate parties get separate accounts. Two external broadcasters get two
  accounts with the same roles, each with its own token, so the audit trail
  shows which one acted.
- Tokens keep working across a recovery: exports carry each token's SHA-256
  (as they carry password hashes), and an import restores them. The raw token
  is never exported.
- Password changes do not revoke tokens. A token is not accepted as a password.
