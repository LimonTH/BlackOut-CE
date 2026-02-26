package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.TextElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.util.math.Vec3d;

public class SpeedDisplay extends TextElement {
    public final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> onlyHorizontal = this.sgGeneral.booleanSetting("Only Horizontal", true, "Excludes vertical velocity from calculation.");
    private final Setting<Integer> decimals = this.sgGeneral.intSetting("Decimals", 1, 0, 3, 1, "How many decimal places to show.");

    public SpeedDisplay() {
        super("Speed", "Displays your current movement velocity in blocks per second.");
        this.setSize(40.0F, 10.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player == null) return;

        Vec3d vel = BlackOut.mc.player.getVelocity();
        double speed = onlyHorizontal.get() ? vel.horizontalLength() : vel.length();
        speed *= 20.0;

        String speedString = String.format("%." + decimals.get() + "f", speed);

        this.drawElement(this.stack, "Speed", speedString + " bps");
    }
}