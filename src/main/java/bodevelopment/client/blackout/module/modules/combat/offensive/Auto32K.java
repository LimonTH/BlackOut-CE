package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.util.*;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.block.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Auto32K extends Module {
    private static Auto32K INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<Mode> mode = this.sgGeneral.enumSetting("Operation Mode", Mode.Hopper, "How the 32k sword should be obtained. Hopper is simple, Dispenser is for automated setup.");
    private final Setting<Boolean> yCheck = this.sgGeneral.booleanSetting("Pitch Check", true, "Prevents dispenser placement if your look pitch is too high/low.", () -> this.mode.get() == Mode.Dispenser);
    private final Setting<Boolean> serverDir = this.sgGeneral.booleanSetting("Server Facing", true, "Ensures the dispenser faces the correct direction based on server-side orientation.", () -> this.mode.get() == Mode.Dispenser);
    private final Setting<ObsidianModule.RotationMode> rotationMode = this.sgGeneral.enumSetting("Rotation Logic", ObsidianModule.RotationMode.Instant, "The rotation method used for dispenser and support placement.", () -> this.mode.get() == Mode.Dispenser);
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Inventory Swap", SwitchMode.Silent, "How the module handles item switching for placement.");
    private final Setting<Boolean> currentSlot = this.sgGeneral.booleanSetting("Use Selected Slot", true, "Attempts to move the 32k sword into your currently selected hotbar slot.");
    private final Setting<Integer> swordSlot = this.sgGeneral.intSetting("Target Hotbar Slot", 1, 1, 9, 1, "The specific hotbar slot to move the sword into.", () -> !this.currentSlot.get());
    private final Setting<Boolean> silent = this.sgGeneral.booleanSetting("Silent GUI", true, "Attempts to process container interactions without showing the GUI on your screen.");
    private final Setting<RenderShape> renderShapeHopper = this.sgRender.enumSetting("Hopper Shape", RenderShape.Full, "Visual style for the hopper placement highlight.");
    private final Setting<BlackOutColor> lineColorHopper = this.sgRender.colorSetting("Hopper Line", new BlackOutColor(255, 255, 255, 255), "Outline color for the hopper block.");
    private final Setting<BlackOutColor> sideColorHopper = this.sgRender.colorSetting("Hopper Fill", new BlackOutColor(255, 255, 255, 50), "Face color for the hopper block.");
    private final Setting<RenderShape> renderShapeShulker = this.sgRender.enumSetting("Shulker Shape", RenderShape.Full, "Visual style for the shulker box placement highlight.");
    private final Setting<BlackOutColor> lineColorShulker = this.sgRender.colorSetting("Shulker Line", new BlackOutColor(255, 0, 0, 255), "Outline color for the shulker box.");
    private final Setting<BlackOutColor> sideColorShulker = this.sgRender.colorSetting("Shulker Fill", new BlackOutColor(255, 0, 0, 50), "Face color for the shulker box.");
    private final Setting<RenderShape> renderShapeDispenser = this.sgRender.enumSetting("Dispenser Shape", RenderShape.Full, "Visual style for the dispenser placement highlight.");
    private final Setting<BlackOutColor> lineColorDispenser = this.sgRender.colorSetting("Dispenser Line", new BlackOutColor(255, 255, 255, 255), "Outline color for the dispenser block.");
    private final Setting<BlackOutColor> sideColorDispenser = this.sgRender.colorSetting("Dispenser Fill", new BlackOutColor(255, 255, 255, 50), "Face color for the dispenser block.");
    private final Setting<RenderShape> renderShapeRedstone = this.sgRender.enumSetting("Redstone Shape", RenderShape.Full, "Visual style for the redstone block placement highlight.");
    private final Setting<BlackOutColor> lineColorRedstone = this.sgRender.colorSetting("Redstone Line", new BlackOutColor(255, 0, 0, 255), "Outline color for the redstone activation block.");
    private final Setting<BlackOutColor> sideColorRedstone = this.sgRender.colorSetting("Redstone Fill", new BlackOutColor(255, 0, 0, 50), "Face color for the redstone activation block.");

    private Direction dispenserDir = null;
    private BlockPos hopperPos = null;
    private BlockPos supportPos = null;
    private BlockPos dispenserPos = null;
    private BlockPos redstonePos = null;
    private boolean valid = false;
    private BlockPos boxInside = BlockPos.ORIGIN;
    private BlockPos openedBox = BlockPos.ORIGIN;
    private BlockPos openedHopper = BlockPos.ORIGIN;
    private boolean placed = false;
    private boolean found = false;
    private BlockPos calcMiddle = BlockPos.ORIGIN;
    private int progress = 0;
    private Direction calcDispenserDir = null;
    private BlockPos calcHopperPos = null;
    private BlockPos calcSupportPos = null;
    private BlockPos calcDispenserPos = null;
    private BlockPos calcRedstonePos = null;
    private boolean calcValid = false;
    private double calcValue = 0.0;
    private int calcR = 0;

    public Auto32K() {
        super("Auto 32K", "Automatically sets up a hopper/dispenser system to retrieve and equip '32k' (super-enchanted) weapons.", SubCategory.OFFENSIVE, true);
        INSTANCE = this;
    }

    public static Auto32K getInstance() {
        return INSTANCE;
    }

    public boolean isSilenting() {
        if (BlackOut.mc.player == null || BlackOut.mc.world == null) {
            return false;
        } else {
            return this.silent.get() && (BlackOut.mc.player.currentScreenHandler instanceof Generic3x3ContainerScreenHandler || BlackOut.mc.player.currentScreenHandler instanceof HopperScreenHandler);
        }
    }

    @Override
    public void onEnable() {
        this.resetPos();
    }

    @Event
    public void onMove(MoveEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.shouldCalc()) {
                this.calc(1.0F);
                this.endCalc();
            }

            if (!SettingUtils.grimPackets()) {
                this.update(false);
            }
        }
    }

    @Event
    public void onTickPre(TickEvent.Pre event) {
        this.placed = false;
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && SettingUtils.grimPackets()) {
            this.update(false);
        }
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.update(true);
            if (this.shouldCalc()) {
                this.startCalc();
            }
        }
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.shouldCalc()) {
                this.calc(event.tickDelta);
            }
        }
    }

    @Event
    public void onRenderPost(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.hopperPos != null) {
                this.renderBox(this.hopperPos, this.renderShapeHopper, this.sideColorHopper, this.lineColorHopper);
            }

            if (this.hopperPos != null) {
                this.renderBox(this.hopperPos.up(), this.renderShapeShulker, this.sideColorShulker, this.lineColorShulker);
            }

            if (this.dispenserPos != null) {
                this.renderBox(this.dispenserPos, this.renderShapeDispenser, this.sideColorDispenser, this.lineColorDispenser);
            }

            if (this.redstonePos != null) {
                this.renderBox(this.redstonePos, this.renderShapeRedstone, this.sideColorRedstone, this.lineColorRedstone);
            }
        }
    }

    private void renderBox(BlockPos pos, Setting<RenderShape> shape, Setting<BlackOutColor> sideColor, Setting<BlackOutColor> lineColor) {
        Render3DUtils.box(BoxUtils.get(pos), sideColor.get(), lineColor.get(), shape.get());
    }

    private void resetPos() {
        this.dispenserDir = null;
        this.hopperPos = null;
        this.supportPos = null;
        this.dispenserPos = null;
        this.redstonePos = null;
        this.valid = false;
        this.boxInside = BlockPos.ORIGIN;
        this.openedBox = BlockPos.ORIGIN;
        this.openedHopper = BlockPos.ORIGIN;
        this.placed = false;
        this.found = false;
        this.calcMiddle = BlockPos.ORIGIN;
        this.progress = 0;
        this.calcDispenserDir = null;
        this.calcHopperPos = null;
        this.calcSupportPos = null;
        this.calcDispenserPos = null;
        this.calcRedstonePos = null;
        this.calcValid = false;
        this.calcValue = 0.0;
        this.calcR = 0;
    }

    private void calc(float tickDelta) {
        if (this.calcMiddle != null) {
            int d = this.calcR * 2 + 1;
            int target = d * d * d;

            for (int i = this.progress; i < target * tickDelta; i++) {
                this.progress = i;
                int x = i % d - this.calcR;
                int y = i / d % d - this.calcR;
                int z = i / d / d % d - this.calcR;
                BlockPos pos = this.calcMiddle.add(x, y, z);
                this.updatePos(pos);
            }
        }
    }

    private boolean shouldCalc() {
        return !this.valid || !this.found;
    }

    private void startCalc() {
        this.calcDispenserPos = null;
        this.calcHopperPos = null;
        this.calcRedstonePos = null;
        this.calcSupportPos = null;
        this.calcDispenserDir = null;
        this.calcValue = -42069.0;
        this.found = false;
        this.calcValid = false;
        this.progress = 0;
        this.calcR = (int) Math.ceil(SettingUtils.maxPlaceRange());
        this.calcMiddle = BlockPos.ofFloored(BlackOut.mc.player.getEyePos());
    }

    private void endCalc() {
        this.dispenserDir = this.calcDispenserDir;
        this.hopperPos = this.calcHopperPos;
        this.supportPos = this.calcSupportPos;
        this.dispenserPos = this.calcDispenserPos;
        this.redstonePos = this.calcRedstonePos;
        this.found = this.valid = this.calcValid;
    }

    private void update(boolean place) {
        switch (this.mode.get()) {
            case Hopper:
                this.moveSword();
                if (!this.valid) {
                    return;
                }

                this.place(Blocks.HOPPER, this.hopperPos, place);
                if (this.placeShulker(place)) {
                    this.hopperUpdate();
                }
                break;
            case Dispenser:
                this.moveSword();
                if (!this.valid) {
                    return;
                }

                this.place(Blocks.HOPPER, this.hopperPos, place);
                this.place(Blocks.OBSIDIAN, this.supportPos, place);
                this.place(Blocks.DISPENSER, this.dispenserPos, place);
                if (!this.boxUpdate()) {
                    return;
                }

                this.place(Blocks.REDSTONE_BLOCK, this.redstonePos, place);
                this.hopperUpdate();
        }
    }

    private boolean boxUpdate() {
        if (this.dispenserPos == null) {
            return false;
        } else if (this.get(this.dispenserPos).getBlock() != Blocks.DISPENSER) {
            return false;
        } else {
            Direction dir = SettingUtils.getPlaceOnDirection(this.dispenserPos);
            if (dir == null) {
                return false;
            } else {
                boolean isOpened = this.openedBox.equals(this.dispenserPos);
                boolean isBox = this.boxInside.equals(this.dispenserPos);
                if (!isOpened) {
                    this.openedBox = this.dispenserPos;
                    this.interactBlock(Hand.MAIN_HAND, this.dispenserPos.toCenterPos(), dir, this.dispenserPos);
                }

                if (!isBox) {
                    this.putBox();
                }

                return isBox;
            }
        }
    }

    private boolean hopperUpdate() {
        if (this.get(this.hopperPos).getBlock() != Blocks.HOPPER) {
            return false;
        } else {
            Direction dir = SettingUtils.getPlaceOnDirection(this.hopperPos);
            if (dir == null) {
                return false;
            } else {
                if (!this.openedHopper.equals(this.hopperPos)) {
                    this.openedHopper = this.hopperPos;
                    this.interactBlock(Hand.MAIN_HAND, this.hopperPos.toCenterPos(), dir, this.hopperPos);
                }

                return this.openedHopper.equals(this.hopperPos);
            }
        }
    }

    private int getSlot() {
        return this.currentSlot.get() ? BlackOut.mc.player.getInventory().selectedSlot : this.swordSlot.get() - 1;
    }

    private void putBox() {
        ScreenHandler handler = BlackOut.mc.player.currentScreenHandler;
        if (handler instanceof Generic3x3ContainerScreenHandler) {
            for (Slot slot : handler.slots) {
                if (OLEPOSSUtils.isShulker(slot.getStack())) {
                    this.boxInside = this.dispenserPos;
                    BlackOut.mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, BlackOut.mc.player);
                    BlackOut.mc.player.closeScreen();
                    return;
                }
            }
        }
    }

    private boolean moveSword() {
        ScreenHandler handler = BlackOut.mc.player.currentScreenHandler;
        if (!(handler instanceof HopperScreenHandler)) {
            return false;
        }
        var registry = BlackOut.mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var sharpnessEntry = registry.getEntry(Enchantments.SHARPNESS);
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack.getItem() instanceof SwordItem) {
                int sharpnessLevel = sharpnessEntry.map(entry -> EnchantmentHelper.getLevel(entry, stack)).orElse(0);
                if (sharpnessLevel >= 10) {
                    int s = this.getSlot();

                    if (s != BlackOut.mc.player.getInventory().selectedSlot) {
                        InvUtils.swap(s);
                    }
                    BlackOut.mc.interactionManager.clickSlot(handler.syncId, slot.id, s, SlotActionType.SWAP, BlackOut.mc.player);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean placeShulker(boolean place) {
        BlockPos pos = this.hopperPos.up();
        if (BlackOut.mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
            return true;
        } else {
            Hand hand = OLEPOSSUtils.getHand(OLEPOSSUtils::isShulker);
            FindResult findResult = null;
            if (hand == null && !(findResult = this.switchMode.get().find(OLEPOSSUtils::isShulker)).wasFound()) {
                return false;
            } else {
                PlaceData data = SettingUtils.getPlaceData(pos, false);
                if (!data.valid()) {
                    return false;
                } else if (!SettingUtils.inInteractRange(data.pos())) {
                    return false;
                } else if (SettingUtils.shouldRotate(RotationType.BlockPlace) && !this.rotateBlock(data, RotationType.BlockPlace, "placing")) {
                    return false;
                } else if (place && !this.placed) {
                    if (hand == null && !this.switchMode.get().swap(findResult.slot())) {
                        return false;
                    } else {
                        this.placeBlock(hand, data);
                        BlockState state = ((hand != null ? Managers.PACKET.handStack(hand) : findResult.stack()).getItem() instanceof BlockItem blockitem
                                ? blockitem.getBlock()
                                : Blocks.SHULKER_BOX)
                                .getDefaultState();
                        BlackOut.mc.world.setBlockState(pos, state);
                        this.placed = true;
                        if (hand == null) {
                            this.switchMode.get().swapBack();
                        }

                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
    }

    private void place(Block block, BlockPos pos, boolean place) {
        if (pos != null) {
            if (block.asItem() instanceof BlockItem blockItem) {
                if (BlackOut.mc.world.getBlockState(pos).getBlock() != block) {
                    Hand hand = OLEPOSSUtils.getHand(blockItem);
                    FindResult findResult = null;
                    if (hand != null || (findResult = this.switchMode.get().find(blockItem)).wasFound()) {
                        PlaceData data = SettingUtils.getPlaceData(pos, false);
                        if (data.valid()) {
                            if (SettingUtils.inPlaceRange(data.pos())) {
                                if (!SettingUtils.shouldRotate(RotationType.BlockPlace) || this.rotateBlock(data, RotationType.BlockPlace, "placing")) {
                                    if (place && !this.placed) {
                                        if (block == Blocks.DISPENSER) {
                                            switch (this.rotationMode.get()) {
                                                case Instant:
                                                    if (!this.rotate(this.dispenserDir.asRotation(), 0.0F, 0.0, 45.0, RotationType.InstantOther, "facing")) {
                                                        return;
                                                    }
                                                    break;
                                                case Normal:
                                                    if (!this.rotate(this.dispenserDir.asRotation(), 0.0F, 0.0, 45.0, RotationType.Other, "facing")) {
                                                        return;
                                                    }
                                            }
                                        }

                                        if (hand != null || this.switchMode.get().swap(findResult.slot())) {
                                            if (this.rotationMode.get() == ObsidianModule.RotationMode.Packet) {
                                                this.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(this.dispenserDir.asRotation(), 0.0F, Managers.PACKET.isOnGround()));
                                            }

                                            this.placeBlock(hand, data);
                                            BlockState state = block.getDefaultState();
                                            if (block == Blocks.DISPENSER) {
                                                state = state.with(
                                                        DispenserBlock.FACING, Direction.fromRotation(Managers.ROTATION.prevYaw).getOpposite()
                                                );
                                            }

                                            BlackOut.mc.world.setBlockState(pos, state);
                                            this.placed = true;
                                            if (hand == null) {
                                                this.switchMode.get().swapBack();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updatePos(BlockPos hopper) {
        switch (this.mode.get()) {
            case Hopper:
                this.updateHopper(hopper);
                break;
            case Dispenser:
                this.updateDispenser(hopper);
        }
    }

    private void updateHopper(BlockPos hopper) {
        double value = this.getValue(hopper);
        if (!(value < this.calcValue)) {
            BlockState state = this.get(hopper);
            if (state.getBlock() != Blocks.HOPPER) {
                if (!OLEPOSSUtils.replaceable(hopper)) {
                    return;
                }

                PlaceData data = SettingUtils.getPlaceData(hopper);
                if (!data.valid()) {
                    return;
                }

                if (!SettingUtils.inPlaceRange(data.pos())) {
                    return;
                }
            }

            if (SettingUtils.inInteractRange(hopper) && SettingUtils.getPlaceOnDirection(hopper) != null) {
                if (OLEPOSSUtils.replaceable(hopper.up()) || this.get(hopper.up()).getBlock() instanceof ShulkerBoxBlock) {
                    this.calcHopperPos = hopper;
                    this.calcValid = true;
                    this.calcValue = value;
                }
            }
        }
    }

    private void updateDispenser(BlockPos hopper) {
        double value = this.getValue(hopper);
        if (!(value < this.calcValue)) {
            BlockState state = this.get(hopper);
            if (state.getBlock() != Blocks.HOPPER) {
                if (!OLEPOSSUtils.replaceable(hopper)) {
                    return;
                }

                PlaceData data = SettingUtils.getPlaceData(hopper);
                if (!data.valid()) {
                    return;
                }

                if (!SettingUtils.inPlaceRange(data.pos())) {
                    return;
                }
            }

            if (SettingUtils.inInteractRange(hopper) && SettingUtils.getPlaceOnDirection(hopper) != null) {
                if (OLEPOSSUtils.replaceable(hopper.up()) || this.get(hopper.up()).getBlock() instanceof ShulkerBoxBlock) {
                    for (Direction dir : Direction.Type.HORIZONTAL) {
                        BlockPos pos = hopper.offset(dir).up();
                        if (this.validDispenser(pos, dir)) {
                            for (Direction direction : Direction.values()) {
                                BlockPos pos2 = pos.offset(direction);
                                if (this.get(pos2).getBlock() == Blocks.REDSTONE_BLOCK && this.get(pos).getBlock() != Blocks.DISPENSER) {
                                    break;
                                }

                                if (OLEPOSSUtils.replaceable(pos2) && direction != Direction.DOWN && direction != dir.getOpposite()) {
                                    PlaceData datax = SettingUtils.getPlaceData(pos2, false);
                                    if (datax.valid() && SettingUtils.inPlaceRange(datax.pos())) {
                                        this.calcDispenserDir = dir;
                                        this.calcHopperPos = hopper;
                                        this.calcSupportPos = hopper.offset(dir);
                                        this.calcDispenserPos = this.calcSupportPos.up();
                                        this.calcRedstonePos = this.calcDispenserPos.offset(direction);
                                        this.calcValid = true;
                                        this.calcValue = value;
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean directionCheck(BlockPos pos, Direction direction) {
        Vec3d center = pos.toCenterPos();
        if (this.serverDir.get()) {
            double yaw = RotationUtils.getYaw(center);
            if (Math.abs(RotationUtils.yawAngle(direction.asRotation(), yaw)) > 40.0) {
                return false;
            }
        }

        if (this.yCheck.get()) {
            double pitch = RotationUtils.getPitch(center);
            return Math.abs(pitch) < 45.0;
        } else {
            return true;
        }
    }

    private double getValue(BlockPos pos) {
        double value = 0.0;

        for (PlayerEntity player : BlackOut.mc.world.getPlayers()) {
            double distance = player.squaredDistanceTo(pos.toCenterPos());
            if (distance < 100.0) {
                value += distance;
            }
        }

        return value;
    }

    private boolean validDispenser(BlockPos pos, Direction direction) {
        BlockState state = BlackOut.mc.world.getBlockState(pos);
        if (SettingUtils.getPlaceOnDirection(pos) == null) {
            return false;
        } else if (state.getBlock() == Blocks.DISPENSER) {
            return state.get(DispenserBlock.FACING) == direction.getOpposite();
        } else if (!OLEPOSSUtils.replaceable(pos)) {
            return false;
        } else if (!this.directionCheck(pos, direction)) {
            return false;
        } else {
            PlaceData data = SettingUtils.getPlaceData(pos, (p, d) -> d == Direction.DOWN, null);
            return data.valid() && SettingUtils.inPlaceRange(data.pos());
        }
    }

    private BlockState get(BlockPos pos) {
        return BlackOut.mc.world.getBlockState(pos);
    }

    public enum Mode {
        Hopper,
        Dispenser
    }
}
