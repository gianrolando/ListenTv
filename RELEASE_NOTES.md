# ListenTv 1.1.0 — expanded live catalog

- Expand Rai from 4 to all 15 channels in the current RaiPlay live catalog.
- Use high-resolution color Rai logos, including thematic channels.
- Add 11 Warner Bros. Discovery entries and a Discovery filter.
- Adapt WorldLiveTV's current anonymous Aurora playback procedure, including HLS-only sources and updated Discovery/Turbo channel names.
- Keep stable catalog IDs, favorites, custom sources, background audio and Android Auto integration.
- Sign with the same private release key as 1.0.0 so this installs as an update.

All 15 Rai manifests and 9 Discovery manifests returned HTTP 200 during checks.
K2 and Frisbee authorization succeeds, but their CDN currently returns HTTP 504
for both formats; they are included with source availability dependent on the
broadcaster. Paid Discovery services are not included.

The repository remains private; no public release is part of this delivery.
