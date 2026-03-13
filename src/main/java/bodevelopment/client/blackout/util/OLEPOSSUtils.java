package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class OLEPOSSUtils {
    public static final EquipmentSlot[] equipmentSlots = new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static BlockPos roundedPos() {
        return roundedPos(BlackOut.mc.player);
    }

    public static BlockPos roundedPos(Entity entity) {
        return new BlockPos(entity.getBlockX(), (int) Math.round(entity.getY()), entity.getBlockZ());
    }

    public static long testTime(Runnable runnable) {
        long start = System.currentTimeMillis();
        runnable.run();
        long end = System.currentTimeMillis();
        return end - start;
    }

    public static double similarity(String string1, String string2) {
        String shorter;
        String longer;
        if (string1.length() > string2.length()) {
            shorter = string2.toLowerCase();
            longer = string1.toLowerCase();
        } else {
            shorter = string1.toLowerCase();
            longer = string2.toLowerCase();
        }

        int i = 0;
        int a = 0;

        for (int c = 0; c < longer.length(); c++) {
            char charLonger = longer.charAt(c);

            for (int ci = i; ci < shorter.length(); ci++) {
                char charShorter = shorter.charAt(ci);
                if (charLonger == charShorter) {
                    a++;
                    i = ci;
                    break;
                }
            }
        }

        return (float) a / longer.length();
    }

    public static boolean inWater(AABB box) {
        return inFluid(box, FluidTags.WATER);
    }

    public static boolean inLava(AABB box) {
        return inFluid(box, FluidTags.LAVA);
    }

    public static boolean inFluid(AABB box, TagKey<Fluid> tag) {
        int minX = Mth.floor(box.minX + 0.001);
        int maxX = Mth.ceil(box.maxX - 0.001);
        int minY = Mth.floor(box.minY + 0.001);
        int maxY = Mth.ceil(box.maxY - 0.001);
        int minZ = Mth.floor(box.minZ + 0.001);
        int maxZ = Mth.ceil(box.maxZ - 0.001);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    FluidState fluidState = BlackOut.mc.level.getFluidState(new BlockPos(x, y, z));
                    if (fluidState.is(tag) && y + fluidState.getOwnHeight() > minY) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static double fluidHeight(AABB box, TagKey<Fluid> tag) {
        int minX = Mth.floor(box.minX + 0.001);
        int maxX = Mth.ceil(box.maxX - 0.001);
        int minY = Mth.floor(box.minY + 0.001);
        int maxY = Mth.ceil(box.maxY - 0.001);
        int minZ = Mth.floor(box.minZ + 0.001);
        int maxZ = Mth.ceil(box.maxZ - 0.001);
        double maxHeight = 0.0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    FluidState fluidState = BlackOut.mc.level.getFluidState(new BlockPos(x, y, z));
                    if (fluidState.is(tag)) {
                        maxHeight = Math.max(maxHeight, y + fluidState.getOwnHeight() - box.minY);
                    }
                }
            }
        }

        return maxHeight;
    }

    public static double approach(double from, double to, double delta) {
        return to > from ? Math.min(from + delta, to) : Math.max(from - delta, to);
    }

    public static Vec3 getLerpedPos(Entity entity, double tickDelta) {
        double x = Mth.lerp(tickDelta, entity.xo, entity.getX());
        double y = Mth.lerp(tickDelta, entity.yo, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zo, entity.getZ());
        return new Vec3(x, y, z);
    }

    public static AABB getLerpedBox(Entity entity, double tickDelta) {
        double x = Mth.lerp(tickDelta, entity.xo, entity.getX());
        double y = Mth.lerp(tickDelta, entity.yo, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zo, entity.getZ());
        double halfX = entity.getBoundingBox().getXsize() / 2.0;
        double halfZ = entity.getBoundingBox().getZsize() / 2.0;
        return new AABB(x - halfX, y, z - halfZ, x + halfX, y + entity.getBoundingBox().getYsize(), z + halfZ);
    }

    public static int secondsSince(LocalDateTime dateTime) {
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);
        return (int) java.time.Duration.between(dateTime, currentTime).getSeconds();
    }

    private static int daysInYear(int year) {
        return Year.of(year).length();
    }

    public static String getDateTimeString(int seconds) {
        int minutes = (int) (seconds / 60.0F);
        seconds -= minutes * 60;
        int hours = (int) (minutes / 60.0F);
        minutes -= hours * 60;
        int days = (int) (hours / 24.0F);
        hours -= days * 24;
        int months = (int) (days / 30.0F);
        days -= months * 30;
        int years = (int) (months / 12.0F);
        months -= years * 12;
        if (years > 0) {
            return years + "y " + months + "mo";
        } else if (months > 0) {
            return months + "mo " + days + "d";
        } else if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + " s";
        }
    }

    public static String getTimeString(long ms) {
        int hours = (int) (ms / 3600000.0 % 60.0);
        int minutes = (int) (ms / 60000.0 % 60.0);
        int seconds = (int) (ms / 1000.0 % 60.0);
        StringBuilder result = new StringBuilder();
        if (hours > 0) {
            result.append(hours).append("h ");
        }

        if (minutes > 0) {
            result.append(minutes).append("m ");
        }

        if (seconds > 0 || hours == 0 && minutes == 0) {
            result.append(seconds).append("s");
        }

        return result.toString();
    }

    public static double safeDivide(double v1, double v2) {
        double v3 = v1 / v2;
        return Double.isNaN(v3) ? 1.0 : v3;
    }

    public static <T> List<T> reverse(List<T> list) {
        List<T> l = new ArrayList<>();
        list.forEach(t -> l.addFirst(t));
        return l;
    }

    public static <T> void limitList(List<T> list, int cap) {
        if (list.size() > cap) {
            list.subList(cap, list.size()).clear();
        }
    }

    public static <T> boolean contains(T[] array, T object) {
        for (T t : array) {
            if (t.equals(object)) {
                return true;
            }
        }

        return false;
    }

    public static InteractionHand getHand(Item item) {
        return getHand(stack -> stack.getItem() == item);
    }

    public static InteractionHand getHand(Predicate<ItemStack> predicate) {
        if (predicate.test(Managers.PACKET.getStack())) {
            return InteractionHand.MAIN_HAND;
        } else {
            return predicate.test(BlackOut.mc.player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
        }
    }

    public static ItemStack getItem(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? Managers.PACKET.getStack() : (hand == InteractionHand.OFF_HAND ? BlackOut.mc.player.getOffhandItem() : null);
    }

    public static Vec3 getMiddle(AABB box) {
        return new Vec3((box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0);
    }

    public static boolean inside(Entity en, AABB bb) {
        if (BlackOut.mc.level == null) return false;
        return BlackOut.mc.level.getBlockCollisions(en, bb).iterator().hasNext();
    }

    public static int closerToZero(int x) {
        return (int) (x - Math.signum((float) x));
    }

    public static Vec3 getClosest(Vec3 playerPos, Vec3 feet, double width, double height) {
        double halfWidth = width / 2.0;
        return getClosest(
                playerPos,
                feet.x() - halfWidth,
                feet.x() + halfWidth,
                feet.y(),
                feet.y() + height,
                feet.z() - halfWidth,
                feet.z() + halfWidth
        );
    }

    public static Vec3 getClosest(Vec3 playerPos, AABB box) {
        return getClosest(playerPos, box.minX, box.maxX, box.minY, box.maxY, box.minZ, box.maxZ);
    }

    public static Vec3 getClosest(Vec3 playerPos, double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        return new Vec3(
                Mth.clamp(playerPos.x(), minX, maxX),
                Mth.clamp(playerPos.y(), minY, maxY),
                Mth.clamp(playerPos.z(), minZ, maxZ)
        );
    }

    public static boolean strictDir(BlockPos pos, Direction dir, boolean ncp) {
        return switch (dir) {
            case DOWN -> BlackOut.mc.player.getEyePosition().y <= pos.getY() + (ncp ? 0.5 : 0.0);
            case UP -> BlackOut.mc.player.getEyePosition().y >= pos.getY() + (ncp ? 0.5 : 1.0);
            case NORTH -> BlackOut.mc.player.getZ() < pos.getZ();
            case SOUTH -> BlackOut.mc.player.getZ() >= pos.getZ() + 1;
            case WEST -> BlackOut.mc.player.getX() < pos.getX();
            case EAST -> BlackOut.mc.player.getX() >= pos.getX() + 1;
            default -> throw new IncompatibleClassChangeError();
        };
    }

    public static AABB getCrystalBox(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1);
    }

    public static AABB getCrystalBox(Vec3 pos) {
        return new AABB(
                pos.x() - 1.0, pos.y(), pos.z() - 1.0, pos.x() + 1.0, pos.y() + 2.0, pos.z() + 1.0
        );
    }


    public static boolean replaceable(BlockPos pos) {
        if (BlackOut.mc.level == null) return false;
        return replaceable(BlackOut.mc.level.getBlockState(pos));
    }

    public static boolean replaceable(BlockState state) {
        return state.canBeReplaced();
    }


    // Основной метод с серьёзной проверкой на солидность
    public static boolean solid2(BlockPos block) {
        if (BlackOut.mc.level == null) return false;
        BlockState state = BlackOut.mc.level.getBlockState(block);

        return !state.getCollisionShape(BlackOut.mc.level, block,
                BlackOut.mc.player != null ? CollisionContext.of(BlackOut.mc.player) : CollisionContext.empty()
        ).isEmpty();
    }

    // Доп метод для проверки установки факелов и прочего
    public static boolean solid(BlockPos block) {
        if (BlackOut.mc.level == null) return false;
        BlockState state = BlackOut.mc.level.getBlockState(block);

        return state.isFaceSturdy(BlackOut.mc.level, block, Direction.UP);
    }

    public static boolean isGapple(Item item) {
        return item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE;
    }

    public static boolean isShulker(ItemStack stack) {
        return isShulker(stack.getItem());
    }

    public static boolean isShulker(Item item) {
        return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public static boolean isGapple(ItemStack stack) {
        return isGapple(stack.getItem());
    }

    public static boolean isBed(Item item) {
        return item instanceof BedItem;
    }

    public static boolean isBed(ItemStack stack) {
        return isBed(stack.getItem());
    }

    public static boolean collidable(BlockPos block) {
        if (BlackOut.mc.level == null) return false;
        BlockState state = BlackOut.mc.level.getBlockState(block);
        return !state.getCollisionShape(BlackOut.mc.level, block).isEmpty();
    }

    public static int getEnchantmentLevel(net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantmentKey, ItemStack stack) {
        if (BlackOut.mc.level == null) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(
                BlackOut.mc.level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(enchantmentKey),
                stack
        );
    }

    // Проверка качества блока (Safe vs Unsafe)
    public static boolean isSafe(BlockPos pos) {
        if (BlackOut.mc.level == null) return false;
        float resistance = BlackOut.mc.level.getBlockState(pos).getBlock().getExplosionResistance();
        return resistance >= 600.0f;
    }

    public static int getEquipmentEnchantmentLevel(net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantmentKey, net.minecraft.world.entity.LivingEntity entity) {
        if (BlackOut.mc.level == null) return 0;
        return EnchantmentHelper.getEnchantmentLevel(
                BlackOut.mc.level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(enchantmentKey),
                entity
        );
    }
}
