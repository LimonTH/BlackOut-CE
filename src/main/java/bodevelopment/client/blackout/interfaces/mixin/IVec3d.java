package bodevelopment.client.blackout.interfaces.mixin;

public interface IVec3d {
    void blackout_Client$set(double x, double y, double z);

    void blackout_Client$setXZ(double x, double z);

    void blackout_Client$setX(double x);

    void blackout_Client$setY(double y);

    void blackout_Client$setZ(double z);
}
