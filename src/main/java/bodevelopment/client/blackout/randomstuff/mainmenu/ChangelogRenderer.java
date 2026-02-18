package bodevelopment.client.blackout.randomstuff.mainmenu;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;

public class ChangelogRenderer {
    private final ArrayList<String> changelog = new ArrayList<>();
    private float longest = 0.0F;
    private float changelogX = 450.0F;
    private float changelogY = -200.0F;
    private float targetX = 450.0F;
    private float targetY = -200.0F;
    private boolean dragging = false;
    private float dragOffsetX, dragOffsetY;

    public void initChangelog() {
        changelog.clear();
        changelog.add("I kill old changelog, Alas! =)");
        changelog.add("Initial restore from deobfuscation");
        changelog.add("Fix crashes with mixins and other uncorrected functions");
        changelog.add("Fix UI rendering issues: scissors, textures, ...");
        changelog.add("Swapped keyboard layout from Filipino to English");
        changelog.add("Fix binding issues");
        changelog.add("ClickGUI now can opened in Main Menu");
        changelog.add("Modules cleanup");
        changelog.add("Add TAB-autocomplete with -commands");
        changelog.add("FakePlayer fixes, record now worked correctly");
        changelog.add("Update 3D rendering for client");
        changelog.add("Resolve system synchronizations(linux, windows, macOS)");
        changelog.add("Updated to Minecraft 1.21.1");
        changelog.add("Global rendering overhaul");
        changelog.add("Improved world-logic communication");
        changelog.add("Update changelog render logic");
        changelog.add("Fixed ClickGUI issues in main menu");
        changelog.add("Fixed utils logic for better performance");
        changelog.add("That's ALL, for now =)");

        float maxW = 0;
        for (String s : changelog) {
            float w = BlackOut.FONT.getWidth(s) * 1.5F;
            if (w > maxW) maxW = w;
        }
        longest = maxW + 60.0F;
    }

    public void render(MatrixStack stack, float mx, float my, boolean themeMode, Color mainColor, Color secondColor, float speed) {
        if (changelog.isEmpty()) {
            initChangelog();
        }

        float fontHeight = BlackOut.FONT.getHeight() * 1.5F;
        int maxLines = 15;
        float width = Math.max(longest, 350.0F);
        float height = BlackOut.BOLD_FONT.getHeight() + Math.min(changelog.size(), maxLines) * fontHeight + 50.0F;

        boolean mousePressed = GLFW.glfwGetMouseButton(BlackOut.mc.getWindow().getHandle(), 0) == 1;
        if (this.dragging) {
            if (mousePressed) {
                this.targetX = mx - this.dragOffsetX;
                this.targetY = my - this.dragOffsetY;
            } else {
                this.dragging = false;
            }
        } else if (mousePressed) {
            if (mx >= this.changelogX && mx <= this.changelogX + width && my >= this.changelogY && my <= this.changelogY + 40.0F) {
                this.dragging = true;
                this.dragOffsetX = mx - this.changelogX;
                this.dragOffsetY = my - this.changelogY;
            }
        }
        this.changelogX = MathHelper.lerp(0.2F, this.changelogX, this.targetX);
        this.changelogY = MathHelper.lerp(0.2F, this.changelogY, this.targetY);

        stack.push();
        stack.translate(this.changelogX, this.changelogY, 0.0F);

        RenderUtils.roundedShadow(stack, 0.0F, 0.0F, width, height, 15.0F, 15.0F, new Color(0, 0, 0, 120).getRGB());
        RenderUtils.drawLoadedBlur("title", stack, renderer -> renderer.rounded(0.0F, 0.0F, width, height, 15.0F, 10, 1.0F, 1.0F, 1.0F, 1.0F));

        RenderUtils.rounded(stack, 0.0F, 0.0F, width, height, 15.0F, 2.0F, new Color(20, 20, 20, 160).getRGB(), new Color(0, 0, 0, 200).getRGB());

        if (themeMode && mainColor != null && secondColor != null) {
            RenderUtils.tenaRounded(stack, 0.0F, 0.0F, width, height, 15.0F, 1.5F, mainColor.getRGB(), secondColor.getRGB(), speed);
        }

        BlackOut.BOLD_FONT.text(stack, "Update Notes", 2.2F, width / 2.0F, 12.0F, Color.WHITE.getRGB(), true, false);
        RenderUtils.rounded(stack, 15.0F, 38.0F, width - 30.0F, 1.5F, 1.0F, 0.0F, new Color(255, 255, 255, 50).getRGB(), 0);

        for (int j = 0; j < Math.min(changelog.size(), maxLines); j++) {
            BlackOut.FONT.text(stack, "• " + changelog.get(j), 1.5F, 18.0F, 50.0F + (j * fontHeight), new Color(225, 225, 225).getRGB(), false, false);
        }
        stack.pop();
    }
}