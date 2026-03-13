package bodevelopment.client.blackout.randomstuff.mainmenu;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;

public class AltHelpRenderer {
    private final List<String> helpLines = new ArrayList<>();
    private float helpX = -900.0F; // Начальная позиция слева
    private float helpY = -300.0F;
    private float targetX = -900.0F;
    private float targetY = -300.0F;
    private boolean dragging = false;
    private float dragOffsetX, dragOffsetY;
    private float width = 380.0F;

    public void init() {
        helpLines.clear();
        helpLines.add("Controls:");
        helpLines.add("LMB - Select / Login");
        helpLines.add("RMB - Refresh (script)");
        helpLines.add("Wheel - Delete account");
        helpLines.add("");
        helpLines.add("Script Tags (use {}, [], <>, etc):");
        helpLines.add("ra / rndAdj - Random Adjective");
        helpLines.add("rs / rndSub - Random Noun");
        helpLines.add("n1 - n5 - Random Numbers");
        helpLines.add("");
        helpLines.add("Example: {ra}_{rs}_{n3}");
        helpLines.add("");
        helpLines.add("Account Info:");
        helpLines.add("Green Border - Currently Active");
        helpLines.add("Microsoft / Mojang - Premium");
        helpLines.add("Cracked - Non-premium (Offline)");
        helpLines.add("Script - You Script (Offline)");
    }

    public void render(PoseStack stack, float mx, float my) {
        if (helpLines.isEmpty()) init();

        float fontScale = 1.4F;
        float fontHeight = BlackOut.FONT.getHeight() * fontScale;

        float height = 65.0F + (helpLines.size() * fontHeight) + 20.0F;

        boolean mousePressed = GLFW.glfwGetMouseButton(BlackOut.mc.getWindow().getWindow(), 0) == 1;
        if (this.dragging) {
            if (mousePressed) {
                this.targetX = mx - this.dragOffsetX;
                this.targetY = my - this.dragOffsetY;
            } else {
                this.dragging = false;
            }
        } else if (mousePressed) {
            if (mx >= this.helpX && mx <= this.helpX + width && my >= this.helpY && my <= this.helpY + 40.0F) {
                this.dragging = true;
                this.dragOffsetX = mx - this.helpX;
                this.dragOffsetY = my - this.helpY;
            }
        }

        this.helpX = Mth.lerp(0.2F, this.helpX, this.targetX);
        this.helpY = Mth.lerp(0.2F, this.helpY, this.targetY);

        stack.pushPose();
        stack.translate(this.helpX, this.helpY, 0.0F);

        RenderUtils.roundedShadow(stack, 0.0F, 0.0F, width, height, 15.0F, 15.0F, new Color(0, 0, 0, 120).getRGB());
        RenderUtils.drawLoadedBlur("title", stack, renderer -> renderer.rounded(0.0F, 0.0F, width, height, 15.0F, 10, 1.0F, 1.0F, 1.0F, 1.0F));
        RenderUtils.rounded(stack, 0.0F, 0.0F, width, height, 15.0F, 2.0F, new Color(20, 20, 20, 160).getRGB(), new Color(10, 10, 10, 225).getRGB());

        BlackOut.BOLD_FONT.text(stack, "Quick Guide", 2.0F, width / 2.0F, 12.0F, Color.WHITE.getRGB(), true, false);

        RenderUtils.rounded(stack, 15.0F, 38.0F, width - 30.0F, 1.5F, 1.0F, 0.0F, new Color(255, 255, 255, 50).getRGB(), 0);

        for (int i = 0; i < helpLines.size(); i++) {
            String line = helpLines.get(i);
            if (line.isEmpty()) continue;

            int color = line.contains(":") ? new Color(230, 230, 230).getRGB() : new Color(160, 160, 160).getRGB();

            if (line.endsWith(":")) color = Color.WHITE.getRGB();

            BlackOut.FONT.text(stack, line, fontScale, 20.0F, 52.0F + (i * fontHeight), color, false, false);
        }

        stack.popPose();
    }
}