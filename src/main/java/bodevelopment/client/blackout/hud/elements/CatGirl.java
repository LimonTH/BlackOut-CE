package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;

public class CatGirl extends HudElement {
    public final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<CatType> catType = this.sgGeneral.enumSetting("Cat Type", CatType.First, "Which cat do you like more?");
    private final Setting<Side> side = this.sgGeneral.enumSetting("Cat Side", Side.Left, "The side to which the image will be directed.");

    public CatGirl() {
        super("Cat Girl", "Renders a static decorative texture overlay on the HUD interface.");
        TextureRenderer t = BOTextures.getCat2Renderer();
        this.setSize(t.getWidth() / 4.0F, t.getHeight() / 4.0F);
    }

    @Override
    public void render() {
        if (PlayerUtils.isInGame()) {
            TextureRenderer t = this.catType.get() == CatType.First ? BOTextures.getCatRenderer() : BOTextures.getCat2Renderer();

            float width = t.getWidth() / 4.0F;
            float height = t.getHeight() / 4.0F;
            this.setSize(width, height);

            boolean isRight = this.side.get() == Side.Right;

            if (isRight) {
                t.quadUV(this.stack, 0.0F, 0.0F, width, height, 1.0F, 0.0F, 0.0F, 1.0F);
            } else {
                t.quadUV(this.stack, 0.0F, 0.0F, width, height, 0.0F, 0.0F, 1.0F, 1.0F);
            }
        }
    }

    public enum CatType {
        First,
        Second
    }

    public enum Side {
        Left,
        Right
    }
}
