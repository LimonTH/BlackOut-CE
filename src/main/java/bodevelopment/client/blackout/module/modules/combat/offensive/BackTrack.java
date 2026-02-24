package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.mixin.accessors.AccessorInteractEntityC2SPacket;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.randomstuff.timers.TickTimerList;
import bodevelopment.client.blackout.randomstuff.timers.TimerMap;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class BackTrack extends Module {
    private static BackTrack INSTANCE;
    public final TickTimerList<Pair<OtherClientPlayerEntity, Box>> hit = new TickTimerList<>(false);
    public final TickTimerList<Pair<OtherClientPlayerEntity, Box>> spoofed = new TickTimerList<>(false);
    public final TimerMap<OtherClientPlayerEntity, Vec3d> realPositions = new TimerMap<>(false);
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Integer> time = this.sgGeneral.intSetting("Ticks", 5, 0, 20, 1, ".");
    public final Setting<Integer> maxTime = this.sgGeneral.intSetting("Max Ticks", 50, 0, 100, 1, ".");
    private final Setting<RenderShape> renderShape = this.sgGeneral.enumSetting("Render Shape", RenderShape.Full, "Which parts of render should be rendered.");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Line Color", new BlackOutColor(255, 0, 0, 255), "Line color of rendered boxes.");
    private final Setting<BlackOutColor> sideColor = this.sgGeneral.colorSetting("Side Color", new BlackOutColor(255, 0, 0, 50), "Side color of rendered boxes.");

    public BackTrack() {
        super("Back Track", ".", SubCategory.OFFENSIVE, true);
        INSTANCE = this;
    }

    public static BackTrack getInstance() {
        return INSTANCE;
    }

    @Event
    public void onSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            AccessorInteractEntityC2SPacket accessor = (AccessorInteractEntityC2SPacket) packet;
            if (accessor.getType().getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK && BlackOut.mc.world.getEntityById(accessor.getId()) instanceof OtherClientPlayerEntity player) {
                Box box = player.getBoundingBox();
                Pair<OtherClientPlayerEntity, Box> pair = new Pair<>(player, box);
                this.hit.remove(timer -> timer.value.getLeft().equals(player));
                this.hit.add(pair, this.time.get());
                if (!this.spoofed.contains(timer -> timer.value.getLeft().equals(player))) {
                    this.spoofed.remove(timer -> timer.value.getLeft().equals(player));
                    this.spoofed.add(pair, this.maxTime.get());
                }
            }
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        this.realPositions
                .remove(
                        (key, value) -> {
                            if (System.currentTimeMillis() > value.endTime) {
                                return true;
                            } else {
                                Box box = new Box(
                                        value.value.getX() - 0.3,
                                        value.value.getY(),
                                        value.value.getZ() - 0.3,
                                        value.value.getX() + 0.3,
                                        value.value.getY() + key.getBoundingBox().getLengthY(),
                                        value.value.getZ() + 0.3
                                );
                                Render3DUtils.box(box, this.sideColor.get(), this.lineColor.get(), this.renderShape.get());
                                return false;
                            }
                        }
                );
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.hit.timers.removeIf(item -> {
            if (item.ticks-- <= 0) {
                this.spoofed.remove(timer -> ((Pair) item.value).getLeft().equals(timer.value.getLeft()));
                return true;
            } else {
                return false;
            }
        });
        this.spoofed.update();
    }
}
