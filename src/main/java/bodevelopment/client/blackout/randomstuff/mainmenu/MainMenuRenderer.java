package bodevelopment.client.blackout.randomstuff.mainmenu;

import com.mojang.blaze3d.vertex.PoseStack;

public interface MainMenuRenderer {
    void render(PoseStack stack, float height, float mx, float my, String text1, String text2, float progress);
    void renderBackground(PoseStack stack, float width, float height, float mx, float my);

    int onClick(float mx, float my);
}
