## App Tiles

The "SciServer Apps" tiles at the bottom of the dashboard view can be configured via either of two configuration variables:

* **appTiles**: Tiles from this configuration and only this configuration will be displayed.
* **addAppTiles**: Tiles from this configuration will be appended to the default configured set (e.g. casjobs, if casjobs enabled, etc).

if both are supplied, **appTiles** takes precedence. Both of these variables expect the same JSON structure, which must
be a list of the following structs:

```js
{ "name": "the name of the tile, e.g. Compute",
  "description": "the description text, appearing below the name",
  "serviceUrl": "where the link sends the user in browser (in a new window)",
  // ONE of the following. If multiple given, preferred in order below
  "iconUrl": "the 'src' field of the icon image, this can be an href or static image data",
  "staticIcon": "a string that references one of the included images, e.g. 'compute'",
  "glyph": "a string representing the 'class' field of a span, for use with fontawesome/glyphicons, e.g. 'fa fa-search fa-3x'"
}
```

and the value must be given as a string to the config file, properly quoted. For example, in dashboard.env.js:

```js
'use strict'
module.exports = {
...
  appTiles: '[ {"name": "name", "description": "description", "serviceUrl": "serviceUrl", "iconUrl": "iconUrl"} ]'
...
```

