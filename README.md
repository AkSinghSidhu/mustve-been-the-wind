# Must've Been the Wind

![Minecraft 1.21.11](https://img.shields.io/badge/Minecraft-1.21.11-3C8527)
![Fabric 0.19.5+](https://img.shields.io/badge/Fabric-0.19.5%2B-1976D2)
![License CC0-1.0](https://img.shields.io/badge/License-CC0--1.0-lightgrey)

**A living wind system for Minecraft Fabric.** Campfire smoke bends downwind, rain and snow slant,
clouds drift across the sky, and sudden gusts roll through even the calmest day.

> Named after the oldest excuse in gaming.

![A gust front bending a campfire plume](docs/screenshots/gust-front-plume.png)

## Features

### Weather-aware wind
Wind is not a single global effect. It changes with the weather and the time of day:

| Conditions | Wind strength |
|---|---|
| Clear day | 2–8% |
| Clear dawn/dusk | 1.5–6.5% |
| Clear night | 1–5% |
| Rain, day | 25–50% |
| Rain, night | 28–55% |
| Thunderstorm, day | 80–100% |
| Thunderstorm, night | 85–100% |

The bands blend smoothly as rain and thunder build or fade. Direction wanders on its own, and strong
wind swings farther than a light breeze.

### Gusts, lulls, squalls and gust fronts
The background wind is punctuated by short events. They arrive on their own schedule, weighted by the
weather, and have a smooth attack, hold and release:

| Event | Duration | Effect |
|---|---:|---|
| Breeze | 8–14 s | A gentle rise |
| Gust | 3–5 s | A quick push above the background wind |
| Lull | 3–6 s | A sudden drop, even during a storm |
| Squall | 5–9 s | A strong push with a direction veer |
| Gust front | 8–14 s | The strongest push, with the biggest veer |

Rain begins with a squall. A thunderstorm arrives with a gust front. A slow multi-minute cycle also
makes long storms rise, peak and fall instead of sitting at one speed.

### Smoke, clouds and precipitation
- Campfire smoke inherits the wind, so plumes bend and trail downwind. Signal fires work too.
- Clouds drift with the wind instead of using vanilla's fixed eastward motion.
- Rain and snow columns lean downwind, with the slant growing as the wind strengthens.
- Hostile mobs downwind of a lit campfire can investigate the smoke (experimental).

### Wind Vane
The Wind Vane is a small utility item. Right-click while holding it to read the current direction and
strength in the action bar.

Craft it with one stick and four iron ingots:

```text
  I
ISI
  I
```

`I` = iron ingot, `S` = stick. It also appears in the Tools & Utilities creative tab.

## Commands

| Command | Description |
|---|---|
| `/wind` | Show the current wind, weather/time band and active event |
| `/wind set <degrees> <strength>` | Pin the wind. `0` = north, `90` = east, `180` = south, `270` = west |
| `/wind auto` | Release a pinned wind back to natural weather |
| `/wind event <breeze\|gust\|lull\|squall\|front>` | Trigger a wind event immediately |

`/wind event` also releases a pinned wind so the event can take effect.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.5 or newer |
| Fabric API | 0.141.6+1.21.11 or newer |
| Java | 21 or newer |

Install the mod on both the client and server. The server owns the wind state; the client uses it for
the visual effects.

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11.
3. Put both `fabric-api-*.jar` and `mustve-been-the-wind-*.jar` into your `mods` folder.
4. Launch the Fabric profile.

## Screenshots

| Calm day | Gust front |
|---|---|
| ![Calm campfire plume](docs/screenshots/calm-plume.png) | ![Campfire plume bent by a gust front](docs/screenshots/gust-front-plume.png) |

| Rain | Thunderstorm |
|---|---|
| ![Slanted rain](docs/screenshots/rain.png) | ![Thunderstorm wind](docs/screenshots/thunderstorm.png) |

## Building from source

You need JDK 21 or newer. The Gradle wrapper handles the rest.

```bash
git clone https://github.com/AkSinghSidhu/mustve-been-the-wind.git
cd mustve-been-the-wind
./gradlew build
```

The mod jar is written to `build/libs/mustve-been-the-wind-0.1.0.jar`.

For a normal development client:

```bash
./gradlew runClient
```

## How it works

- `WindState` keeps the live wind on the server: a direction, a strength, weather/time bands, a slow
  background cycle and transient events.
- The wind is synced to clients four times per second with a small custom payload, so short gusts are
  visible in particles, clouds and weather.
- Client mixins redirect the campfire smoke spawn, offset the cloud renderer, and shear precipitation
  vertices for the wind lean.
- The technical mod ID is `windsmoke`; the item is `windsmoke:wind_vane`.

## License

This project is released under [CC0-1.0](LICENSE). Do whatever you like with it.

Built with [Fabric](https://fabricmc.net/).
