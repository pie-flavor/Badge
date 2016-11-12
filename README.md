# Badge

Badge is a small plugin that puts prefixes on player names, both in chat and above their heads. Ordinarily, most permission plugins would implement this, but if they don't, then this plugin is for you.

### Configuration

`prefixes[]`: This tag holds all prefixes. Each prefix has two tags, `name` and `display`. `name` is how it's represented in the permission node, as `badge.prefix.<prefixname>`. If the name has dots, they are converted to underscores. `display` is how it appears in-game, using the [color code syntax](https://wiki.ess3.net/mc/).

### Caveats

This plugin will break most other plugins that depend on scoreboards, as scoreboards are the only way to add prefixes on heads. This may be fixed in the future.

### Changelog

1.0.0: Broke scoreboards.
