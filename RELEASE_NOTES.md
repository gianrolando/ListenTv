# ListenTv 1.3.1 — code quality and reliability

- Cache immutable channel lists and ID lookups; refresh them when custom sources change.
- Avoid reloading unchanged player artwork and repainting unchanged channel selections.
- Encode Android Auto artwork on a separate worker, only for the requested page.
- Limit Android Auto catalog notifications to actual catalog/favorite changes and programme transitions.
- Retain valid cached Rai programmes during partial schedule failures, including failed daily requests.
- Validate cached programme titles, duration and retention window before publication.
- Reuse strict XMLTV timestamp parsers and skip timestamps for unknown channels.
- Ignore nested XML descriptions masquerading as programme titles or schedule entries.
- Try other Discovery stream variants when one variant uses unsupported protection.
- Remove the unused diagnostics subsystem, synchronous diagnostic writes, unused icons and permission.
- Remove stale favorites when deleting custom sources; preserve the existing 48-channel catalog.
- Apply consistent Java formatting throughout production code and regression tests.

No playback credentials, signing keys or guide listings are bundled. The repository remains private.
