# Validation status

## Initial import — 2026-10-25

This repository starts from the TV Ascolto 0.1.0 source prototype, renamed to
`listentv` with the Java namespace and application ID `it.gianrolando.listentv`.
English is the default interface language; Italian translations are included.

Local checks: XML parsing, matching translation keys, Java package references,
resource references and repository contents. No Android SDK or usable JDK is
available locally, so compilation and Android lint are delegated to Actions.
The Actions run is the authoritative build result; it is not a device test.

The original prototype reported review of provider procedures and Media3
callback signatures. Those checks are inherited provenance, not new playback tests.

## Device tests still required

- Install and launch on API 23 and a recent Android version, in English and Italian.
- Play every broadcaster, including protected and unavailable streams.
- Listen with the screen off for at least 20 minutes and test notification controls.
- Test calls, navigation audio, audio focus and audio output disconnection.
- Browse and play from Android Auto without first opening the phone activity.
- Add/remove favorites and custom sources; test catalog updates and pagination.
- Test network loss, Wi-Fi/mobile changes, expired URLs and long pauses.
- Measure bandwidth for separate audio renditions and multiplexed streams.

Run interactive tests while parked or with Desktop Head Unit.
