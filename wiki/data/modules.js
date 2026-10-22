window.WIKI_MODULES = [
  {
    "id": "simplebuilding",
    "displayName": "SimpleBuilding",
    "description": "Building tools, storage, machines, and enchantments.",
    "version": "2.0.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth-config",
      "simpleovens",
      "simplemobheads",
      "simplecrucibles",
      "simpleoctets",
      "simpleblocks",
      "simplehammers",
      "simplestorage"
    ],
    "optional": [
      "jei",
      "roughlyenoughitems",
      "modmenu",
      "jade",
      "appleskin",
      "mousetweaks",
      "trinkets_updated",
      "curios",
      "simplemaps",
      "simplequestionary",
      "simpleriding",
      "simplesandwiches",
      "simpletweaks",
      "simplemoney",
      "simplefun",
      "simpledimensions",
      "simpletrims",
      "simplemobs",
      "simpleend",
      "simplerespawn",
      "simplebeacons",
      "simpleshops",
      "simplewanderers"
    ],
    "overview": {
      "status": "in Arbeit",
      "open": "86",
      "missing": "Modulfamilie-Umzug läuft: Blocks, Tiegel, polierter Endstein, Octets fertig; Hämmer in wave1 (noch nicht im master, check rot); offen Gadgets, Lager (Stufe 8), Enderite, Release (Stufe 9); dazu Abnahmen im Spiel.",
      "asOf": "19.10.2026"
    },
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA0UlEQVR42mO4devWf0owA00NSEpKwhBLTk4GYwwD0BUbGxtj1fjnzx8w1tDQwHQBsiYFBQUUzQcOHABrfPfuHZiN1QB0jdg0X7hw4X9iYiJuA7BpBmmEYWTbUQwAOR+bs5E1o9sONwBbaMMMAWkCYWy2Y/WCljgLGAtwMv0XEREBawBhkM0EDQBp7PDn+S8nwAxmI8vBDMKZkGCa7VRY/2fZcv7nZGUkPiXCNEeacGDVjBy4oMBGDjMUA9A1Iycs9JSJ1QvImrHFDEgMXZzi3AgA+fgUIAeG5JAAAAAASUVORK5CYII=",
    "counts": {
      "items": 539,
      "blocks": 318,
      "recipes": 855,
      "config": 226,
      "features": 47
    }
  },
  {
    "id": "wiringexample",
    "displayName": "Wiring Example",
    "description": "Minimal module used to verify loader and integration wiring.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [],
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAq0lEQVR42mMwt7L9TwlmABHbdu0nGXdNmIFqwKwFy/6fOnsRTIOwFRcHCoaJgzBIc3JGLqYByBik6YaGxP9ZUgIoBoA0gywiygCQZpAhMANAmmGYZBcga8YwAN2/MPw/ywuMccmjGACzDUaDNV/YAMFANrIczFsoBqArQDcA3QIMAyjyAnogggKJpEBENgCmAD0aiTIAWQHJLgClLnQDkDFeA2ACpGK4AZRgAFr1JhbFyc+5AAAAAElFTkSuQmCC",
    "counts": {
      "items": 1,
      "features": 1
    },
    "dataHash": "b1da0996bb98"
  },
  {
    "id": "simplemoney",
    "displayName": "Simple Money",
    "description": "Currency production, weighted villager trades and treasure loot.",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "jei",
      "modmenu",
      "simplebuilding",
      "simplefun",
      "simplemodels",
      "simplequalityoflife",
      "simpleriding",
      "simplesandwiches",
      "simplesounds",
      "simpletweaks",
      "simplevisuals",
      "wiringexample"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAABoUlEQVR42r2TS0sbURiG/V2CgjQBtaUxxFEhtQS7ELWWpG4CophfoFatmlSdgAUxCyt00ZBKQUSCF7zhQry1CUWN8TImxpnODI8ns3e0iC5evsN3Pp7znpdzSuKJOR6jkicBtL59g8/twVvtsvSuxm3VewHFofWpTo4Xo3g/uzBNzVLHpGT11r52ERkO3w0onnqa7EFN/eCV7KB+vpT2ZA37qRhqOs7FZpTJ0cG7AaEWL6Z+jKn+FVWhOdaAS35BINYkeinMm12+jEbsHRhnK6BfEpQ/IEWq6E2E2D/dAO0E/eQX8ti4fQa/EyMWJDAngalgahn2MutincO4WrV3UAQUdnppCzfyWlhvm/axuBenccJD93QQXdftQ+xwV2BcH4ornPMy6iCV3UJe/kTfQgizILLRzxgYHLLPQMunhQ7oX+oS9s9R81kBWkK9yWLkth8Qohgq5I7w/6zD0BT06yP+ZDYtwD9lyx4QHhoh+L6Wb2N+8tkdtEvhRkmzlhzgu/zR2puZmb3/KTfVS5SXlSE5q6l1OvBUOvFJnmf8TP+jW3RFGn0zsn8bAAAAAElFTkSuQmCC",
    "counts": {
      "items": 8,
      "recipes": 9,
      "features": 73
    },
    "dataHash": "781c10aed481"
  },
  {
    "id": "simpleriding",
    "displayName": "Simple Riding",
    "description": "Saddle speed, horse jumps, nautilus dashes, mount armor utilities, loot, and trades.",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplebuilding",
      "modmenu"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA2UlEQVR42mO4devWf3z4//MpOOW+7C/4z4AuQCz/+6M+MM2AbipMAp2NCzOAnIhuEzr2kGD/X6MvCMcoBhCyDab5f1cIGL+OV0YxhAGfrTDNIE0gvNpVAmwIiIYZgmIAsjNBimAakTFMHMMAbM7EZghMXFuADWIALABhToY5E6QYxke2HWQASBxuAHpgIWsC0TA+DCNrxhkGME3obJBGEB9rNOKLe5BGmK0wAzBSIno6gGlCdi7c4MwYRF4gJiUi422z2v772FvCMxpJeQFbasWbG3HlSOQsDgBbY+6dp3ZhZwAAAABJRU5ErkJggg==",
    "counts": {
      "items": 8,
      "recipes": 8,
      "features": 35
    },
    "dataHash": "9ecd3270fe72"
  },
  {
    "id": "simplemodels",
    "displayName": "Simple Models",
    "description": "Server-approved custom item models, anvil assignment, and a searchable model browser.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [
      "simplebuilding",
      "modmenu"
    ],
    "parent": "simpleaesthetics",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAn0lEQVR42mMQldL6TwlmABGNnYuxYq1ZB8AYm5yJQxaqAQHR9f+XrNwDpmEaQWxsfJBmVT0fTAPQFaJjmDzIIhQDCGlEthmEYepRDAApmPfhPxjj0gyThxmCYQAMoxuErBHZFTgNwKeRfgag+5kkA2CKSDIAZgiyIlwGYEQjekLCZQCyPEZKBKUudIUwA9ANxjAAJoCOYRqxyaEYQAkGACSFGALwloT6AAAAAElFTkSuQmCC",
    "counts": {
      "items": 1,
      "recipes": 1,
      "features": 14
    },
    "dataHash": "5173fcf52c53"
  },
  {
    "id": "simplefun",
    "displayName": "Simple Fun",
    "description": "Bounded playful mechanics, throwable bricks and cosmetic transformations (the animal heads moved to Simple Mobheads in 1.4.0).",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "modmenu",
      "jei",
      "jade"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAh0lEQVR42mOw8Iv9TwlmABE7bn0kGfcceYFqwJwzr/+ffvIVTBPCIM2pvcsxDSBWM8ginAZ4yYsQxDhdAJI8WRWFF68IsgKrwzCAWM15Bgq4DQApwIdBmgka0OVggKERJkaUAdg0Eu0CmAJ8mGAsEBOFOA2ASRKDMQwApS6yDYAJkIrhBlCCAZ7ocpZYjVK8AAAAAElFTkSuQmCC",
    "counts": {
      "items": 2,
      "recipes": 2,
      "features": 38
    },
    "dataHash": "239fe29a2e87"
  },
  {
    "id": "simplevisuals",
    "displayName": "Simple Visuals",
    "description": "Bounded client HUD, tooltips, anvil formatting, and Vanilla immersion particles.",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplebuilding",
      "simplemodels",
      "modmenu"
    ],
    "parent": "simpleaesthetics",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAABgWlDQ1BzUkdCIElFQzYxOTY2LTIuMQAAKJF1kb9LQlEUxz9qUZRhUENDkIQ1ZZiB1NJglAXVoAb9WvTp08Afj/eMiNagNSiIWvo11F9Qa9AcBEURRHOtRS0lr/M0UCLP5Z77ud97zuHec8EezShZo84H2VxBD4eC7rn5BXfDK05aseGhK6YY2nRkPEpN+7yXSLFbr1Wrdty/1pxIGgrYGoVHFE0vCE8IT60WNIt3hNuVdCwhfCbcp8sFhe8sPV7mF4tTZf62WI+GR8HeKuxOVXG8ipW0nhWWl+PJZlaU3/tYL3Emc7MRWbtldmIQJkQQN5OMMUqAAYbFB/Dip1921Mj3lfJnyEuuIl5jDZ1lUqQp0CfqilRPyqqKnpSRYc3q/9++Guqgv1zdGYT6Z9N874GGbShumebXkWkWj8HxBJe5Sn7+EIY+RN+qaJ4DcG3A+VVFi+/CxSZ0PGoxPVaSHDLtqgpvp9AyD2030LRY7tnvOScPEF2Xr7qGvX3olXjX0g9t+GfpNyGCNQAAAAlwSFlzAAALEwAACxMBAJqcGAAAAdBJREFUKJFlUjFo20AUfTp9nQylWWwQGGtJsySQkkkYCh06FDp7KF1STMC0NDHNoMWTS7t5CcZD6SI6ZCiN505dDO3gbim4BIqH4LiYKhlaGmrrpOvwhawmx3F3uv/+e1/3vrF3dOZ7cWdoAujWyk/fT5EbloDvxa7rNvvpvbF3dAagvjoLxg4A34vbA82xGwXKuLIheGN0lCAMQ6Uinn/+qjAMowQsniporQE0+9MowWKhlIp0oji2v3XZGxWlpFZ1qWNebDY+fPvte/F64efnH5ZO1Hy+GLy4X77z6EtY0BrP1i8Ojm8mGqaBbq1MrNgeaKVWGJ2VO58vbBsHxytEanfjvFQqNftTApaV5NH5HKXQGxWlNAFQPvzp1YPr53svP+ZfOU14+/gWANQn/FmpVCaTSZ5r5/CUDwRASto5PM2qevdkk2MPX3+1bWkIIrKkTKlFqxo31mZSEpFlCLJtuR2cANgOTq6gG2uzpdPs2hUfGL27cZ4563ux4K2+OuPant/+ZQgyBO1vXTJ3MHaCseN7MYDO0BTdWrkzNIOxYwm0qvGb7w6RRWT1RsX2XcNKWweu66b/kLUhZ7IOzwwEoNmfcjv95wO/9HUQk/L6Dx4w4ZYODKssAAAAAElFTkSuQmCC",
    "counts": {
      "features": 77
    },
    "dataHash": "c45e2e2abc1f"
  },
  {
    "id": "simplesounds",
    "displayName": "Simple Sounds",
    "description": "Bounded local Vanilla sound counterparts for the twelve Simple Visuals immersion effects.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplevisuals",
      "simplebuilding",
      "modmenu"
    ],
    "parent": "simpleaesthetics",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAmElEQVR42mNICHP4TwlmABHfnh8hGX+4sQDVgE931/7/8fYKmCaEQZrXzinGNAAZa2lpgTE2zSCL8BoA0jhr1iwMA6wWnAAbgNUFMBtBODs7G8UAZI0wNlYDQBqRMcwAZI0wNtEGoNtMWwOwhQE2zQSjETkWYBrRDcJpAEwBzDXIBiBjrAbgUkyUAaDURbYBMAFSMdwASjAABHrCsb/q5xYAAAAASUVORK5CYII=",
    "counts": {
      "features": 20
    },
    "dataHash": "033c5483dd22"
  },
  {
    "id": "simplequalityoflife",
    "displayName": "Simple Quality of Life",
    "description": "Super-mod: bundles Simple Weather, Simple Interfaces, Simple Loot, Simple Mob Manners, Simple Farming, Simple Tools and Simple Movement with one switch each; guide book and start quest.",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config",
      "simpleweather",
      "simpleinterfaces",
      "simpleloot",
      "simplemobmanners",
      "simplefarming",
      "simpletools",
      "simplemovement"
    ],
    "optional": [
      "simplebuilding",
      "modmenu"
    ],
    "overview": {
      "status": "in Arbeit",
      "open": "16",
      "missing": "Grundstock fertig; neu Item-Rahmen (in wave1) und Container-Tooltips (auf Branch), Bündel-Ansicht geplant; Abnahmen im Spiel offen.",
      "asOf": "19.10.2026"
    },
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAq0lEQVR42mOwD3P6TwlmABGHn50gGc+7tgzVgDV3Nv2//PY6mIZhKxUBMEYWA2GQ5qJZFZgGoGs+uSQLjJENAWkGWUTQAGwuAGmGYbwGwDSiY6IMAClcUWKDgbujtFEMwWoAsmaQBnSc5aQANwTDAGyarxlYodDIhuA0AFkzOiZoALqT0TWTbAA25+M0ABaI2AxB14w1EEGpCySBKw2gpwUUA2ACpGK4AZRgANECVTHhFbnoAAAAAElFTkSuQmCC",
    "counts": {
      "items": 1,
      "recipes": 1,
      "features": 9
    },
    "dataHash": "8deca1391c5f"
  },
  {
    "id": "simpletweaks",
    "displayName": "Simple Tweaks",
    "description": "Legacy compatibility and server-authoritative claims, on by default.",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simpledimensions",
      "simplebuilding",
      "simplemoney",
      "simpleweather"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA+klEQVR42o2TIQ7CQBBFewZSTUjqKkhRCBBVOEwF6REgGBQGwwHQVcgegRoOATfgBBUI/JI/yd902O4W8buzk/7XmdlttFnPzaTIgsI7PkUElPXWVI+rrN04vxRe8Cgba8DxfhYDAYgh5qeH3MaQAsDwui1N+2kdIQ/jr5wK8PKqKh0hz9ZgRCVOBUiEAH3tWACCoQrQIqVawIP0f1tQQ2T/Q4A6ScxzMZNVtcATCJ0CTO/TTgTIPo7FLBeJFfguEgQTzDD2AngsvpvI8tmCAiDAYDigrpgDDCYKewXgVPlFxtwD0qSp7LkqgE9ojfe/C+AR2r/RpxCcgC81FzUy44IBpwAAAABJRU5ErkJggg==",
    "counts": {
      "items": 2,
      "blocks": 1,
      "recipes": 4,
      "features": 39
    },
    "dataHash": "83589f4b01a1"
  },
  {
    "id": "simpledimensions",
    "displayName": "Simple Dimensions",
    "description": "Configurable dimensions, light arches, and safe linked portals.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplebuilding",
      "modmenu",
      "flan",
      "ftbchunks",
      "openpartiesandclaims",
      "griefdefender",
      "claimchunk"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAlUlEQVR42mPwc475TwlmABG3Tn4AYy42IaIwSO2RFc9QDTiz5RVY0lor5r+UgAZWDJIDqQFp7q1cht0AkMIq/Qtg7Cc1A0xvKPgBpkFyIDVPbnwhbABME8wQZAMIugBZE7JBRBuA7gWYQUQbALMV3SCSXADShG4Q2V5AjwWiDBi4WCA7JYJSF7F5AcMAmACpGG4AJRgArKknSmNVLV8AAAAASUVORK5CYII=",
    "counts": {
      "items": 1,
      "blocks": 2,
      "recipes": 1,
      "features": 7
    },
    "dataHash": "4d2fff5f6bf4"
  },
  {
    "id": "simplesandwiches",
    "displayName": "Simple Sandwiches",
    "description": "Sandwiches on a cutting board, a kitchen knife, cheese and butter from the cauldron, and eating straight out of bundles.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [
      "simplebuilding",
      "simpleend"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAoElEQVR42mNwCov5TwlmABEnnn0gGS+79gzVgE13Xv2//vYLmCaEQZorZi3DNACEvbQE8GKQZpBFWA0AKZgTpYQXwwzBMACm+dGOQrwYZghWAwhphmEMA0ACMXuM/uvXyoBpZDYuMZAeDBd8OmFEFMbphQspWWD8oqsfzsYmRv0wQI4FYjBWA4hJRMiJCcMAUOoCSRCLUQyACZCK4QZQggG7vYVNITlFHQAAAABJRU5ErkJggg==",
    "counts": {
      "items": 6,
      "blocks": 18,
      "recipes": 17,
      "features": 6
    },
    "dataHash": "fc7348a0ab85"
  },
  {
    "id": "simplelib",
    "displayName": "SimpleLib",
    "description": "Shared content bundled by SimpleBuilding, Simple Crucibles and Simple Sandwiches: warm food, shared materials, id moves, in-world upgrades and UI building blocks.",
    "version": "0.2.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [],
    "overview": {
      "status": "fertig (wächst mit)",
      "open": "4",
      "missing": "Basis für alle Mods; offen Kreativtab-Zeilenlayout-Abnahme und Stil-Bausteine, wächst mit jeder Familienstufe.",
      "asOf": "19.10.2026"
    },
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA3klEQVR42mMIC3D4TwlmABHP7x4hGd84sQDVgLsX1v5/++wKmMaG3VUawBjEBmmeM6EY0wBcmkD444v//5fWvvwvw2UMtginAciaQBpAuC/5DgoGGYJiAC5NXy79x9BcHXgSjEGGgA0AaTq/6TdWm0D4eO1/FI0gtSBsp1gMNgRsQK7rDrAgNptgGmA41GAWGGMYADMEXSNMA8g7MI0wDDYARMAMgBmCrBHkBWRNMI3wMAARyIYgOxGbJhDGmg5ghuDThIyxpgNkDdg04TUAlLrwacBrAEyAVAw3gBIMAI2ehfZwc0OFAAAAAElFTkSuQmCC",
    "counts": {
      "items": 10,
      "blocks": 7,
      "recipes": 29,
      "features": 3
    },
    "dataHash": "b0e346d89e7d"
  },
  {
    "id": "simpleinterfaces",
    "displayName": "Simple Interfaces",
    "description": "Restyle of the Vanilla container screens (a coloured box per block, drawn in code), linked containers and Easy Shulkers / Ender Chests; a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config",
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAcUlEQVR42mPQsgj4TwlmABGLd9wlGWf1nEA1gIGBgSTskzoBYQBIgBRw7NgxsB4MA0ASxODQ0FDcBpibm8MxTAO6GEEDvG48QHERSKy7uxsuRtAAZL9iE8NrwOAIA2T/DqEwgEkQi1EMAGUMivICJRgA0mVXKrPxqV4AAAAASUVORK5CYII=",
    "counts": {
      "features": 43
    },
    "dataHash": "9e46375155db"
  },
  {
    "id": "simplemaps",
    "displayName": "Simple Maps",
    "description": "Wayfinder maps: endless maps per dimension with zoom, waypoints in the locator bar, framed views and cartography table upgrades.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplemoney"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA70lEQVR42mMIM9f6TwlmABHPty0mGd/oykI14O6s+v9vT+0B0yAc56GDgWFyIM1zkn0QBmBTvGN+0f9HF1egYHQ1KAZ8fnYEjH3zL4BpkAGxXSb/7bOV4AaAxGDqMAwAKQZhmCEwF4DEkA0AGQgSw2oATBLmAnQvwFyF1QUgQZgkzACQS0AaD1hqo3gBxMcZBtgMICoMkJ2P7AVkQ/AaAJNADwOrEIQhVjpScIzTAJD/sAUiSNOLncr/V1RCaBAfxQCYRmQnImOQhq9fP/y/ICEBpjEMQMeg5IqMQRpAtoM0Y7gAlrYJYeQwQMkLlGAAvJFvToS+xP8AAAAASUVORK5CYII=",
    "counts": {
      "items": 4,
      "recipes": 4,
      "features": 8
    },
    "dataHash": "a5974ff454b8"
  },
  {
    "id": "simpletrims",
    "displayName": "Simple Trims",
    "description": "Glowing, Emitting and Pulsating armor trim templates, placeable trim templates and trim upgrades (moved from SimpleBuilding, ids migrate). Axe on its own, sledgehammer with Simple Hammers.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "jei"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAABj0lEQVR42oWTzUsCQRyGOyp9f1C3ig4hQZFkhGVYaNShIrUvDYswRDEsMzLD1RIR+iBCgijo4KW6Bt3r2slrx6COXfoLfvUOzrTTah2eZXd23mfenWUqPFPd9BudrlJQ6r2aClw+Xm4ECFmsLjLN+mjQExIi9Rzw+rQpC96fM/T59igEHOvqOrnjB0KEeQjnjxxaAQ87U1kKnV9KIg4XlRXw8G2hQIn7B43A6t+i+oZWsT9CoK4OiToMIcZwjzAkJQXGPhujxzjK4OG7Yhv77h4TjEUVWcBXnw5GaClzSBNxRUgAwmjQ2NQm6kt7gIH+GTcLK9d5Go/GGMP+ELV39DIQ5EgCPAzN+cgW3ibvSY7s4R2aTGYo8H0PiVPZlwSYL/1GDIz4w7R8miNDl0XgPT6jxXSWSXj1soIBh4c6DWZJgCb+iyuqqW2hqupmJuFhjQABCEyulZ8GxU9AGDiS6dIC9eExza+R2RNksoVYilXW6+vItpGQwhoBH+AiCABWhsAaiIidVyMEfx3n/471F3h08EUyFpsCAAAAAElFTkSuQmCC",
    "counts": {
      "items": 4,
      "blocks": 1,
      "recipes": 8,
      "features": 6
    },
    "dataHash": "722f6419ce54"
  },
  {
    "id": "simplemobs",
    "displayName": "Simple Mobs",
    "description": "Hollow Howler and Hollow Watcher bosses, Shellker, Soulfire Blaze, Dread and Deceiver mobs, Blighted and Abyssal Wither with Nether Heart and Red Nether Gem, and the Potion of Silence.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA00lEQVR42mPwcgj4TwlmABGXj9zFwNY8AnCMzIfJ71hwAtWAQ2sv/L9/5RmYBims0nL47yWiAWbDxGB8kOaW4gmYBsAwugEwDOODLMJqALJimAHTtLT+H3K3AtPIBmIYABLMllD4/7QqBq4BhP93acHZMMNA6sAuQjcAXTM2A0AYpwHomsEGXM8i3wCY7ciuAGnGaQAsHLDZDjMEphlnLMAMwOZ3kIEEDYAZgm4AstNxJiSQBDavoGvGagAodcEkkQ3BphnDAJgAOkZOmdjk4QZQggG69BxeWTVzmgAAAABJRU5ErkJggg==",
    "counts": {
      "items": 20,
      "blocks": 2,
      "recipes": 62,
      "features": 12
    },
    "dataHash": "7190530b3fc0"
  },
  {
    "id": "simpleweather",
    "displayName": "Simple Weather",
    "description": "Weather switch (server) and rain density (client); a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplequalityoflife",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAnUlEQVR42mOQVtD5TwlmABHdk5aSjF0C8lANiMto+r96wz4wDcJZxQ0oGCYOwiDNhlb+mAYga/706RMKhhkC0gyyCKsBMNvQNSMbAsMYBuDTiMswuAGkaqbYgB0HzmAaABIkBaMYAAsDUjDWWLCvfwCPKhAG8WFsdD6GAaRoxpmQkBUQwhgGgFIXMTbD+CgGwARAkiAahgnx4QZQggHab2K50IVYOwAAAABJRU5ErkJggg==",
    "counts": {
      "features": 3
    },
    "dataHash": "bc9c18e432a3"
  },
  {
    "id": "simplemovement",
    "displayName": "Simple Movement",
    "description": "Manual crawling, server-allowed autowalk, faster ladders and Frost Walker on powder snow; a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplequalityoflife",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAmElEQVR42mNI8FX7TwlmABHfbswhGX84kIBqwKeTZf9/PN4OpglhkOa1Xa6YBiBjLXFuFIysGWQRUQasyLD63+2vBTcApBmGMQzwWvEAjrEZgKyZaC8QbQCy7TRzAQyTFQbIGGYIXheAFOAyAGYITgNgTgQp8tISxYlB8hgGoAcUemJCxhguAKUudAPwYRQDYAKkYrgBlGAAoGJxbA1gD88AAAAASUVORK5CYII=",
    "counts": {
      "features": 17
    },
    "dataHash": "6fc08806676c"
  },
  {
    "id": "simpletools",
    "displayName": "Simple Tools",
    "description": "Full-durability bonus, the Thrift and Soulbound enchantments, the Death Vault, the Biome Brush, anvil repairs without extra cost and lava buckets into furnaces; a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplebuilding",
      "simplequalityoflife",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAwUlEQVR42mMIc/D6TwlmABHPj1wmiEX4heAYxL+xYAeqAXfXHvr/9sp9MI2OQZpOvPgCxj07boD5c4pbMA0gRvODS5P+5+y3AYuTZACy5jk3o4gzAJdm+35t4g0Aad6/Ig1FM1EGgBTkn7z2PyYgDKtmgoEIUgTSfOP3//92xpZwjSAMikK8BoAUeTm5gjFIM7JGGMZpAEhhTlLKf0NtPawa8RqArhmbRpwGkKIZwwBSNWMYABKA+RckQSyGG0AJBgAtb4QCDYMvfgAAAABJRU5ErkJggg==",
    "counts": {
      "items": 2,
      "blocks": 1,
      "recipes": 4,
      "features": 32
    },
    "dataHash": "7b2c7b8013b7"
  },
  {
    "id": "simplefarming",
    "displayName": "Simple Farming",
    "description": "Feather Falling protects farmland, Sharpness cuts grass, a hoe harvests and replants ripe crops; a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplebuilding",
      "simplequalityoflife",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAkUlEQVR42mMw8Ir6TwlmABGrL78hGVfseIBqQPehJ/8P3f/4/8WBADAbhHGxQZrDWhZiGgBStKNNB0zjYoM0gyzCagBMEYjGxQYZQLELcBpAsQtAuGuWAQobpgGZjdcAZENgmtA14zXASkPgf5aXAgYGidPHAGQnk+wFdMUkBSIodaErwsdHMQAmQCqGG0AJBgArfJi0eSLOewAAAABJRU5ErkJggg==",
    "counts": {
      "features": 8
    },
    "dataHash": "c881b63a533d"
  },
  {
    "id": "simplemobmanners",
    "displayName": "Simple Mob Manners",
    "description": "Mute mobs by type or name tag, keep named babies young, piglins ignore gold trims and tools; a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplequalityoflife",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA3ElEQVR42mOIcQr4TwlmABEfTtyFYzUDHTBGFsMm/mzZCVQDXm26AFHw5QUYg9ggMWziIM3LKibgNkBJRQnDAJAYzIAv159hGgCSCFvi9r/lTAIYg9gwZyOLwcSxGmBSYAE3BKYYhmFiIDUYBoAEqtZ8AWOQAhiGaUYWg6kD6UExwKnoGkkYwwCdoA1gCZgN2DTBxEFqMQw4d+nW/zkLVhGFM3IqUA2ABSIxhsA0Y40FkAJ8hoA0EzSAkO04DQClLnyGIGvGSMowAZAkTCEuDLMdxQBkDEu+uDC6egCW7WeIdqcojAAAAABJRU5ErkJggg==",
    "counts": {
      "items": 1,
      "features": 11
    },
    "dataHash": "564d3430f293"
  },
  {
    "id": "simpleloot",
    "displayName": "Simple Loot",
    "description": "Vault cooldown in days instead of once per player; a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplequalityoflife",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAcElEQVR42mOIDVD4TwlmABEf704iGb84EYBqwOsLGf+/PtsApglhkOblE6xQDdCS4iEJYzVgRYnN/yNdXmAaH8ZrADGYdgag408XHMCYpDBAxjsmKIDxEAoDig3IclIgChMdC0SnRFjaJhXDDaAEAwDEoiaJlv+QRgAAAABJRU5ErkJggg==",
    "counts": {
      "features": 5
    },
    "dataHash": "13210b0e0ad4"
  },
  {
    "id": "simplequestionary",
    "displayName": "Simple Questionary",
    "description": "The Questionary Table: put any item in and read its lore, its use and a secret in an open book.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAg0lEQVR42mMw13L7TwlmABHbFp8jGXdlbUA1YFb97v+n9twG04QwSHOyTz2mAcRqBlmEYYCViggGhmnCJodiAEjgw4sLGPjEjglgjE0OpAfDgKysKjhGNwBdDqsBdHEBXgOo5gJsBtA+DKqstDAwLM5xyRFMSPgwSkKCpW1SMdwASjAAhoVuHqiCJG4AAAAASUVORK5CYII=",
    "counts": {
      "blocks": 1,
      "recipes": 1,
      "features": 2
    },
    "dataHash": "6e84609aa395"
  },
  {
    "id": "simpleaesthetics",
    "displayName": "Simple Aesthetics",
    "description": "Super-mod without content of its own: bundles Simple Visuals, Simple Sounds and Simple Models with one on/off switch each.",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplevisuals",
      "simplesounds",
      "simplemodels"
    ],
    "optional": [],
    "overview": {
      "status": "fertig (Abnahme offen)",
      "open": "6",
      "missing": "Funktionen fertig; offen nur Abnahme im Spiel (Modelle-UI, Stufen/Stärke der Effekte und Sounds) und Stil-Audit-Reste.",
      "asOf": "19.10.2026"
    },
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAp0lEQVR42mNIcHD4TwlmABHfjhwhGX9YsADVgE9r1/7/ceUKmCaEQZrXFhdjGgDCXnoiODFMM8girAaAFJ2YHoUTwwzC6gKY5hsGBmAM02SQcAOM0Q1CMQDZZmIMgBmC1QB8+MMFAzDGaYDBAgcwRvczWQbAAguE0V2IEQbIgYgt1NHFcKYDbBqwGYrTAGQn48IwNRgGgFIXTJIYjGIATIBUDDeAEgwA2P53d0BxE80AAAAASUVORK5CYII=",
    "counts": {
      "features": 1
    },
    "dataHash": "8f0a91afc1e7"
  },
  {
    "id": "simpleenddragonfight",
    "displayName": "Simple End Dragon Fight",
    "description": "Ender Dragon fight: +1 health bar per player on the island, a faster dragon, entry animations and Dragonmites from half health.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [],
    "parent": "simpleend",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAy0lEQVR42mOIinD4TwlmABFvXh4hGT+4swDVgCcP1/7/+P4KmBbhlsGKQXIgDNK8cF4xpgEwhfdvL/v/4d0xFAwSg8mDLMIwAKbx5JGu/9Yq/v8XxJwAYxAbhkFyMIOwGqAmZvzfQNYBjJE1wsRAGKQGwwBkf4JswOUFZG+AMIoBMOfDbMfmBZAcsjdQDAA5jVgvwLxBOy+AMLoXYOJYvYAcC9i8gc35OKMR3RB0zTgNAKUumN+QFaNrBGGMpAwTgGFceQFZDYoBlGAA+3Ert1q9x4UAAAAASUVORK5CYII=",
    "counts": {
      "items": 3,
      "recipes": 1,
      "features": 6
    },
    "dataHash": "5c8bd4f16cf9"
  },
  {
    "id": "simpleend",
    "displayName": "Simple End",
    "description": "End wood (Ashwood, Duskwood) with buds, trees and stick scrub, End grass, the Ash Valley and Dawnlands biomes on the outer End islands; bundles Simple End Dragon Fight.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [
      "simpleenddragonfight"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "overview": {
      "status": "fertig (Abnahme offen)",
      "open": "4",
      "missing": "Funktionen fertig; offen Client-Sicht im Spiel, Stil-Audit-Reste und die Nebelwand-Frage (Lohen-Seite).",
      "asOf": "19.10.2026"
    },
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAzUlEQVR42mOQk9D+TwlmABET2paQjH1sclANSA1v/L9u+d7/Jy8eBrOxYZgcSLO5th+mAcgKZy2dCueD2MiaQRbhNQCGc6uywRjGB2mGYaIN8HIKwNBM0IC4kKT/r59+AOPTh66AMUiMKAOQNYPwxq7TYAOWzt6AYghOA5A1w2wHaQbhupIO/AYg2w6zGWYASDMIw1yB0wCYJlya8RoAwshOhmkGuQZZM04DQBIgRciakW3GawAodcEkQQrRNeKNRpgAMkbXjC6PYgAlGADBDC0kWCrJGAAAAABJRU5ErkJggg==",
    "counts": {
      "items": 8,
      "blocks": 47,
      "recipes": 35,
      "features": 8
    },
    "dataHash": "8743da5d21e0"
  },
  {
    "id": "simplerespawn",
    "displayName": "Simple Respawn",
    "description": "Downed instead of dead: a teammate can revive you within the bleed-out time (then 3 hearts, no food); own downed screen with spectate, inventory view and give up.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAoElEQVR42mNwjor9TwlmABEn33wkGa948ALVgC1PXv+34uBAwSAxbOIgzZULl6MaAJL4UFDw/4qWFhzDNCCLwcRRDIBpRjcAHwbpwTAAXREMEGUANo3ogGoGdImIoBoAC0RivADTjBELsOjCZwBMM9ZoRI5zbIGGrBmvASBJdEPQNeM0AFkBzBBsmrEacOPjVwxFyEkXrwEwAVIx3ABKMADfR3xpZ2gp6gAAAABJRU5ErkJggg==",
    "counts": {
      "items": 1,
      "recipes": 1,
      "features": 6
    },
    "dataHash": "0060d25a18d6"
  },
  {
    "id": "simplebeacons",
    "displayName": "Simple Beacons",
    "description": "Strong Beacon (6 pyramid levels, 6 more effects, two-column screen), Strong Conduit (7x7x7 frame, up to 144 blocks under water, Dolphin's Grace) and the Strong Upgrade template (nether star). Axe on its own, sledgehammer with a hammer mod.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAwUlEQVR42mPQDQ77TwlmABHLHj0HYz5OMaIwSG3BhRuoBoAk4lxa/jvpJoFpEIZpwCYeOG0OpgEgRXpybnDcEiMDxshicsJ6uA0Amb7vzy8MA5ANJmgASBFMw7lFVmCMLIbXAGSbQOxvd2rAGFkMrwEwJ4NsBWn8/+saGIPYMDkQJtkAEJ8oA0BORPYGyBCQZrIDETkaiQpEmAEwDejpwO3EJeJjAd0FsPRB0AUgBYQwhgGgjAESIDYzgdSiGEAJBgB92uqn9eW6TwAAAABJRU5ErkJggg==",
    "counts": {
      "items": 3,
      "blocks": 2,
      "recipes": 6,
      "features": 7
    },
    "dataHash": "405f79eb15ce"
  },
  {
    "id": "simpleovens",
    "displayName": "Simple Ovens",
    "description": "Reinforced, Netherite and Enderite furnaces, smokers and blast furnaces (2x/4x/8x speed, double experience from Netherite) and furnace minecarts, moved from SimpleBuilding (ids migrate). In-world upgrades with the axe on its own, with the sledgehammer of Simple Hammers.",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAiElEQVR42mOItVL4TwlmABEfd00iGb+YEIBqgJaWFkl4eYYVpgFdXV3/8/LywDQMo/NBGK8BVlZWeDFBA/DZDBMjygB8huE1QE5ODi8m2YATXgLkGQDT+KFLAcUQogwAaYBp/H/DC8UQogwAaQBpRMYgMaIMwBZ9REcj2UkZlDFAAqRiuAGUYACdtBRwbfGWOAAAAABJRU5ErkJggg==",
    "counts": {
      "items": 4,
      "blocks": 9,
      "recipes": 9,
      "features": 7
    },
    "dataHash": "de4cccaba053"
  },
  {
    "id": "simplemobheads",
    "displayName": "Simple Mobheads",
    "description": "A head for every mob: dropped when a charged creeper's explosion kills it, worn for a small secret, placed, or played on a note block. Required sub-mod of SimpleBuilding; Simple Fun's and SimpleBuilding's heads moved in.",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib",
      "cloth_config"
    ],
    "optional": [
      "simplebuilding",
      "simplemobs",
      "simpleend",
      "simpleenddragonfight",
      "jei",
      "modmenu"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAgklEQVR42mMIcLP6TwlmABF3z+0iGZ/YMAHVADkrCZLwhPoMTAOiNnj9t+syAdPobGQ+XgPwaYSxCbpASEoAjGGaYHyiXYCuAd1AnAagOx1dI1FeQNeMbAhRXkB3NjZXEO0C5EAlygXoYYALk5QOsKUJnAaQnZRBGQMkQCqGG0AJBgA3BxAT2z0G5AAAAABJRU5ErkJggg==",
    "counts": {
      "items": 1,
      "blocks": 188,
      "recipes": 1,
      "features": 12
    },
    "dataHash": "1cf69f154a86"
  },
  {
    "id": "simpleshops",
    "displayName": "Simple Shops",
    "description": "Player shops: a Sales Block with a price (up to five exact price items) and a Display Case with up to five goods, one piece per trade. Protected from strangers, explosions, pistons and hoppers; no claim right needed to buy.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "simplemoney",
      "simpletweaks"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAhUlEQVR42mMwdov6TwlmABEbzr0hGTdseIBqgJWKAEk4pn4hwgCQwAkDAzD+8OEDGBPig/RQzwCKvQAyYNLuJ/+7kozANDoGBRgMg9SAaLwGGBxIAGOY5g8HDMCYZANgthJlAMVhABJcUWJDFMZrQFWQBk6NMDnauYDsMICFKqkYbgAlGABcnECwCVT7ggAAAABJRU5ErkJggg==",
    "counts": {
      "items": 1,
      "blocks": 5,
      "recipes": 6,
      "features": 10
    },
    "dataHash": "30dbd747d283"
  },
  {
    "id": "simplewanderers",
    "displayName": "Simple Wanderers",
    "description": "Village Wanderer: one Wandering Trader per village with a leashed horse, goods split from the traveling trader, a new stock every 3 days, a restock every 1.5 days and a range by village biome. Works on its own with Vanilla.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [
      "simplebuilding",
      "simplemoney",
      "simplemobheads"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAd0lEQVR42mOICrD6TwlmABFv7u4iGT84MQHVABEVN5LwwgkZmAboxawgCuM1QN6pAadGmBxBA07MysKKiTIApPDDhw//tbqs4BpBbJAYiE2UATC8osILjJHFhoABFAcizBBsmKh0QFFCIjspgzIGSIBUDDeAEgwAwIBBl9sRlXEAAAAASUVORK5CYII=",
    "counts": {
      "items": 2,
      "recipes": 1,
      "features": 6
    },
    "dataHash": "34ebe8a6188b"
  },
  {
    "id": "simplestats",
    "displayName": "Simple Stats",
    "description": "Every tool, weapon and piece of armor keeps its own statistics (blocks mined, kills by mob and variant, damage absorbed, distances, catches) with an item-specific statistics screen.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [
      "simplebuilding"
    ],
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAcElEQVR42mMICI76TwlmABF3H70hGZ+48ABhgJWV1f8L15/8f/bqI5gmhEGaJ0xbiGkAIeywIQCMtaos6GTAiwMGYLyiSwHs7CFgwA0HBzCepaFBuQEgDUPMAHIwSl4AJWOQLcRiFBfABEjFcAMowQBeNqVBEv8rtgAAAABJRU5ErkJggg==",
    "counts": {
      "items": 1,
      "recipes": 1,
      "features": 10
    },
    "dataHash": "c2498e0550c4"
  },
  {
    "id": "simpleblocks",
    "displayName": "Simple Blocks",
    "description": "Missing building blocks: Hushed Deepslate Tiles (dampen vibrations like wool), Sculk Deepslate, the missing Vanilla stairs, slabs and walls and cracked/mossy brick variants, and SimpleBuilding's building blocks without a home of their own (suspended and levitating sand/gravel, dirt/grass/sand/gravel slabs, cracked and chiseled ice, the nautilus shell block, three froglight colors; ids migrate). The Shapecutter cuts stone and wood into every stair, slab and shape. Works on its own with Vanilla; required sub-mod of SimpleBuilding.",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "jei"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA20lEQVR42qWSIQ7CQBBFe4gKTlCFrKMCgaoiJJVwgFpQ1RyAI+BQ3AKBRGCxhASFqG7zJ/nJzHRSRMVru/3737abTZbrbTeFBJfb6xeSpjMhys73jxVcH9/u+W7lzmLTHAWOkQGU96fLUMCJWTYXKOCYORYyAgZ1fTCw6N9zvhFU1W5Q9F/APM8XsQDoogcZyn8FZbkRWCyKlTAqQOgFgGUt4IYaASbzX8cEAM+hQEso0kWWQ4H+Cr0f+r/16kbgDxIFgGVuHAhPIk4XAqBFvkiMgC88LEaZEUyhB68MB6pVB/byAAAAAElFTkSuQmCC",
    "counts": {
      "items": 1,
      "blocks": 53,
      "recipes": 282,
      "features": 8
    },
    "dataHash": "6490eb6fe341"
  },
  {
    "id": "simplecrucibles",
    "displayName": "Simple Crucibles",
    "description": "Iron, Reinforced, Netherite and Enderite crucibles and barrels (a cooking station without fuel), the reinforced cauldron and village field kitchens, moved from SimpleLib and SimpleBuilding (ids migrate). In-world upgrades with the axe on its own, with the sledgehammer of Simple Hammers.",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "jei",
      "jade"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA9klEQVR42qWTIQvCQBiG91NMYlCjUZdsBrEuiXZBhIHCisFwzWRyIIhFm1YNFsU0TFb9HyfvwTvu2B0XFp7tbt/3Pncbt6DRCmUZAlz257tBs1qxovdMVqkpWG6OeeNXdOQ2ieVFzBWvbk3BOsL94dQUoIAQGn9ZpIAIcIwahOi1CtCgB3SRLnEKUjGTSbtuiPQdYSzikVuQ3Xa5RIfCqBf6BTqQAYQggQBzr+DzPBUECHsF8XggD+tFDuYAQe4Ac6cADb4dOL8BDxKCWB13rAYBw8B5kChAiBKGuQOubhVcH29V4HngK9jCBQEfENfPpPcYgjL8AfeqMA6tHhHIAAAAAElFTkSuQmCC",
    "counts": {
      "items": 27,
      "blocks": 11,
      "recipes": 6,
      "features": 7
    },
    "dataHash": "80c06e88b8ed"
  },
  {
    "id": "simpleoctets",
    "displayName": "Simple Octets",
    "description": "Octets: half-block cubes (0.5 x 0.5 x 0.5) of every wood, every block with stairs and slabs and the melon, which mix in one cell, moved from SimpleBuilding (ids migrate). Cut in the stonecutter, crafted back into the block; the sledgehammer of Simple Hammers carves them. Octets of SimpleBuilding's and Simple End's blocks only with that mod.",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "simpleend",
      "jei"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAArUlEQVR42mOwCrD6TwlmABG77u7Ciq00BMAYm9yEExNQDVhwYcH/s8/OgmmYxi1dAf9PLEiB80FyIAzSnDEhA9MAmGaQJhgGGYJsEEgzyCK8BswqcgBjmCFVYQZgDDOAoAtgBoAwTDNRBsD8is8AmCEoBuAKNJgBuOThBuALNLyBCjMAX6DhDVR0A3D5Gac81QzAFWiwKMMljxKNoNSFrhEd44xGmACpGOEFCjAA7+40ZjR1wYIAAAAASUVORK5CYII=",
    "counts": {
      "items": 1,
      "blocks": 176,
      "recipes": 521,
      "features": 6
    },
    "dataHash": "fa3e13af5aa2"
  },
  {
    "id": "simplehammers",
    "displayName": "Simple Hammers",
    "description": "Seven sledgehammers (Stone to Enderite) that mine a 3x3 area, reshape blocks into stairs and slabs, carve octets, crush diamond and quartz blocks and upgrade blocks in the world strike by strike, with the enchantments Break Through, Override and Radius, moved from SimpleBuilding (ids migrate). While it is loaded the in-world ways of the other Simple mods take the hammer instead of the axe.",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "simpleoctets",
      "simplecrucibles",
      "jei"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA40lEQVR42mMItlL7jw9HRUXhlWcAEY92zcGKQZpBQFNTE0PuwoQEVAOuLyj7/+rsdjANwjDNb968gRsCkwNpnpbhimkANs0wDDLAw1T0f6SHBtginAaga7537x4Yg9jVSab/NeS4iXMBsmYQXr9+/f/6+nqwSwgaADMkOjoarBGbIQQNAGGQn5ENIckAkGaQn9WBfgZpxGYITgOQNYP8C1KMbAheA0Ca29PU4ZphGNkQEMZqAC7NyIaAbMdqAEhzuq8cTs3ohsANAGkkZDM2Q+CxgKwZJEgKhnsBpBmECWVtbBgAwfhqwWiFR/AAAAAASUVORK5CYII=",
    "counts": {
      "items": 8,
      "recipes": 11,
      "features": 9
    },
    "dataHash": "30b7beb2f3cc"
  },
  {
    "id": "simpleoptimization",
    "displayName": "Simple Optimization",
    "description": "Super-mod without content of its own: bundles Simple Opt Render, Simple Opt Memory and Simple Opt Server with one on/off switch each.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric"
    ],
    "requires": [
      "simpleoptrender",
      "simpleoptmemory",
      "simpleoptserver"
    ],
    "optional": [],
    "overview": {
      "status": "in Arbeit",
      "open": "13",
      "missing": "Stufe 1 + Teile von Stufe 2 nur auf claude-wave1 und nur Fabric; offen Kompat-Ziel, 2. Messreihe, Benchmark beim Besitzer (Stufe 3), Stufen 4/5.",
      "asOf": "19.10.2026"
    },
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAj0lEQVR42mOQU3H+TwlmABH9M0/ixVleHCgYJOYVtQLVgOTCLf/Xbb0BppExSMP/Bw5g/GCBApgP0mzqWIlpALGaQRYRNACXZhjGawA2zegYpwHImrFhmIF4DUDHZAciSCHMAPRwIGgAPs0EDSCkGa8BxGjGaQCyZlwacRoASl0wAwhpxjAAJkAqhhtACQYAbsktVjUvMIsAAAAASUVORK5CYII=",
    "counts": {
      "features": 1
    },
    "dataHash": "c6d6d341c731"
  },
  {
    "id": "simpleoptrender",
    "displayName": "Simple Opt Render",
    "description": "Client optimizations (leaf culling, light-map cache, fast IP ping, telemetry off); each area steps back when a known optimization mod covers it.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric"
    ],
    "requires": [],
    "optional": [],
    "parent": "simpleoptimization",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAhElEQVR42mMw1PL9TwlmABFrF98gGVdlHUA1oLf+5P/Dex6DaUIYpDnCpwvTAGI1gyzCa4CWlhVWDNIMw1gNgCn88OEDVoxsEIYB6Bqtum6gYHSDUAzApRkEBCQUcBpC0ACQZqIMoNgLVAlE5HgmOxqRFRDCGAaAUhfZBsAESMVwAyjBAB7LeXTJODZuAAAAAElFTkSuQmCC",
    "counts": {
      "features": 7
    },
    "dataHash": "b2d1f93d90d8"
  },
  {
    "id": "simpleoptmemory",
    "displayName": "Simple Opt Memory",
    "description": "Memory optimizations (block states, models); stage 1 only holds the switch, config and mod detection.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric"
    ],
    "requires": [],
    "optional": [],
    "parent": "simpleoptimization",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAe0lEQVR42mPwDYj9TwlmABE37n4kGR848QLVgJMXXv9//OwrmCaEQZq7JizHNIBYzSCLUAzgsxIhC6MYoNVlRRLGaoCUlBQYwxTh49PGAIq88OKAw3+5LC2iaQwDKIoF5HQAimdiMNaERKxmrAaAUhfZBsAESMVwAyjBAPycZS9pdCIrAAAAAElFTkSuQmCC",
    "counts": {
      "features": 2
    },
    "dataHash": "6f05fd404d00"
  },
  {
    "id": "simpleoptserver",
    "displayName": "Simple Opt Server",
    "description": "Server optimizations: view distance follows server load (MSPT) within server.properties bounds; steps back for ServerCore.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric"
    ],
    "requires": [],
    "optional": [],
    "parent": "simpleoptimization",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAZ0lEQVR42mMQEtf6TwlmABG1rYtJxnpWWagGeIfV/5eSUSAKgzQravlgN2DWghV4MUjNwmV78BugpWeCgkFiDg8S4AZQ7AKCBgxOF3w4YDBiwiAqIQMvxmkAKHWRnZRhAqRiuAGUYABjgOz4Wx7bWwAAAABJRU5ErkJggg==",
    "counts": {
      "features": 2
    },
    "dataHash": "c718c2a98ad5"
  },
  {
    "id": "simplestorage",
    "displayName": "Simple Storage",
    "description": "Crates (open wooden food boxes for every wood, hoppers and comparators work), the Storage Crafting Table (a crafting table that keeps its grid), tiered and trapped chests, trapped copper chests and tier shulker boxes (more slots, bigger stacks, upgraded in the world keeping their contents), moved from SimpleBuilding (ids migrate). Chest carts and chest boats, bundles, quivers, the placed bundle and the storage enchantments Drawer, Deep Pockets and Funnel (8c-1) also moved, then the four backpacks (8c-2).",
    "version": "0.9.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "simpleend",
      "trinkets_updated",
      "curios"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAoUlEQVR42mOw8nL4TwlmABG7Lh8hGU/YsQDVgAWH1v4/e/8KmIZhV3EuMEYWA2GQ5oyWYkwD0DVjY4M0gyzCMABmG7EYxQCQwPVsZ5IwSA+GAcdiTInCO8KMKDNgnqcWpgEgQZDJyPjBgwdgjC6O1YBsNcH/ndaqKLijowOM0cVBajEMADkNJIFuGzoGqcHqgmFgAHpAEcIoBsAMIQXD9AEARSUc8fQdSmQAAAAASUVORK5CYII=",
    "counts": {
      "items": 17,
      "blocks": 37,
      "recipes": 177,
      "features": 7
    },
    "dataHash": "ecaaba130eda"
  },
  {
    "id": "simplemusic",
    "displayName": "Simple Music",
    "description": "Sheet music with a text note code (editor with test listening, .txt/.nbs import), the Orchestrion (note block + jukebox: redstone loop, chest above = next sheet, comparator progress) and uploaded mono Ogg discs (quota, OP approval). Needs SimpleLib; SimpleBuilding optional.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "simplemobheads"
    ],
    "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAApUlEQVR42mOIclb+TwlmABFvTk4nGT9YEYFqwJMt+f8/3tgCpglhkOaFlfaYBiBjISERFIysGWQRVgNgivX0jLBimDxWF8A0JiSkgTG6ZgUFFRSDUAxA1wwzYEHBGTDG5RoMA5BtKfGZ95+Hhw+MQWx0l9DGAHSM7gXksEExADkQ0V1CVCBSJRphiQSG0RMSshxWA0CpC10RPoxiAEyAVAw3gBIMAP+6D2a7uZI2AAAAAElFTkSuQmCC",
    "counts": {
      "items": 21,
      "blocks": 2,
      "recipes": 38,
      "features": 16
    },
    "dataHash": "787e9d90d89c"
  }
];
