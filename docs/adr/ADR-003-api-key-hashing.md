# ADR-003: API Key Hashing Strategy

**Date:** Day 4 of 14  
**Status:** Accepted

## Context

API keys are stored in the database and looked up on every authenticated
request. Storing raw keys is a security risk if the database is ever
compromised. I needed a hashing strategy that is secure and fast enough
for per-request use.

BCrypt is designed to be slow to defeat brute force on human-chosen
passwords. API keys are long randomly generated strings, not passwords.
Running BCrypt on every request would add significant latency with no
real security gain.

## Decision

I use SHA-256. The hash is stored, the raw key is never persisted. On
each request the incoming key is hashed and compared to the stored value.

## Consequences

These decisions enable:
- Safe storage with no raw secret in the database
- Fast per-request authentication with negligible hashing overhead
- Simple key rotation by issuing a new key and updating the hash

These decisions rule out:
- BCrypt for any API key use case in this system
- Storing or logging raw keys anywhere in the stack