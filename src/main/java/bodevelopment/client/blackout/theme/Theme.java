package bodevelopment.client.blackout.theme;

import bodevelopment.client.blackout.module.modules.client.ThemeSettings;
import bodevelopment.client.blackout.util.ColorUtils;

import java.awt.*;

public enum Theme {
    BLACKOUT("BlackOut", new Color(155, 0, 0), new Color(200, 20, 20)),
    BLAZE_ORANGE("Blaze Orange", new Color(255, 170, 80), new Color(255, 130, 0)),
    ROSY_PINK("Rosy Pink", new Color(255, 100, 200), new Color(190, 80, 155)),
    SUNSET("Sunset", new Color(255, 95, 80), new Color(255, 165, 70)),
    OCEAN_BREEZE("Ocean Breeze", new Color(70, 120, 160), new Color(30, 55, 90)),
    FOREST_GLADE("Forest Glade", new Color(70, 190, 85), new Color(40, 80, 55)),
    MOUNTAIN_DUSK("Mountain Dusk", new Color(85, 100, 115), new Color(45, 60, 80)),
    GOLDEN_HOUR("Golden Hour", new Color(250, 185, 0), new Color(255, 220, 50)),
    CORAL_REEF("Coral Reef", new Color(255, 130, 80), new Color(255, 160, 120)),
    MIDNIGHT_SKY("Midnight Sky", new Color(25, 25, 112), new Color(0, 0, 140)),
    SPRING_BLOSSOM("Spring Blossom", new Color(255, 180, 190), new Color(255, 105, 180)),
    DEEP_SEA("Deep Sea", new Color(0, 105, 150), new Color(0, 80, 150)),
    LAVENDER("Lavender", new Color(150, 123, 182), new Color(123, 104, 238)),
    DAYLIGHT("Daylight", new Color(255, 223, 186), new Color(135, 206, 250)),
    WINTER_CHILL("Winter Chill", new Color(240, 248, 255), new Color(143, 210, 229)),
    SUMMER_BREEZE("Summer Breeze", new Color(255, 218, 185), new Color(255, 160, 122)),
    CRIMSON_TIDE("Crimson Tide", new Color(220, 20, 60), new Color(178, 34, 34)),
    DEEP_BLUE("Deep Blue", new Color(0, 0, 255), new Color(0, 0, 119)),
    MONOCHROME("Monochrome", new Color(225, 225, 225), new Color(80, 80, 80)),
    COTTON_CANDY("Cotton Candy", new Color(255, 182, 193), new Color(255, 105, 180)),
    VIOLET_DREAM("Violet Dream", new Color(138, 43, 226), new Color(148, 0, 211)),
    NUCLEAR_GLOW("Nuclear Glow", new Color(73, 224, 0), new Color(229, 213, 0)),
    OCEAN_WAVE("Ocean Wave", new Color(0, 119, 190), new Color(0, 72, 144)),
    FOREST_CANOPY("Forest Canopy", new Color(32, 182, 32), new Color(0, 89, 0)),
    FROSTY_MINT("Frosty Mint", new Color(152, 251, 152), new Color(46, 139, 87)),
    LAVENDER_FIELDS("Lavender Fields", new Color(230, 230, 250), new Color(123, 104, 238)),
    MIDNIGHT_PURPLE("Midnight Purple", new Color(75, 0, 130), new Color(138, 43, 226)),
    TANGERINE_DREAM("Tangerine Dream", new Color(255, 140, 0), new Color(255, 69, 0)),
    BLUE_SKY("Blue Sky", new Color(135, 206, 235), new Color(70, 130, 180)),
    CHARCOAL("Charcoal", new Color(105, 105, 105), new Color(47, 79, 79)),
    ELECTRIC_LIME("Electric Lime", new Color(204, 255, 0), new Color(127, 255, 0)),
    AMBER_BLOOM("Amber Bloom", new Color(220, 140, 40), new Color(225, 95, 200)),
    SCARLET_WOODLAND("Scarlet Woodland", new Color(155, 11, 11), new Color(220, 20, 60)),
    AZURE_REEF("Azure Reef", new Color(0, 120, 200), new Color(0, 212, 170)),
    CANDY_CANE("Candy Cane", new Color(220, 20, 20), new Color(225, 225, 225)),
    ALIEN_GREEN("Alien Green", new Color(0, 225, 0), new Color(35, 140, 35));

    private final String name;
    private final int mainColor;
    private final int secondaryColor;

    Theme(String name, Color color, Color secondaryColor) {
        this.name = name;
        this.mainColor = color.getRGB();
        this.secondaryColor = secondaryColor.getRGB();
        ThemeSettings.themes.add(this);
    }

    public int getMain() {
        return this.mainColor;
    }

    public int getSecondary() {
        return this.secondaryColor;
    }

    public int mainWithAlpha(int alpha) {
        return ColorUtils.withAlpha(this.mainColor, alpha);
    }

    public int secondaryWithAlpha(int alpha) {
        return ColorUtils.withAlpha(this.secondaryColor, alpha);
    }

    public String getName() {
        return this.name;
    }
}
