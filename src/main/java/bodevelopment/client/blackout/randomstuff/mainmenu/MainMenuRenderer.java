package bodevelopment.client.blackout.randomstuff.mainmenu;

import net.minecraft.client.util.math.MatrixStack;

public interface MainMenuRenderer {
    void render(MatrixStack stack, float height, float mx, float my, String text1, String text2, float progress);
    void renderBackground(MatrixStack stack, float width, float height, float mx, float my);

    int onClick(float mx, float my);
}
