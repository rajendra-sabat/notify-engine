# ADR-004: JSONB for Template Variables

**Date:** Day 4 of 14  
**Status:** Accepted

## Context

Notification requests include a map of template variables such as 
username, brand name, and billing amounts. These variables differ 
per notification type and will grow as new templates are added. 
I needed a storage strategy that avoids schema changes every time 
a new variable is introduced.

I considered a separate notification_variables table with key-value 
rows, and a JSONB column on the notifications table. The separate 
table is fully relational but adds a join on every read and requires 
no querying inside the structure. JSONB stores the map as parsed 
binary, supports indexing if needed later, and requires no schema 
change to add new variables.

## Decision

I store template variables as a JSONB column on the notifications 
table, mapped to Map<String, String> in Java using the JsonType from 
hypersistence-utils.

## Consequences

These decisions enable:
- Adding new template variables without any schema migration
- Clean Java mapping via hypersistence-utils JsonType
- Future JSONB indexing if variable-level querying is needed

These decisions rule out:
- Relational querying across individual template variable values
- A separate variables table
