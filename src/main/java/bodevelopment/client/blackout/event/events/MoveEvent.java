package bodevelopment.client.blackout.event.events;

import bodevelopment.client.blackout.event.Cancellable;
import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.modules.combat.defensive.Clip;
import bodevelopment.client.blackout.module.modules.misc.Clear;
import bodevelopment.client.blackout.module.modules.movement.*;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

public class MoveEvent {
    public static class Post {
        private static final Post INSTANCE = new Post();

        private Post() {}

        public static Post get() {
            return INSTANCE;
        }
    }

    public static class PostSend {
        private static final PostSend INSTANCE = new PostSend();

        private PostSend() {}

        public static PostSend get() {
            return INSTANCE;
        }
    }

    public static class Pre extends Cancellable {
        private static final Pre INSTANCE = new Pre();
        public Vec3d originalMovement = new Vec3d(0.0, 0.0, 0.0);
        public Vec3d movement = new Vec3d(0.0, 0.0, 0.0);
        public MovementType movementType = MovementType.SELF;
        public int xzValue = 0;
        public int yValue = 0;

        public static Pre get(Vec3d movement, MovementType type) {
            INSTANCE.movement = movement;
            INSTANCE.movementType = type;
            INSTANCE.originalMovement = movement;
            INSTANCE.xzValue = 0;
            INSTANCE.yValue = 0;
            INSTANCE.setCancelled(false);
            return INSTANCE;
        }

        public void setXZ(Module module, double x, double z) {
            int v = this.getValue(module);
            if (this.xzValue <= v) {
                ((IVec3d) this.movement).blackout_Client$setXZ(x, z);
                this.xzValue = v;
            }
        }

        public void setY(Module module, double y) {
            int v = this.getValue(module);
            if (this.yValue <= v) {
                ((IVec3d) this.movement).blackout_Client$setY(y);
                this.yValue = v;
            }
        }

        public void set(Module module, double x, double y, double z) {
            this.setXZ(module, x, z);
            this.setY(module, y);
        }

        private int getValue(Module module) {
            return switch (module) {
                case BurrowTrap burrowTrap -> 13;
                case Clip clip -> 5;
                case ElytraFly elytraFly -> 10;
                case FastFall fastFall -> 11;
                case Flight flight -> 9;
                case HoleSnap holeSnap -> 6;
                case Jesus jesus -> 4;
                case LongJump longJump -> 8;
                case PacketFly packetFly -> 14;
                case PhaseWalk phaseWalk -> 15;
                case Scaffold scaffold -> 7;
                case Speed speed -> 1;
                case Strafe strafe -> 2;
                case TargetStrafe targetStrafe -> 3;
                case null, default -> module instanceof Clear ? 16 : 0;
            };
        }
    }
}
