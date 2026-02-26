package bodevelopment.client.blackout.module;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.BindMode;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwingState;
import bodevelopment.client.blackout.enums.SwingType;
import bodevelopment.client.blackout.event.events.ModuleEvent;
import bodevelopment.client.blackout.helpers.RotationHelper;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.modules.visual.misc.SwingModifier;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.Settings;
import bodevelopment.client.blackout.module.setting.WarningSettingGroup;
import bodevelopment.client.blackout.module.setting.settings.KeyBindSetting;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.util.ChatUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import bodevelopment.client.blackout.util.SoundUtils;
import com.google.gson.JsonObject;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Module extends RotationHelper {
    public final String name;
    public final String description;
    public final SubCategory category;
    public final List<SettingGroup> settingGroups = new ArrayList<>();
    public final SettingGroup sgModule = this.addGroup("Module");
    public final KeyBindSetting bind;
    public final Setting<BindMode> bindMode;
    private final Setting<String> displayName;
    public boolean enabled = false;
    public long toggleTime = 0L;

    public Module(String name, String description, SubCategory category, boolean subscribe) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.set(this);

        this.displayName = this.sgModule.stringSetting("Name", name, "The internal name used for this module in the interface.");
        this.bind = (KeyBindSetting) Settings.keySetting("Bind", "The keyboard key assigned to toggle this module.", null);
        this.bindMode = this.sgModule.enumSetting("Bind Mode", BindMode.Toggle, "Determines if the module toggles on press or stays active only while holding the key.");

        if (subscribe) {
            BlackOut.EVENT_BUS.subscribe(this, this::shouldSkipListeners);
        }
    }

    public boolean toggleable() {
        return true;
    }

    public String getFileName() {
        return this.name.replaceAll(" ", "");
    }

    public String getDisplayName() {
        String dn = this.displayName.get();
        return dn.isEmpty() ? this.name : dn;
    }

    public void toggle() {
        if (this.enabled) {
            this.disable();
        } else {
            this.enable();
        }
    }

    public void silentEnable() {
        this.enable(null, 0, false);
    }

    public void enable() {
        this.enable(null, 2, true);
    }

    public void enable(String message) {
        this.enable(message, 2, true);
    }

    public void enable(String message, int time, boolean sendNotification) {
        if (!this.enabled) {
            BlackOut.EVENT_BUS.post(new ModuleEvent.Enable(this));
            this.onEnable();
            this.enabled = true;
            this.toggleTime = System.currentTimeMillis();
            if (sendNotification) {
                this.sendNotification(
                        message == null ? this.getDisplayName() + Formatting.GREEN + " Enabled" : " " + message,
                        message == null ? "Enabled " + this.getDisplayName() : message,
                        "Module Toggle",
                        Notifications.Type.Enable,
                        time == 0 ? 2 : time
                );
                if (Notifications.getInstance().sound.get()) {
                    SoundUtils.play(1.0F, 1.0F, "enable");
                }
            }
        }
    }

    public void silentDisable() {
        this.doDisable(null, 0, Notifications.Type.Disable, false);
    }

    public void disable() {
        this.disable(null, 2);
    }

    public void disable(String message) {
        this.disable(message, 2);
    }

    public void disable(String message, int time) {
        this.doDisable(message, time, Notifications.Type.Disable, true);
    }

    public void disable(String message, int time, Notifications.Type type) {
        this.doDisable(message, time, type, true);
    }

    private void doDisable(String message, int time, Notifications.Type type, Boolean sendNotification) {
        if (this.enabled) {
            BlackOut.EVENT_BUS.post(new ModuleEvent.Disable(this));
            this.onDisable();
            this.enabled = false;
            this.toggleTime = System.currentTimeMillis();
            if (sendNotification) {
                this.sendNotification(
                        message == null ? this.getDisplayName() + Formatting.RED + " OFF" : " " + message,
                        message == null ? "Disabled " + this.getDisplayName() : message,
                        "Module Toggle",
                        type,
                        time == 0 ? 2 : time
                );
                if (Notifications.getInstance().sound.get()) {
                    SoundUtils.play(1.0F, 1.0F, "disable");
                }
            }
        }
    }

    protected void sendNotification(String chatMessage, String text, String bigText, Notifications.Type type, double time) {
        Notifications notifications = Notifications.getInstance();
        if (notifications.chatNotifications.get()) {
            this.sendMessage(chatMessage);
        }

        Managers.NOTIFICATIONS.addNotification(text, bigText, time, type);
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public String getInfo() {
        return null;
    }

    protected void sendMessage(String message) {
        ChatUtils.addMessage(Notifications.getInstance().getClientPrefix() + " " + message, Objects.hash(this.name + "toggle"));
    }

    protected void sendPacket(Packet<?> packet) {
        Managers.PACKET.sendPacket(packet);
    }

    protected void sendInstantly(Packet<?> packet) {
        Managers.PACKET.sendInstantly(packet);
    }

    protected void sendSequencedInstantly(SequencedPacketCreator packetCreator) {
        if (BlackOut.mc.interactionManager != null && BlackOut.mc.world != null) {
            PendingUpdateManager sequence = BlackOut.mc.world.getPendingUpdateManager().incrementSequence();
            Packet<?> packet = packetCreator.predict(sequence.getSequence());
            this.sendInstantly(packet);
            sequence.close();
        }
    }

    protected void sendSequenced(SequencedPacketCreator packetCreator) {
        if (BlackOut.mc.interactionManager != null && BlackOut.mc.world != null) {
            PendingUpdateManager sequence = BlackOut.mc.world.getPendingUpdateManager().incrementSequence();
            Packet<?> packet = packetCreator.predict(sequence.getSequence());
            this.sendPacket(packet);
            sequence.close();
        }
    }

    protected void sendSequencedPostGrim(SequencedPacketCreator packetCreator) {
        if (BlackOut.mc.interactionManager != null && BlackOut.mc.world != null) {
            PendingUpdateManager sequence = BlackOut.mc.world.getPendingUpdateManager().incrementSequence();
            Packet<?> packet = packetCreator.predict(sequence.getSequence());
            Managers.PACKET.sendPostPacket(packet);
            sequence.close();
        }
    }

    protected void placeBlock(Hand hand, PlaceData data) {
        boolean shouldSneak = data.sneak() && !BlackOut.mc.player.isSneaking();
        if (shouldSneak) {
            this.sendPacket(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        }

        this.placeBlock(hand, data.pos().toCenterPos(), data.dir(), data.pos());
        if (shouldSneak) {
            this.sendPacket(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }
    }

    protected void placeBlock(Hand hand, Vec3d blockHitVec, Direction blockDirection, BlockPos pos) {
        Hand finalHand = Objects.requireNonNullElse(hand, Hand.MAIN_HAND);
        Vec3d eyes = BlackOut.mc.player.getEyePos();
        boolean inside = eyes.x > pos.getX()
                && eyes.x < pos.getX() + 1
                && eyes.y > pos.getY()
                && eyes.y < pos.getY() + 1
                && eyes.z > pos.getZ()
                && eyes.z < pos.getZ() + 1;
        SettingUtils.swing(SwingState.Pre, SwingType.Placing, finalHand);
        this.sendSequenced(s -> new PlayerInteractBlockC2SPacket(finalHand, new BlockHitResult(blockHitVec, blockDirection, pos, inside), s));
        SettingUtils.swing(SwingState.Post, SwingType.Placing, finalHand);
    }

    protected void interactBlock(Hand hand, Vec3d blockHitVec, Direction blockDirection, BlockPos pos) {
        Hand finalHand = Objects.requireNonNullElse(hand, Hand.MAIN_HAND);
        Vec3d eyes = BlackOut.mc.player.getEyePos();
        boolean inside = eyes.x > pos.getX()
                && eyes.x < pos.getX() + 1
                && eyes.y > pos.getY()
                && eyes.y < pos.getY() + 1
                && eyes.z > pos.getZ()
                && eyes.z < pos.getZ() + 1;
        SettingUtils.swing(SwingState.Pre, SwingType.Interact, finalHand);
        this.sendSequenced(s -> new PlayerInteractBlockC2SPacket(finalHand, new BlockHitResult(blockHitVec, blockDirection, pos, inside), s));
        SettingUtils.swing(SwingState.Post, SwingType.Interact, finalHand);
    }

    protected void useItem(Hand hand) {
        Hand finalHand = Objects.requireNonNullElse(hand, Hand.MAIN_HAND);
        float yaw = Managers.ROTATION.prevYaw;
        float pitch = Managers.ROTATION.prevPitch;
        if (SettingUtils.grimUsing()) {
            this.sendPacket(
                    new PlayerMoveC2SPacket.Full(
                            BlackOut.mc.player.getX(),
                            BlackOut.mc.player.getY(),
                            BlackOut.mc.player.getZ(),
                            yaw,
                            pitch,
                            Managers.PACKET.isOnGround()
                    )
            );
        }

        SettingUtils.swing(SwingState.Pre, SwingType.Using, finalHand);
        this.sendSequenced(s -> new PlayerInteractItemC2SPacket(finalHand, s, yaw, pitch));
        SettingUtils.swing(SwingState.Post, SwingType.Using, finalHand);
    }

    protected void useItemInstantly(Hand hand) {
        Hand finalHand = Objects.requireNonNullElse(hand, Hand.MAIN_HAND);
        float yaw = Managers.ROTATION.prevYaw;
        float pitch = Managers.ROTATION.prevPitch;
        if (SettingUtils.grimUsing()) {
            this.sendPacket(
                    new PlayerMoveC2SPacket.Full(
                            BlackOut.mc.player.getX(),
                            BlackOut.mc.player.getY(),
                            BlackOut.mc.player.getZ(),
                            Managers.ROTATION.prevYaw,
                            Managers.ROTATION.prevPitch,
                            Managers.PACKET.isOnGround()
                    )
            );
        }

        SettingUtils.swing(SwingState.Pre, SwingType.Using, finalHand);
        this.sendSequencedInstantly(s -> new PlayerInteractItemC2SPacket(finalHand, s, yaw, pitch));
        SettingUtils.swing(SwingState.Post, SwingType.Using, finalHand);
    }

    protected void releaseUseItem() {
        this.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN, 0));
    }

    protected void attackEntity(Entity entity) {
        SettingUtils.swing(SwingState.Pre, SwingType.Attacking, Hand.MAIN_HAND);
        this.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, BlackOut.mc.player.isSneaking()));
        SettingUtils.swing(SwingState.Post, SwingType.Attacking, Hand.MAIN_HAND);
        if (entity instanceof EndCrystalEntity) {
            Managers.ENTITY.setSemiDead(entity.getId());
        }
    }

    protected void clientSwing(SwingHand swingHand, Hand realHand) {
        Hand hand = switch (swingHand) {
            case MainHand -> Hand.MAIN_HAND;
            case OffHand -> Hand.OFF_HAND;
            case RealHand -> Objects.requireNonNullElse(realHand, Hand.MAIN_HAND);
        };
        BlackOut.mc.player.swingHand(hand, true);
        SwingModifier.getInstance().startSwing(hand);
    }

    protected void blockPlaceSound(BlockPos pos, ItemStack stack) {
        if (stack != null) {
            this.blockPlaceSound(pos, stack.getItem());
        }
    }

    protected void blockPlaceSound(BlockPos pos, Item item) {
        if (item instanceof BlockItem blockItem) {
            this.blockPlaceSound(pos, blockItem);
        }
    }

    protected void blockPlaceSound(BlockPos pos, BlockItem blockItem) {
        BlackOut.mc
                .world
                .playSound(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        blockItem.getPlaceSound(BlackOut.mc.world.getBlockState(pos)),
                        SoundCategory.BLOCKS,
                        1.0F,
                        1.0F,
                        true
                );
    }

    protected void blockPlaceSound(BlockPos pos, BlockItem blockItem, float volume, float pitch, boolean distance) {
        BlackOut.mc
                .world
                .playSound(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        blockItem.getPlaceSound(BlackOut.mc.world.getBlockState(pos)),
                        SoundCategory.BLOCKS,
                        volume,
                        pitch,
                        distance
                );
    }

    protected SettingGroup addGroup(String name) {
        SettingGroup group = new SettingGroup(name);
        this.settingGroups.add(group);
        return group;
    }

    protected SettingGroup addGroup(String name, String warning) {
        SettingGroup group = new WarningSettingGroup(name, warning);
        this.settingGroups.add(group);
        return group;
    }

    public void readSettings(JsonObject jsonObject) {
        this.settingGroups.forEach(group -> group.settings.forEach(s -> s.read(jsonObject)));
    }

    public void writeSettings(JsonObject jsonObject) {
        this.settingGroups.forEach(group -> group.settings.forEach(s -> s.write(jsonObject)));
    }

    public boolean shouldSkipListeners() {
        return !this.enabled;
    }

    protected void closeInventory() {
        this.sendPacket(new CloseHandledScreenC2SPacket(BlackOut.mc.player.currentScreenHandler.syncId));
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof Module module && module.name.equals(this.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }

}
