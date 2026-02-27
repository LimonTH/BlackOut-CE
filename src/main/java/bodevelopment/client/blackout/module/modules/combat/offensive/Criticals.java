package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.mixin.accessors.AccessorInteractEntityC2SPacket;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.OnlyDev;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

// TODO: NEED PATCHES
// TODO: добавить общий cooldown на crit packets, чтобы не флагаться на burst-атаках.
// TODO: учитывать задержку сервера и reset shouldSpoof при teleport/correction пакете.
// TODO: добавить проверки окружения (slime, honey, web) где крит не должен отправляться.
// TODO: расширить проверку hasSpace (2.1) с учётом collidable blocks и bounding box.
// TODO: синхронизировать режимы с Aura/AutoCrystal, чтобы не конфликтовать.
// TODO: добавить fallback, если player не на земле, но event приходит из packet spoof.
// TODO: добавить оптимизацию для Grim/Matrix с более безопасными микросдвигами.
// TODO: обновить Strict режим: ограничить spoofTime при движении и reset при input.
// TODO: добавить опцию исключать certain entity types (armor stands, ender crystals).
@OnlyDev
public class Criticals extends Module {
    private static Criticals INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Bypass Mode", Mode.Packet, "The method used to trick the server into registering a critical hit.");
    private final Setting<Integer> spoofTime = this.sgGeneral.intSetting("Spoof Duration", 500, 0, 2500, 50, "The duration in milliseconds to maintain a false falling state for Strict mode.", () -> this.mode.get() == Mode.Strict);

    private boolean shouldSpoof = false;
    private long prevJump = 0L;

    public Criticals() {
        super("Criticals", "Forces every attack to be a critical hit by manipulating the player's vertical position or movement packets.", SubCategory.OFFENSIVE, true);
        INSTANCE = this;
    }

    public static Criticals getInstance() {
        return INSTANCE;
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    @Event
    public void onSend(PacketEvent.Send event) {
        if (BlackOut.mc.player == null || BlackOut.mc.world == null) return;
        if (Aura.getInstance().enabled && Aura.getInstance().isAttacking) return;

        if (event.packet instanceof AccessorInteractEntityC2SPacket packet
                && packet.getType().getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {

            Entity attackedEntity = BlackOut.mc.world.getEntityById(packet.getId());

            if (attackedEntity == null ||
                    attackedEntity instanceof ItemEntity ||
                    attackedEntity instanceof ExperienceOrbEntity) return;

            if (BlackOut.mc.player.isSubmergedInWater() || BlackOut.mc.player.isInLava() ||
                    BlackOut.mc.player.isClimbing() || BlackOut.mc.player.hasStatusEffect(StatusEffects.BLINDNESS))
                return;

            if (BlackOut.mc.player.isOnGround()) {
                doCritLogic();
            }
        }
    }

    public void doCritLogic() {
        double x = BlackOut.mc.player.getX();
        double y = BlackOut.mc.player.getY();
        double z = BlackOut.mc.player.getZ();

        boolean hasSpace = BlackOut.mc.world.getBlockState(new BlockPos((int) x, (int) (y + 2.1), (int) z)).isAir();

        switch (this.mode.get()) {
            case Packet:
                double pOffset = hasSpace ? 0.2 : 0.01;
                sendPos(x, y + pOffset, z, false);
                sendPos(x, y, z, false);
                break;

            case NCP:
                double nOffset = hasSpace ? 0.11 : 0.02;
                sendPos(x, y + nOffset, z, false);
                sendPos(x, y + 0.01, z, false);
                break;

            case Jump:
                if (hasSpace) BlackOut.mc.player.jump();
                break;

            case Strict:
                if (!this.input() && Managers.PACKET.isOnGround()) {
                    sendPos(x, y + 1.1E-7, z, false);
                    sendPos(x, y + 1.0E-8, z, false);
                    this.prevJump = System.currentTimeMillis();
                    this.shouldSpoof = true;
                }
                break;

            case BlocksMC:
                if (BlackOut.mc.player.age % 4 == 0) {
                    sendPos(x, y + 0.0011, z, true);
                    sendPos(x, y, z, false);
                }
                break;

            case Grim:
                sendPos(x, y + 0.000000118, z, false);
                sendPos(x, y + 0.000000011, z, false);
                break;

            case GrimOld:
                Vec3d pos = Managers.PACKET.pos;
                this.sendPacket(new PlayerMoveC2SPacket.Full(pos.getX(), pos.getY() - 1.0E-6, pos.getZ(),
                        Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch, false));
                break;

            case Matrix:
                double mOffset = hasSpace ? 0.0625 : 0.0001;
                sendPos(x, y + mOffset, z, false);
                sendPos(x, y + 0.0001, z, false);
                break;

            case MiniHop:
                sendPos(x, y + 0.001337, z, false);
                sendPos(x, y, z, false);
                break;
        }
    }

    private void sendPos(double x, double y, double z, boolean onGround) {
        this.sendInstantly(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround));
    }

    @Event
    public void onSent(PacketEvent.Sent event) {
        if (Aura.getInstance().enabled && Aura.getInstance().isAttacking) return;
        if (event.packet instanceof PlayerMoveC2SPacket packet && packet.isOnGround()) {
            this.shouldSpoof = false;
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player == null) return;
        if (this.input()) this.shouldSpoof = false;

        if (this.shouldSpoof && (System.currentTimeMillis() - this.prevJump < this.spoofTime.get())) {
            Managers.PACKET.spoofOG(false);
        } else {
            this.shouldSpoof = false;
        }
    }

    private boolean input() {
        if (BlackOut.mc.player == null) return false;
        return BlackOut.mc.player.input.getMovementInput().lengthSquared() > 0.0F || BlackOut.mc.player.input.jumping;
    }

    public enum Mode {
        Packet, Jump, NCP, Strict, BlocksMC, Grim, GrimOld, Matrix, MiniHop
    }
}