package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.hud.HudEditor;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import com.mojang.blaze3d.platform.InputConstants;
import bodevelopment.client.blackout.interfaces.mixin.ICreativeModeInventoryScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import org.lwjgl.glfw.GLFW;

public class GUIMove extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgRotation = this.addGroup("Rotation");

    private final Setting<Screens> mode = this.sgGeneral.enumSetting("Screen Filter", Screens.Both, "Determines which graphical interfaces allow player movement (e.g., standard containers vs. personal inventory).");
    private final Setting<Boolean> jump = this.sgGeneral.booleanSetting("Allow Jumping", true, "Enables vertical displacement while an interface is active.");
    private final Setting<SneakMode> sneakMode = this.sgGeneral.enumSetting("Sneak Mode", SneakMode.Normal, "How crouching is handled in screens.");
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
        if (BlackOut.mc.player == null || BlackOut.mc.screen == null || this.skip()) return;

        this.updateKey(BlackOut.mc.options.keyUp);
        this.updateKey(BlackOut.mc.options.keyDown);
        this.updateKey(BlackOut.mc.options.keyLeft);
        this.updateKey(BlackOut.mc.options.keyRight);

        if (this.jump.get()) this.updateKey(BlackOut.mc.options.keyJump);
        if (this.sprint.get()) this.updateKey(BlackOut.mc.options.keySprint);

        switch (this.sneakMode.get()) {
            case Normal -> this.updateKey(BlackOut.mc.options.keyShift);
            case Packet -> {
                boolean isDown = isKeyDown(InputConstants.getKey(BlackOut.mc.options.keyShift.saveString()).getValue());
                BlackOut.mc.player.setShiftKeyDown(isDown);
            }
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player == null || BlackOut.mc.screen == null || this.skip()) return;

        if (this.arrowsRotate.get()) {
            float speed = (float) (this.rotateSpeed.get() * event.tickDelta * 5);
            float yaw = BlackOut.mc.player.getYRot();
            float pitch = BlackOut.mc.player.getXRot();

            if (isKeyDown(GLFW.GLFW_KEY_LEFT)) yaw -= speed;
            if (isKeyDown(GLFW.GLFW_KEY_RIGHT)) yaw += speed;
            if (isKeyDown(GLFW.GLFW_KEY_UP)) pitch -= speed;
            if (isKeyDown(GLFW.GLFW_KEY_DOWN)) pitch += speed;

            pitch = Mth.clamp(pitch, -90, 90);
            BlackOut.mc.player.setYRot(yaw);
            BlackOut.mc.player.setXRot(pitch);
        }
    }

    private void updateKey(KeyMapping bind) {
        InputConstants.Key key = InputConstants.getKey(bind.saveString());
        boolean isDown = isKeyDown(key.getValue());

        if (bind.isDown() != isDown) {
            bind.setDown(isDown);
        }
    }

    private boolean isKeyDown(int keyCode) {
        return GLFW.glfwGetKey(BlackOut.mc.getWindow().getWindow(), keyCode) == GLFW.GLFW_PRESS;
    }

    private void resetKeys() {
        if (BlackOut.mc.options == null) return;
        BlackOut.mc.options.keyUp.setDown(false);
        BlackOut.mc.options.keyDown.setDown(false);
        BlackOut.mc.options.keyLeft.setDown(false);
        BlackOut.mc.options.keyRight.setDown(false);
        BlackOut.mc.options.keyJump.setDown(false);
        BlackOut.mc.options.keyShift.setDown(false);
        BlackOut.mc.options.keySprint.setDown(false);
    }

    private boolean skip() {
        if (BlackOut.mc.screen instanceof ChatScreen
                || BlackOut.mc.screen instanceof SignEditScreen
                || BlackOut.mc.screen instanceof AnvilScreen
                || BlackOut.mc.screen instanceof AbstractCommandBlockEditScreen
                || BlackOut.mc.screen instanceof BookEditScreen
                || Managers.CLICK_GUI.CLICK_GUI.isOpen()
                || HudEditor.isOpen()
                || BlackOut.mc.screen instanceof StructureBlockEditScreen) return true;

        if (BlackOut.mc.screen instanceof CreativeModeInventoryScreen screen) {
            CreativeModeTab selectedGroup = ((ICreativeModeInventoryScreen) screen).blackout_Client$getSelectedTab();
            return selectedGroup != null && selectedGroup.getType() == CreativeModeTab.Type.SEARCH;
        }

        return switch (this.mode.get()) {
            case Inventory -> !(BlackOut.mc.screen instanceof InventoryScreen);
            case GUI -> (BlackOut.mc.screen instanceof InventoryScreen);
            default -> false;
        };
    }

    public enum Screens {
        GUI, Inventory, Both
    }

    public enum SneakMode {
        Normal,
        Packet,
        Disabled
    }
}