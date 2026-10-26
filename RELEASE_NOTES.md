# ListenTv 1.2.0 — catalog corrections

- Remove Rai Radio 2, keeping television channels in the catalog.
- Show TV8 in the Discovery filter, retaining the working Sky playback source.
- Add La7 Cinema, San Marino RTV and San Marino RTV Sport under More/Altri.
- Add the new channel logos and resolve RTV signed live URLs dynamically.
- 48 television entries in total; existing favorites and custom sources remain.
- Use the same permanent release key as stable 1.0.0 and 1.1.0.

HTTP checks validated La7 Cinema's authorization and DASH manifest and both
San Marino HLS master/media playlists. These checks do not validate hardware
DRM decoding or physical Android Auto playback. The previously documented
upstream K2/Frisbee availability limitation remains.

The GitHub repository remains private. No public publication is included.
