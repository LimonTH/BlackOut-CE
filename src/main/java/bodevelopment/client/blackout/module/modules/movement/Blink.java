package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.defensive.Surround;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Blink extends Module {
    private static Blink INSTANCE;
    public final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<BlinkMode> blinkMode = this.sgGeneral.enumSetting("Mode", BlinkMode.Normal, ".");
    private final Setting<Integer> packets = this.sgGeneral.intSetting("Packets", 10, 0, 50, 1, "Disabled after sending this many packets.");
    private final Setting<Integer> ticks = this.sgGeneral.intSetting("Ticks", 100, 0, 100, 1, ".");
    private final Setting<Boolean> disableSurround = this.sgGeneral.booleanSetting("Disable On Surround", false, ".");
    private final Setting<Boolean> render = this.sgGeneral.booleanSetting("Render", true, ".");
    private final Setting<RenderShape> renderShape = this.sgGeneral.enumSetting("Render Shape", RenderShape.Full, "Which parts should be rendered.");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Line Color", new BlackOutColor(255, 0, 0, 255), ".");
    private final Setting<BlackOutColor> sideColor = this.sgGeneral.colorSetting("Side Color", new BlackOutColor(255, 0, 0, 50), ".");
    private int delayed = 0;
    private Box box = null;
    private int time = 0;

    public Blink() {
        super("Blink", "Basically fakes huge lag.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static Blink getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        this.delayed = 0;
        this.time = 0;
        if (BlackOut.mc.player != null) {
            this.box = BlackOut.mc.player.getBoundingBox();
            Vec3d serverPos = Managers.PACKET.pos;
            this.box = new Box(
                    serverPos.x - this.box.getLengthX() / 2.0,
                    serverPos.y,
                    serverPos.z - this.box.getLengthZ() / 2.0,
                    serverPos.x + this.box.getLengthX() / 2.0,
                    serverPos.y + this.box.getLengthY(),
                    serverPos.z + this.box.getLengthZ() / 2.0
            );
        }
    }

    @Override
    public String getInfo() {
        return this.delayed + "/" + this.packets.get();
    }

    @Override
    public boolean shouldSkipListeners() {
        return false;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.time++;
        if (BlackOut.mc.player == null || BlackOut.mc.world == null || this.ticks.get() > 0 && this.time > this.ticks.get()) {
            this.disable();
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (this.enabled) {
            if (this.disableSurround.get() && Surround.getInstance().enabled) {
                this.disable(this.getDisplayName() + "disabled, enabled surround");
            }

            if (this.box != null && this.render.get()) {
                Render3DUtils.box(this.box, this.sideColor.get(), this.lineColor.get(), this.renderShape.get());
            }
        }
    }

    public boolean onSend() {
        if (!this.shouldDelay()) {
            return false;
        } else {
            this.delayed++;
            if (this.packets.get() > 0 && this.delayed >= this.packets.get()) {
                if (this.blinkMode.get() == BlinkMode.Normal) {
                    this.disable(this.getDisplayName() + " reached the limit of " + this.packets.get() + " packets");
                }

                return true;
            } else {
                return true;
            }
        }
    }

    public boolean shouldDelay() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            return switch (this.blinkMode.get()) {
                case Damage ->
                        BlackOut.mc.player.hurtTime > 0 && (this.packets.get() == 0 || BlackOut.mc.player.hurtTime < this.packets.get());
                case Normal -> true;
            };
        } else {
            return false;
        }
    }

    public enum BlinkMode {
        Damage,
        Normal
    }
}
