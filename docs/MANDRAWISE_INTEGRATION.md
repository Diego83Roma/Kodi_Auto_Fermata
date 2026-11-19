# MandraWise / Wiseplay integration experiment

This branch starts from the preserved Fermata Auto #151 commit
(`08274c383966c2ad80b96cc8442c483dc4de2527`). The stable base is not changed.

## First increment: W3U catalogue reader

`me.aap.fermata.wiseplay.WiseplayW3uParser` parses the documented Wiseplay
JSON-based W3U structure:

- playlist metadata: name, author, image and info;
- top-level groups and stations;
- nested station collections (used for folders and provider lists);
- item image, URL, subtitle, embed and isHost fields.

The parser is deliberately isolated and does not yet change Fermata's navigation,
browser, playback engine, Android Auto services, or projection implementation.

## Planned integration

1. Fetch the configured MandraWise URL on a worker thread with bounded size/timeouts.
2. Parse the W3U document and expose its groups/items as browsable Fermata items.
3. For an item with nested providers, show the provider choices just like Wiseplay.
4. Open ordinary direct media URLs with Fermata's existing player.
5. For web-page/embed/provider URLs, use the existing Web component only as a resolver/browser;
   hand off to the player only after a real media URL is detected.
6. Validate the actual MandraWise response and provider behavior before claiming compatibility.

## Important limitations

A W3U entry's `url` is not guaranteed to be a direct video stream. Wiseplay's own
format supports both media URLs and web/embed entries. A page URL may require JavaScript,
cookies, redirects, or a provider-specific resolver. This parser only preserves those
fields; it does not bypass access controls or resolve provider pages by itself.

The screenshots supplied for MandraWise show a title list followed by multiple provider
choices. The exact live W3U JSON has not yet been retrieved, so nested parsing is based on
the public W3U schema and must be checked against the actual list before UI integration.
