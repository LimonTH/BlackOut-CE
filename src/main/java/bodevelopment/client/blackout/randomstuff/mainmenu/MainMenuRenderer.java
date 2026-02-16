package bodevelopment.client.blackout.randomstuff.mainmenu;

import net.minecraft.client.util.math.MatrixStack;

public interface MainMenuRenderer {
    void render(MatrixStack stack, float height, float mx, float my, String text);

    void renderBackground(MatrixStack stack, float width, float height, float mx, float my);

    int onClick(float mx, float my);
}
