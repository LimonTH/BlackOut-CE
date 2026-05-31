# BlackOut Addon Development Guide

How to create an addon for BlackOut Client (1.21.4+, Fabric, Mojang Mappings).

---

## Table of Contents

1. [Project Setup](#1-project-setup)
2. [Entry Point](#2-entry-point)
3. [Adding Modules](#3-adding-modules)
4. [Adding Commands](#4-adding-commands)
5. [Adding HUD Elements](#5-adding-hud-elements)
6. [Adding Custom Themes](#6-adding-custom-themes)
7. [Adding Custom Menu Music](#7-adding-custom-menu-music)
8. [Adding Custom Main Menu Backgrounds](#8-adding-custom-main-menu-backgrounds)
9. [Playing Sounds](#9-playing-sounds)
10. [Event System](#10-event-system)
11. [Notifications](#11-notifications)
12. [Lifecycle Hooks](#12-lifecycle-hooks)
13. [ClickGUI Screens](#13-clickgui-screens)
14. [Building & Testing](#14-building--testing)

---

## 1. Project Setup

> **Quick start:** Clone the [addon template repository](https://github.com/LimonTH/Blackout-CE-addon-template) for a ready-to-use project skeleton with all required files pre-configured.

### `build.gradle`

```groovy
plugins {
    id 'fabric-loom' version '1.6-SNAPSHOT'
    id 'maven-publish'
}

group = 'com.example'
version = '1.0.0'

repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    minecraft "com.mojang:minecraft:1.21.4"
    mappings loom.officialMojangMappings()

    // BlackOut Client — provided by the runtime environment
    modCompileOnly "com.github.LimonTH:Blackout-CE:${project.blackout_version}"
}

loom {
    accessWidenerPath = file("src/main/resources/myaddon.accesswidener")  // optional
}
```

### `gradle.properties`

```properties
blackout_version=1.21.4-SNAPSHOT
```

### `fabric.mod.json` — `src/main/resources/fabric.mod.json`

```json
{
    "schemaVersion": 1,
    "id": "myaddon",
    "version": "1.0.0",
    "name": "My Addon",
    "description": "An example BlackOut addon.",
    "authors": ["YourName"],
    "environment": "client",
    "entrypoints": {
        "bodevelopment/client/blackout": [
            "com.example.myaddon.MyAddon"
        ]
    },
    "depends": {
        "fabricloader": ">=0.16.0",
        "minecraft": "~1.21.4"
    },
    "icon": "assets/myaddon/icon.png"
}
```

> **Critical:** The entry point key MUST be `"bodevelopment/client/blackout"`. This is what `AddonLoader` scans.

---

## 2. Entry Point

Your addon class must extend `BlackoutAddon` and implement `onInitialize()`.

### Minimal Example

```java
package com.example.myaddon;

import bodevelopment.client.blackout.addon.BlackoutAddon;

public class MyAddon extends BlackoutAddon {
    public MyAddon() {
        super("MyAddon",                          // display name
              "com.example.myaddon.modules",      // modulePath
              "com.example.myaddon.commands",     // commandPath
              "com.example.myaddon.hud"           // hudPath
        );
    }

    @Override
    public void onInitialize() {
        // Registration calls go here (themes, music, menu renderers, etc.)
    }
}
```

### Full Constructor (all extension points)

```java
protected BlackoutAddon(
    String name,         // Display name
    String modulePath,   // Package scanned for Module subclasses (or null)
    String commandPath,  // Package scanned for Command subclasses (or null)
    String hudPath,      // Package scanned for HudElement subclasses (or null)
    String themePath,    // Ignored — themes are registered manually via registerTheme() (or null)
    String soundPath,    // Resource prefix for .ogg sounds, e.g. "assets/myaddon/sounds"
    String guiPath       // Package scanned for ClickGuiScreen & MainMenuRenderer (or null)
)
```

The old 4-argument constructor delegates to the full one with `null` for the new paths:
```java
protected BlackoutAddon(String name, String modulePath, String commandPath, String hudPath)
```

---

## 3. Adding Modules

Place module classes in the package specified by `modulePath`. They will be auto-discovered.

```java
package com.example.myaddon.modules;

import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;

public class ExampleModule extends Module {
    public ExampleModule() {
        super("Example",                           // internal name
              "An example addon module.",          // description
              SubCategory.MISC,                    // GUI category
              true);                                // subscribe to EventBus
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (!this.enabled) return;
        // Module logic here
    }
}
```

**Available categories:**

| Parent | SubCategory constant | GUI tab |
|--------|---------------------|---------|
| Combat | `DEFENSIVE`, `OFFENSIVE`, `MISC_COMBAT` | Combat |
| Movement | `MOVEMENT` | Movement |
| Visual | `ENTITIES`, `WORLD`, `MISC_VISUAL` | Visual |
| Misc | `MISC`, `MEMES` | Misc |
| Legit | `LEGIT` | Legit |
| Client | `CLIENT`, `SETTINGS` | Client |

### Using `@Event` in Modules

If `subscribe = true` in the constructor, the module is auto-subscribed to `BlackOut.EVENT_BUS`. Common events:

- `TickEvent.Pre` / `TickEvent.Post` — every client tick
- `RenderEvent.World` — 3D world rendering
- `RenderEvent.Hud.Pre` / `RenderEvent.Hud.Post` — HUD rendering
- `PacketEvent.Receive.Pre` — incoming packets
- `PacketEvent.Send.Pre` — outgoing packets
- `MoveEvent.Pre` / `MoveEvent.Post` — player movement
- `KeyEvent` — keyboard input
- `MouseButtonEvent` — mouse clicks

### Settings

```java
private final SettingGroup sgGeneral = this.addGroup("General");
public final Setting<Boolean> myToggle = this.sgGeneral.boolSetting(
    "My Toggle", true, "Description");
public final Setting<Double> mySlider = this.sgGeneral.doubleSetting(
    "My Slider", 1.0, 0.0, 10.0, 0.1, "Description");
public final Setting<MyEnum> myEnum = this.sgGeneral.enumSetting(
    "My Enum", MyEnum.Value1, "Description");
```

---

## 4. Adding Commands

Place command classes in the package specified by `commandPath`.

```java
package com.example.myaddon.commands;

import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.util.ChatUtils;

public class ExampleCommand extends Command {
    public ExampleCommand() {
        super("example", ".example <arg>");
    }

    @Override
    public String execute(String[] args) {
        if (args.length == 0) return "Usage: " + this.format;
        ChatUtils.info("You said: " + args[0]);
        return null;  // null = no output, non-null = displayed as chat message
    }
}
```

---

## 5. Adding HUD Elements

Place HUD element classes in the package specified by `hudPath`.

```java
package com.example.myaddon.hud;

import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.hud.HudElement;

public class ExampleHud extends HudElement {
    public ExampleHud() {
        super("Example HUD", "Shows example data on screen.");
    }

    @Event
    public void onRender(RenderEvent.Hud.Post event) {
        if (!this.enabled) return;
        // Render your HUD content using this.stack, BlackOut.FONT, etc.
    }
}
```

---

## 6. Adding Custom Themes

Register themes in `onInitialize()`. They appear in **Client → Theme → Theme** selector.

```java
package com.example.myaddon.themes;

import bodevelopment.client.blackout.theme.Theme;
import java.awt.Color;

public enum MyThemes implements Theme {
    CYBER_PUNK("Cyber Punk", new Color(255, 0, 128), new Color(0, 255, 255)),
    NATURE("Nature", new Color(34, 139, 34), new Color(144, 238, 144));

    private final String name;
    private final int mainColor;
    private final int secondaryColor;

    MyThemes(String name, Color main, Color secondary) {
        this.name = name;
        this.mainColor = main.getRGB();
        this.secondaryColor = secondary.getRGB();
    }

    @Override public int getMain() { return mainColor; }
    @Override public int getSecondary() { return secondaryColor; }
    @Override public String getName() { return name; }
    @Override public int mainWithAlpha(int alpha) { /* delegated to ColorUtils */ return mainColor; }
    @Override public int secondaryWithAlpha(int alpha) { /* delegated to ColorUtils */ return secondaryColor; }
}
```

Then in `MyAddon.onInitialize()`:

```java
@Override
public void onInitialize() {
    registerTheme(MyThemes.CYBER_PUNK);
    registerTheme(MyThemes.NATURE);
}
```

---

## 7. Adding Custom Menu Music

Register OGG Vorbis tracks that play in the main menu with full fade-in/out support.

### Step 1: Place `.ogg` files in resources

```
src/main/resources/assets/myaddon/sounds/my_track.ogg
```

### Step 2: Register in `onInitialize()`

```java
@Override
public void onInitialize() {
    registerMusicTrack("My Chill Beat", () ->
        getClass().getClassLoader().getResourceAsStream("assets/myaddon/sounds/my_track.ogg")
    );
}
```

### Step 3: User selects it

1. Open ClickGUI → **Client → Menu Music**
2. Set **Track** to **Custom**
3. Set **Custom Track Name** to `"My Chill Beat"`

The track will play with the same volume, fade duration, track duration, and loop delay settings as built-in tracks.

> **Note:** Each call to `openStream()` must return a **fresh** `InputStream`. The stream is consumed and closed by the audio engine.

---

## 8. Adding Custom Main Menu Backgrounds

Implement `MainMenuRenderer` and register it.

```java
package com.example.myaddon.gui;

import bodevelopment.client.blackout.randomstuff.mainmenu.MainMenuRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

public class MyMenuBackground implements MainMenuRenderer {
    @Override
    public void render(PoseStack stack, float height, float mx, float my,
                       String text1, String text2, float progress) {
        // Draw buttons, logo, splashes, etc.
    }

    @Override
    public void renderBackground(PoseStack stack, float width, float height,
                                  float mx, float my) {
        // Draw the full-screen background (shaders, images, etc.)
    }

    @Override
    public int onClick(float mx, float my) {
        // Return button index or -1
        return -1;
    }
}
```

Register in `onInitialize()`:

```java
@Override
public void onInitialize() {
    registerMainMenuRenderer("MyBackground", new MyMenuBackground());
}
```

User selects: **Client → Main Menu → Mode → Custom**, then sets **Custom Renderer** to `"MyBackground"`.

---

## 9. Playing Sounds

Play arbitrary `.ogg` sounds from your addon's resources at any time.

```java
// In any addon method:
playSound("explosion", 1.0f, 1.0f);  // name, pitch, volume
```

This resolves to `{soundPath}/{name}.ogg` using your addon's ClassLoader. The `soundPath` is set in the constructor (e.g., `"assets/myaddon/sounds"`).

For more control, use `SoundUtils` directly:

```java
InputStream stream = getClass().getClassLoader()
    .getResourceAsStream("assets/myaddon/sounds/custom.ogg");
if (stream != null) {
    SoundUtils.playStream(1.0f, 0.5f, stream);        // pitch, volume, stream
    SoundUtils.playStream(1.0f, 0.5f, stream, true);  // with looping
}
```

---

## 10. Event System

Your addon instance is automatically subscribed to `BlackOut.EVENT_BUS` after `onInitialize()`. Use `@Event` annotations on any method with a single event parameter.

```java
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.event.events.GameJoinEvent;

public class MyAddon extends BlackoutAddon {
    // ...

    @Event
    public void onTick(TickEvent.Pre event) {
        // Called every client tick
    }

    @Event
    public void onWorldJoin(GameJoinEvent event) {
        playSound("welcome", 1.0f, 1.0f);
    }
}
```

**Priority:** Use `@Event(eventPriority = 100)` for earlier/later execution (default = 0, lower = earlier).

---

## 11. Notifications

Send HUD notifications via the built-in convenience method:

```java
import bodevelopment.client.blackout.module.modules.client.NotificationsSettings;

sendNotification(
    "Short text",           // shown in the notification bar
    "Detailed message",     // shown when hovered
    3.0,                    // duration in seconds
    NotificationsSettings.Type.Info  // Info, Warning, Error
);
```

Or use the manager directly:

```java
Managers.NOTIFICATIONS.addNotification("text", "bigText", 3.0, NotificationsSettings.Type.Info);
```

---

## 12. Lifecycle Hooks

Override these methods in your addon class:

```java
@Override public void onEnable() { }       // After all components registered
@Override public void onDisable() { }      // Client shutdown / addon unload
@Override public void onWorldJoin() { }    // Player joins a world/server
@Override public void onWorldLeave() { }   // Player leaves a world/server
```

---

## 13. ClickGUI Screens

Implement `ClickGuiScreen` and place it in the package specified by `guiPath`. Screens are auto-discovered and stored in `addon.guiScreens`.

```java
package com.example.myaddon.gui;

import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;

public class MyScreen extends ClickGuiScreen {
    public MyScreen() {
        super("My Screen", 400, 300, true);
    }

    @Override
    public void render() {
        // Draw your custom GUI content
    }
}
```

To open your screen, use:

```java
Managers.CLICK_GUI.openScreen(new MyScreen());
```

---

## 14. Building & Testing

```bash
# Build the addon JAR
./gradlew build

# The output is at build/libs/myaddon-1.0.0.jar
```

Place the JAR in `minecraft/mods/` alongside BlackOut Client.

### Debugging

- Check the Minecraft log for `[BlackOut]` messages. The loader logs every addon it finds.
- Use `BOLogger.info(...)` / `BOLogger.error(...)` for your own logging.
- If your addon doesn't appear, verify the entry point key in `fabric.mod.json` is exactly `"bodevelopment/client/blackout"`.

### Version Compatibility

BlackOut uses a **two-tier** compatibility system:

#### Tier 1 — API Version (Primary Gate)

The **API version** is the semantic contract between your addon and BlackOut.
It is an integer that increments on breaking changes to the `@PublicAPI` surface.

Your addon automatically reports the API version it was compiled against:

```java
// Inherited from BlackoutAddon — no override needed
public int getApiVersion() { return BlackOut.API_VERSION; }
```

**How the loader interprets it:**

| Condition | Result |
|-----------|--------|
| `addon.api > client.api` | **HARD REJECT** — addon needs a newer BlackOut |
| `addon.api < client.api` | **SOFT WARN** — deprecated APIs may have been removed; addon may still work |
| `addon.api == client.api` | **OK** — exact match, guaranteed compatible |

> **For addon devs:** you don't need to do anything. The API version is embedded at compile time via `BlackOutInfo.API_VERSION` (sourced from `gradle.properties → api_version`).

#### Tier 2 — Client Version (Optional Secondary Gate)

Override `getMinClientVersion()` **only** when your addon depends on a specific
client behaviour that is not reflected in the API version (e.g., a rendering fix,
a non-API internal change, a specific module's behaviour):

```java
@Override
public String getMinClientVersion() {
    return "2.2";  // Minimum BlackOut mod version required (dot-separated numeric)
}
```

Format: dot-separated numeric (`"2.2"`, `"2.3.1"`). Comparison is numeric per segment
(`"2.10" > "2.2"`).

**Wildcards:** use `*` to match any value at a position:

| Min version | Meaning | Matches |
|-------------|---------|---------|
| `"2.*"` | Any 2.x | `2.0`, `2.2`, `2.99` — but NOT `3.0` |
| `"2.2.*"` | Any 2.2.x patch | `2.2.0`, `2.2.5` — but NOT `2.3.0` |
| `"*"` | Any version at all | Everything (same as `null`) |

> **Rule of thumb:** if your addon only uses `@PublicAPI` classes and methods,
> you should **not** override `getMinClientVersion()`. Leave it at the default (`null`).
> The API version check is sufficient.

#### Tier 3 — Minecraft Version

Handled by Fabric Loader via your addon's `fabric.mod.json`:

```json
"depends": {
    "minecraft": "~1.21.4"
}
```

---

## Complete Example

```java
package com.example.myaddon;

import bodevelopment.client.blackout.addon.BlackoutAddon;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.modules.client.MenuMusicSettings;

public class MyAddon extends BlackoutAddon {
    public MyAddon() {
        super("MyAddon",
              "com.example.myaddon.modules",
              "com.example.myaddon.commands",
              "com.example.myaddon.hud",
              "com.example.myaddon.themes",
              "assets/myaddon/sounds",
              "com.example.myaddon.gui"
        );
    }

    @Override
    public void onInitialize() {
        // Custom menu music
        registerMusicTrack("Lofi Hip Hop", () ->
            getClass().getClassLoader()
                .getResourceAsStream("assets/myaddon/sounds/lofi.ogg")
        );

        // Custom main menu background
        registerMainMenuRenderer("MyRenderer", new MyMenuBackground());
    }

    @Override
    public void onWorldJoin() {
        sendNotification("MyAddon", "Welcome to the server!", 3.0,
            NotificationsSettings.Type.Info);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        // Global tick logic (optional — most logic lives in modules)
    }
}
```
