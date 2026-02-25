package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;

public class CatGirl extends HudElement {
    public final SettingGroup sgGeneral = this.addGroup("General");

    public CatGirl() {
        super("Cat Girl", "Renders a static decorative texture overlay on the HUD interface.");
        TextureRenderer t = BOTextures.getCat2Renderer();
        this.setSize(t.getWidth() / 4.0F, t.getHeight() / 4.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            TextureRenderer t = BOTextures.getCat2Renderer();
            float width = t.getWidth() / 4.0F;
            float height = t.getHeight() / 4.0F;
            this.setSize(width, height);
            t.quad(this.stack, 0.0F, 0.0F, width, height);
        }
    }

    public enum Side {
        Left,
        Right
    }
}
