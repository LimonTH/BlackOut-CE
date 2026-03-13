package bodevelopment.client.blackout.event.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

public class RenderEvent {
    public double frameTime = 0.0;
    public float tickDelta = 0.0F;
    private long prevEvent = 0L;

    protected void setFrameTime() {
        if (this.prevEvent > 0L) {
            this.frameTime = (System.currentTimeMillis() - this.prevEvent) / 1000.0;
        }

        this.prevEvent = System.currentTimeMillis();
    }

    public static class Hud extends RenderEvent {
        public GuiGraphics context;

        public static class Post extends Hud {
            private static final Post INSTANCE = new Post();

            public static Post get(GuiGraphics context, float tickDelta) {
                INSTANCE.context = context;
                INSTANCE.tickDelta = tickDelta;
                INSTANCE.setFrameTime();
                return INSTANCE;
            }
        }

        public static class Pre extends Hud {
            private static final Pre INSTANCE = new Pre();

            public static Pre get(GuiGraphics context, float tickDelta) {
                INSTANCE.context = context;
                INSTANCE.tickDelta = tickDelta;
                INSTANCE.setFrameTime();
                return INSTANCE;
            }
        }
    }

    public static class World extends RenderEvent {
        public PoseStack stack = null;

        public static class Post extends World {
            private static final Post INSTANCE = new Post();

            public static Post get(PoseStack stack, float tickDelta) {
                INSTANCE.stack = stack;
                INSTANCE.tickDelta = tickDelta;
                INSTANCE.setFrameTime();
                return INSTANCE;
            }
        }

        public static class Pre extends World {
            private static final Pre INSTANCE = new Pre();

            public static Pre get(PoseStack stack, float tickDelta) {
                INSTANCE.stack = stack;
                INSTANCE.tickDelta = tickDelta;
                INSTANCE.setFrameTime();
                return INSTANCE;
            }
        }
    }
}
