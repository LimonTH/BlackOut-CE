package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.interfaces.mixin.ICreativeInventoryScreen;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class GUIMove extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgRotation = this.addGroup("Rotation");

    private final Setting<Screens> mode = this.sgGeneral.enumSetting("Screen Filter", Screens.Both, "Determines which graphical interfaces allow player movement (e.g., standard containers vs. personal inventory).");
    private final Setting<Boolean> jump = this.sgGeneral.booleanSetting("Allow Jumping", true, "Enables vertical displacement while an interface is active.");
    private final Setting<Boolean> sneak = this.sgGeneral.booleanSetting("Allow Sneaking", true, "Enables crouching functionality while navigating menus.");
    private final Setting<Boolean> sprint = this.sgGeneral.booleanSetting("Allow Sprinting", true, "Maintains athletic pace and kinetic energy while in screens.");

    private final Setting<Boolean> arrowsRotate = this.sgRotation.booleanSetting("Arrow Key Look", true, "Enables camera orientation control using the keyboard arrow keys.");
    private final Setting<Double> rotateSpeed = this.sgRotation.doubleSetting("Angular Velocity", 4.0, 0.0, 10.0, 0.1, "The speed at which the camera rotates when using keyboard input.");

    public GUIMove() {
        super("GUI Move", "Allows for full character locomotion and camera rotation while navigating graphical interfaces.", SubCategory.MOVEMENT, true);
    }

    @Override
    public void onDisable() {
        this.resetKeys();
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player == null || BlackOut.mc.currentScreen == null || this.skip()) return;

        this.updateKey(BlackOut.mc.options.forwardKey);
        this.updateKey(BlackOut.mc.options.backKey);
        this.updateKey(BlackOut.mc.options.leftKey);
        this.updateKey(BlackOut.mc.options.rightKey);

        if (this.jump.get()) this.updateKey(BlackOut.mc.options.jumpKey);
        if (this.sneak.get()) this.updateKey(BlackOut.mc.options.sneakKey);
        if (this.sprint.get()) this.updateKey(BlackOut.mc.options.sprintKey);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player == null || BlackOut.mc.currentScreen == null || this.skip()) return;

        if (this.arrowsRotate.get()) {
            float speed = (float) (this.rotateSpeed.get() * event.tickDelta * 5);
            float yaw = BlackOut.mc.player.getYaw();
            float pitch = BlackOut.mc.player.getPitch();

            if (isKeyDown(GLFW.GLFW_KEY_LEFT)) yaw -= speed;
            if (isKeyDown(GLFW.GLFW_KEY_RIGHT)) yaw += speed;
            if (isKeyDown(GLFW.GLFW_KEY_UP)) pitch -= speed;
            if (isKeyDown(GLFW.GLFW_KEY_DOWN)) pitch += speed;

            pitch = MathHelper.clamp(pitch, -90, 90);
            BlackOut.mc.player.setYaw(yaw);
            BlackOut.mc.player.setPitch(pitch);
        }
    }

    private void updateKey(KeyBinding bind) {
        InputUtil.Key key = InputUtil.fromTranslationKey(bind.getBoundKeyTranslationKey());
        bind.setPressed(isKeyDown(key.getCode()));
    }

    private boolean isKeyDown(int keyCode) {
        return GLFW.glfwGetKey(BlackOut.mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
    }

    private void resetKeys() {
        if (BlackOut.mc.options == null) return;
        BlackOut.mc.options.forwardKey.setPressed(false);
        BlackOut.mc.options.backKey.setPressed(false);
        BlackOut.mc.options.leftKey.setPressed(false);
        BlackOut.mc.options.rightKey.setPressed(false);
        BlackOut.mc.options.jumpKey.setPressed(false);
        BlackOut.mc.options.sneakKey.setPressed(false);
        BlackOut.mc.options.sprintKey.setPressed(false);
    }

    private boolean skip() {
        if (BlackOut.mc.currentScreen instanceof ChatScreen
                || BlackOut.mc.currentScreen instanceof SignEditScreen
                || BlackOut.mc.currentScreen instanceof AnvilScreen
                || BlackOut.mc.currentScreen instanceof AbstractCommandBlockScreen
                || BlackOut.mc.currentScreen instanceof StructureBlockScreen) return true;

        if (BlackOut.mc.currentScreen instanceof CreativeInventoryScreen screen) {
            ItemGroup selectedGroup = ((ICreativeInventoryScreen) screen).blackout_Client$getSelectedTab();
            return selectedGroup != null && selectedGroup.getType() == ItemGroup.Type.SEARCH;
        }

        return switch (this.mode.get()) {
            case Inventory -> !(BlackOut.mc.currentScreen instanceof InventoryScreen);
            case GUI -> (BlackOut.mc.currentScreen instanceof InventoryScreen);
            default -> false;
        };
    }

    public enum Screens {
        GUI, Inventory, Both
    }
}