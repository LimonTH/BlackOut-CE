package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;

public class MewingGuide extends HudElement {
    public final SettingGroup sgGeneral = this.addGroup("General");

    public MewingGuide() {
        super("Mewing Guide", "Helps you mog all of the block game oppz");
        TextureRenderer t = BOTextures.getMewingIconRenderer();
        if (t != null) {
            this.setSize(t.getWidth() / 4.0F, t.getHeight() / 4.0F);
        } else {
            this.setSize(120.0F, 175.0F);
        }
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            TextureRenderer t = BOTextures.getMewingIconRenderer();
            float width = t.getWidth() / 4.0F;
            float height = t.getHeight() / 4.0F;
            this.setSize(width, height);
            t.quad(this.stack, 0.0F, 0.0F, width, height);
        }
    }
}
