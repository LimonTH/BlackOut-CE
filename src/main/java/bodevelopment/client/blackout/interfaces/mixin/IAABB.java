package bodevelopment.client.blackout.interfaces.mixin;

public interface IAABB {
    void blackout_Client$set(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    void blackout_Client$setMinX(double minX);
    void blackout_Client$setMinY(double minY);
    void blackout_Client$setMinZ(double minZ);
    void blackout_Client$setMaxX(double maxX);
    void blackout_Client$setMaxY(double maxY);
    void blackout_Client$setMaxZ(double maxZ);
}
