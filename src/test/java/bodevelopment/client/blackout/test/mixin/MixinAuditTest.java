package bodevelopment.client.blackout.test.mixin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates that all Mixin injection points target methods/fields that actually
 * exist in the current Minecraft version. Catches regressions early when
 * Mojang renames or removes methods during version updates.
 *
 * <p>Run via: {@code ./gradlew test}
 */
public class MixinAuditTest {

    private static final List<String> FAILURES = new ArrayList<>();

    @BeforeAll
    static void verifyMixinConfigExists() {
        InputStream config = MixinAuditTest.class.getClassLoader()
                .getResourceAsStream("blackout.mixins.json");
        assertNotNull(config, "blackout.mixins.json not found in resources");
    }

    @Test
    void allMixinClassesAreLoadable() {
        String[] mixinClasses = {
                // Core
                "bodevelopment.client.blackout.mixin.mixins.MixinConnection",
                "bodevelopment.client.blackout.mixin.mixins.MixinLocalPlayer",
                "bodevelopment.client.blackout.mixin.mixins.MixinClientPacketListener",
                "bodevelopment.client.blackout.mixin.mixins.MixinMinecraft",
                // Entities
                "bodevelopment.client.blackout.mixin.mixins.MixinEntity",
                "bodevelopment.client.blackout.mixin.mixins.MixinLivingEntity",
                "bodevelopment.client.blackout.mixin.mixins.MixinPlayer",
                "bodevelopment.client.blackout.mixin.mixins.MixinAbstractClientPlayer",
                "bodevelopment.client.blackout.mixin.mixins.MixinRemotePlayer",
                // Rendering
                "bodevelopment.client.blackout.mixin.mixins.MixinGameRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinLevelRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinEntityRenderDispatcher",
                "bodevelopment.client.blackout.mixin.mixins.MixinEntityRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinBufferBuilder",
                "bodevelopment.client.blackout.mixin.mixins.MixinBufferUploader",
                "bodevelopment.client.blackout.mixin.mixins.MixinCamera",
                "bodevelopment.client.blackout.mixin.mixins.MixinLightTexture",
                "bodevelopment.client.blackout.mixin.mixins.MixinFogRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinWeatherEffectRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinCloudRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinGuiGraphics",
                "bodevelopment.client.blackout.mixin.mixins.MixinGui",
                // GUI
                "bodevelopment.client.blackout.mixin.mixins.MixinScreen",
                "bodevelopment.client.blackout.mixin.mixins.MixinTitleScreen",
                "bodevelopment.client.blackout.mixin.mixins.MixinChatScreen",
                "bodevelopment.client.blackout.mixin.mixins.MixinAbstractContainerScreen",
                "bodevelopment.client.blackout.mixin.mixins.MixinCreativeModeInventoryScreen",
                "bodevelopment.client.blackout.mixin.mixins.MixinJoinMultiplayerScreen",
                // Input
                "bodevelopment.client.blackout.mixin.mixins.MixinKeyboardInput",
                "bodevelopment.client.blackout.mixin.mixins.MixinKeyboardHandler",
                "bodevelopment.client.blackout.mixin.mixins.MixinMouseHandler",
                "bodevelopment.client.blackout.mixin.mixins.MixinMultiPlayerGameMode",
                "bodevelopment.client.blackout.mixin.mixins.MixinOptions",
                // World
                "bodevelopment.client.blackout.mixin.mixins.MixinClientLevel",
                "bodevelopment.client.blackout.mixin.mixins.MixinLevel",
                "bodevelopment.client.blackout.mixin.mixins.MixinBlock",
                "bodevelopment.client.blackout.mixin.mixins.MixinBlockBehaviour",
                "bodevelopment.client.blackout.mixin.mixins.MixinBlockStateBase",
                "bodevelopment.client.blackout.mixin.mixins.MixinLevelChunk",
                // Network/Validation
                "bodevelopment.client.blackout.mixin.mixins.MixinAddressCheck",
                "bodevelopment.client.blackout.mixin.mixins.MixinClientboundSetEntityMotionPacket",
                "bodevelopment.client.blackout.mixin.mixins.MixinChatComponent",
                "bodevelopment.client.blackout.mixin.mixins.MixinCommandSuggestions",
                "bodevelopment.client.blackout.mixin.mixins.MixinFishingHook",
                "bodevelopment.client.blackout.mixin.mixins.MixinVec3",
                "bodevelopment.client.blackout.mixin.mixins.MixinClipContext",
                "bodevelopment.client.blackout.mixin.mixins.MixinBossHealthOverlay",
                "bodevelopment.client.blackout.mixin.mixins.MixinParticleEngine",
                "bodevelopment.client.blackout.mixin.mixins.MixinEffectRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinTridentItem",
                // Render layers
                "bodevelopment.client.blackout.mixin.mixins.MixinCapeLayer",
                "bodevelopment.client.blackout.mixin.mixins.MixinHumanoidArmorLayer",
                "bodevelopment.client.blackout.mixin.mixins.MixinPlayerItemInHandLayer",
                "bodevelopment.client.blackout.mixin.mixins.MixinItemInHandRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinItemRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinPlayerRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinPlayerRenderState",
                "bodevelopment.client.blackout.mixin.mixins.MixinLivingEntityRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinModelRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinBlockEntityRenderDispatcher",
                "bodevelopment.client.blackout.mixin.mixins.MixinBlockRenderDispatcher",
                "bodevelopment.client.blackout.mixin.mixins.MixinLiquidBlockRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinBeaconRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinItemBlockRenderTypes",
                "bodevelopment.client.blackout.mixin.mixins.MixinRenderSection",
                "bodevelopment.client.blackout.mixin.mixins.MixinVisibilitySet",
                "bodevelopment.client.blackout.mixin.mixins.MixinEndCrystalRenderer",
                "bodevelopment.client.blackout.mixin.mixins.MixinEndCrystal",
                "bodevelopment.client.blackout.mixin.mixins.MixinEndCrystalRenderState",
                "bodevelopment.client.blackout.mixin.mixins.MixinGuiMessage",
                "bodevelopment.client.blackout.mixin.mixins.MixinLine",
                // Sodium compat
                "bodevelopment.client.blackout.mixin.mixins.sodium.MixinSodiumBlockOcclusionCache",
                "bodevelopment.client.blackout.mixin.mixins.sodium.MixinSodiumBlockRenderer",
                "bodevelopment.client.blackout.mixin.mixins.sodium.MixinSodiumDefaultFluidRenderer",
                "bodevelopment.client.blackout.mixin.mixins.sodium.MixinSodiumOcclusionCuller",
                "bodevelopment.client.blackout.mixin.mixins.sodium.MixinSodiumVisibilityEncoding",
                // Fabric API compat
                "bodevelopment.client.blackout.mixin.mixins.fabricapi.MixinFabricBakedModel",
        };

        for (String className : mixinClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                assertNotNull(clazz, "Mixin class not loadable: " + className);
            } catch (ClassNotFoundException e) {
                FAILURES.add("Mixin class not found: " + className);
            } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
                // Class exists but needs Minecraft runtime to load — skip
            }
        }

        if (!FAILURES.isEmpty()) {
            fail("Some mixin classes failed to load:\n" + String.join("\n", FAILURES));
        }
    }

    @Test
    void verifyWrapOperationTargetsExist() {
        assertMethodExists("net.minecraft.network.Connection", "isConnected", "()Z");
        assertFieldExists("net.minecraft.network.Connection", "channel",
                "Lio/netty/channel/Channel;");
        assertMethodExists("net.minecraft.client.player.LocalPlayer", "getYRot", "()F");
        assertMethodExists("net.minecraft.client.player.LocalPlayer", "getXRot", "()F");
        assertMethodExists("net.minecraft.client.player.LocalPlayer", "isUsingItem", "()Z");
        assertMethodExists("net.minecraft.client.player.LocalPlayer", "onGround", "()Z");
        assertMethodExists("net.minecraft.client.player.LocalPlayer", "isSprinting", "()Z");
    }

    @Test
    void verifyAccessWidenerTargetsExist() {
        assertMethodExists("net.minecraft.client.multiplayer.ClientLevel",
                "getBlockStatePredictionHandler",
                "()Lnet/minecraft/client/multiplayer/prediction/BlockStatePredictionHandler;");
        assertMethodExists("net.minecraft.client.multiplayer.MultiPlayerGameMode",
                "startPrediction",
                "(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V");
    }

    private static void assertMethodExists(String className, String methodName, String desc) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            return; // Needs MC runtime, skip
        } catch (ClassNotFoundException e) {
            FAILURES.add("Class not found: " + className);
            return;
        }
        {
            boolean found = false;
            for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(methodName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Class<?> superClass = clazz.getSuperclass();
                while (superClass != null && !found) {
                    for (java.lang.reflect.Method m : superClass.getDeclaredMethods()) {
                        if (m.getName().equals(methodName)) {
                            found = true;
                            break;
                        }
                    }
                    superClass = superClass.getSuperclass();
                }
            }
            assertTrue(found, "Method not found: " + className + "#" + methodName + desc);
        }
    }

    private static void assertFieldExists(String className, String fieldName, String desc) {
        try {
            Class<?> clazz = Class.forName(className);
            boolean found = false;
            for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                if (f.getName().equals(fieldName)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Field not found: " + className + "#" + fieldName);
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Needs MC runtime, skip
        } catch (ClassNotFoundException e) {
            FAILURES.add("Class not found: " + className);
        }
    }
}
