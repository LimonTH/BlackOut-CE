package bodevelopment.client.blackout.gui.clickgui.screens;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.manager.managers.FriendsManager;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.util.BOLogger;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FriendsScreen extends ClickGuiScreen {
    private static final float ITEM_HEIGHT = 75.0F;
    private boolean first;
    private final Map<String, Identifier> skinCache = new HashMap<>();

    public FriendsScreen() {
        super("Friends", 800.0F, 500.0F, true);
    }

    @Override
    protected float getLength() {
        return Managers.FRIENDS.getFriends().size() * ITEM_HEIGHT + 40.0F;
    }

    @Override
    public void render() {
        RenderUtils.rounded(this.stack, 0, 0, width, height - 40.0F, 10, 10, GuiColorUtils.bg1.getRGB(), ColorUtils.SHADOW100I);

        this.stack.push();

        this.stack.translate(0.0F, (float) Math.round(10.0F - this.scroll.get()), 0.0F);

        this.first = true;
        if (Managers.FRIENDS.getFriends().isEmpty()) {
            BlackOut.FONT.text(this.stack, "No friends added yet", 2.0F, width / 2.0F, 30.0F, Color.GRAY, true, true);
        } else {
            Managers.FRIENDS.getFriends().forEach(this::renderFriend);
        }

        this.stack.pop();
    }

    private void renderFriend(FriendsManager.Friend friend) {
        if (!this.first) {
            RenderUtils.line(this.stack, 20.0F, 0.0F, this.width - 20.0F, 0.0F, new Color(255, 255, 255, 12).getRGB());
        }
        this.first = false;

        this.stack.push();

        renderFriendHead(friend);

        BlackOut.FONT.text(this.stack, friend.getName(), 2.0F, 85.0F, 22.0F, Color.WHITE, false, true);
        String subText = (friend.getUuid() != null) ? friend.getUuid().toString().substring(0, 18) + "..." : "Added manually";
        BlackOut.FONT.text(this.stack, subText, 1.3F, 85.0F, 45.0F, new Color(140, 140, 140), false, true);

        int index = Managers.FRIENDS.getFriends().indexOf(friend);
        float itemTopY = (ITEM_HEIGHT * index) + 10.0F - scroll.get();

        if (mx > width - 110 && mx < width - 10 && my > itemTopY && my < itemTopY + ITEM_HEIGHT) {
            RenderUtils.rounded(this.stack, width - 100, 20, 85, 35, 7, 0, new Color(255, 50, 50, 35).getRGB(), 0);
            BlackOut.FONT.text(this.stack, "REMOVE", 1.5F, width - 57.0F, 37.0F, Color.RED, true, true);
        }
        this.stack.pop();

        this.stack.translate(0.0F, ITEM_HEIGHT, 0.0F);
    }

    private void renderFriendHead(FriendsManager.Friend friend) {
        Identifier skin = skinCache.get(friend.getName());

        if (skin == null) {
            if (friend.getUuid() != null) {
                new Thread(() -> {
                    try {
                        var profileResult = BlackOut.mc.getSessionService().fetchProfile(friend.getUuid(), false);

                        if (profileResult != null) {
                            GameProfile fullProfile = profileResult.profile();

                            var skinTextures = BlackOut.mc.getSkinProvider().getSkinTextures(fullProfile);
                            Identifier loadedSkin = skinTextures.texture();

                            if (!loadedSkin.getPath().contains("textures/entity/player/")) {
                                skinCache.put(friend.getName(), loadedSkin);
                            }
                        }
                    } catch (Exception e) {
                        BOLogger.error(e);
                    }
                }).start();

                skin = DefaultSkinHelper.getSkinTextures(friend.getUuid()).texture();
            } else {
                skin = DefaultSkinHelper.getTexture();
            }
        }

        int glId = BlackOut.mc.getTextureManager().getTexture(skin).getGlId();

        RenderSystem.setShaderTexture(0, skin);
        RenderSystem.texParameter(3553, 10241, 9728);
        RenderSystem.texParameter(3553, 10240, 9728);

        float x = 18.0f;
        float y = 15.0f;
        float size = 44.0F;
        float radius = 10.0F;

        TextureRenderer.renderFitRounded(this.stack, x, y, size, size, 0.125F, 0.125F, 0.250F, 0.250F, radius, 16, glId);
        TextureRenderer.renderFitRounded(this.stack, x, y, size, size, 0.625F, 0.125F, 0.750F, 0.250F, radius, 16, glId);
    }

    @Override
    public void onMouse(int button, boolean state) {
        if (state && button == 0) {
            if (my > 0 && my < height - 40) {
                float startY = 10.0F - this.scroll.get();
                for (FriendsManager.Friend friend : Managers.FRIENDS.getFriends()) {
                    if (mx > width - 110 && mx < width - 10 && my > startY && my < startY + ITEM_HEIGHT) {
                        Managers.FRIENDS.remove(friend.getName());
                        return;
                    }
                    startY += ITEM_HEIGHT;
                }
            }
        }
    }
}