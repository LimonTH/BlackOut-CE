package bodevelopment.client.blackout.util.world;

import net.minecraft.world.level.levelgen.WorldgenRandom;

import java.util.ArrayList;
import java.util.List;

public final class EndCitySimulationUtils {
    private static final int T_BASE_FLOOR = 0;
    private static final int T_BASE_ROOF = 1;
    private static final int T_BRIDGE_END = 2;
    private static final int T_BRIDGE_GENTLE = 3;
    private static final int T_BRIDGE_PIECE = 4;
    private static final int T_BRIDGE_STEEP = 5;
    private static final int T_FAT_BASE = 6;
    private static final int T_FAT_MID = 7;
    private static final int T_FAT_TOP = 8;
    private static final int T_FLOOR2_1 = 9;
    private static final int T_FLOOR2_2 = 10;
    private static final int T_ROOF2 = 11;
    private static final int T_SHIP = 12;
    private static final int T_FLOOR3_1 = 13;
    private static final int T_FLOOR3_2 = 14;
    private static final int T_ROOF3 = 15;
    private static final int T_TOWER_BASE = 16;
    private static final int T_TOWER_PIECE = 17;
    private static final int T_TOWER_TOP = 18;

    private static final int[][] TSIZES = {
            {10, 4, 10}, {12, 2, 12}, {5, 6, 2}, {5, 7, 8}, {5, 6, 4}, {5, 7, 4},
            {13, 4, 13}, {13, 8, 13}, {17, 6, 17},
            {12, 8, 12}, {12, 8, 12}, {14, 2, 14}, {13, 24, 29},
            {14, 8, 14}, {14, 8, 14}, {16, 2, 16},
            {7, 7, 7}, {7, 4, 7}, {9, 5, 9}
    };

    private static final int[][] TOWER_BD = {{0, 1, -1, 0}, {1, 6, -1, 1}, {3, 0, -1, 5}, {2, 5, -1, 6}};
    private static final int[][] FAT_BD = {{0, 4, -1, 0}, {1, 12, -1, 4}, {3, 0, -1, 8}, {2, 8, -1, 12}};

    private EndCitySimulationUtils() {
    }

    private static int rx(int x, int z, int r) {
        return switch (r) {
            case 1 -> z;
            case 2 -> -x;
            case 3 -> -z;
            default -> x;
        };
    }

    private static int rz(int x, int z, int r) {
        return switch (r) {
            case 1 -> -x;
            case 2 -> -z;
            case 3 -> x;
            default -> z;
        };
    }

    private static SP mkp(int px, int py, int pz, int t, int rot, int gd) {
        int sx = TSIZES[t][0] - 1, sy = TSIZES[t][1] - 1, sz = TSIZES[t][2] - 1;
        int cx = rx(sx, sz, rot), cz = rz(sx, sz, rot);
        return new SP(px, py, pz,
                px + Math.min(0, cx), py, pz + Math.min(0, cz),
                px + Math.max(0, cx), py + sy, pz + Math.max(0, cz),
                gd, rot);
    }

    private static SP child(SP par, int ox, int oy, int oz, int t, int rot) {
        return mkp(par.px + rx(ox, oz, par.rot), par.py + oy,
                par.pz + rz(ox, oz, par.rot), t, rot, par.gd);
    }

    private static boolean overlaps(SP a, SP b) {
        return a.x2 >= b.x1 && a.x1 <= b.x2
                && a.y2 >= b.y1 && a.y1 <= b.y2
                && a.z2 >= b.z1 && a.z1 <= b.z2;
    }

    private static SP findHit(List<SP> pieces, SP test) {
        for (SP p : pieces) if (overlaps(p, test)) return p;
        return null;
    }

    public static boolean hasShip(long worldSeed, int chunkX, int chunkZ) {
        return getShipInfo(worldSeed, chunkX, chunkZ) != null;
    }

    public static ShipInfo getShipInfo(long worldSeed, int chunkX, int chunkZ) {
        WorldgenRandom r = RandomUtils.endCityRandom(worldSeed, chunkX, chunkZ);
        int rot = r.nextInt(4);

        List<SP> pieces = new ArrayList<>();
        SP p = mkp(0, 0, 0, T_BASE_FLOOR, rot, 0);
        pieces.add(p);
        p = child(p, -1, 0, -1, T_FLOOR2_1, rot);
        pieces.add(p);
        p = child(p, -1, 4, -1, T_FLOOR3_1, rot);
        pieces.add(p);
        p = child(p, -1, 8, -1, T_ROOF3, rot);
        pieces.add(p);

        SP[] shipPiece = {null};
        rc(pieces, r, 0, 1, p, rot, shipPiece);
        if (shipPiece[0] == null) return null;

        SP s = shipPiece[0];
        return new ShipInfo(s.x1, s.y1, s.z1, s.x2, s.y2, s.z2, s.rot);
    }

    private static boolean rc(List<SP> main, WorldgenRandom r, int gt, int depth,
                              SP parent, int rot, SP[] ship) {
        return rc(main, r, gt, depth, parent, rot, ship, 0, 0, 0);
    }

    private static boolean rc(List<SP> main, WorldgenRandom r, int gt, int depth,
                              SP parent, int rot, SP[] ship,
                              int bpX, int bpY, int bpZ) {
        if (depth > 8) return false;
        List<SP> tmp = new ArrayList<>();
        boolean ok = switch (gt) {
            case 0 -> tower(tmp, r, depth, parent, rot, ship);
            case 1 -> bridge(tmp, r, depth, parent, rot, ship);
            case 2 -> house(tmp, r, depth, parent, rot, ship, bpX, bpY, bpZ);
            case 3 -> fat(tmp, r, depth, parent, rot, ship);
            default -> false;
        };
        if (ok) {
            int gd = r.nextInt();
            boolean collision = false;
            for (SP tp : tmp) {
                SP tp2 = new SP(tp.px, tp.py, tp.pz, tp.x1, tp.y1, tp.z1,
                        tp.x2, tp.y2, tp.z2, gd, tp.rot);
                SP hit = findHit(main, tp2);
                if (hit != null && hit.gd != parent.gd) {
                    collision = true;
                    break;
                }
            }
            if (!collision) {
                for (SP tp : tmp)
                    main.add(new SP(tp.px, tp.py, tp.pz, tp.x1, tp.y1, tp.z1,
                            tp.x2, tp.y2, tp.z2, gd, tp.rot));
                return true;
            }
        }
        return false;
    }

    private static boolean tower(List<SP> L, WorldgenRandom r, int d, SP par, int rot, SP[] ship) {
        int tox = 3 + r.nextInt(2), toz = 3 + r.nextInt(2);
        SP p = child(par, tox, -3, toz, T_TOWER_BASE, rot);
        L.add(p);
        p = child(p, 0, 7, 0, T_TOWER_PIECE, rot);
        L.add(p);
        SP anchor = r.nextInt(3) == 0 ? p : null;
        int layers = 1 + r.nextInt(3);
        for (int k = 0; k < layers; k++) {
            p = child(p, 0, 4, 0, T_TOWER_PIECE, rot);
            L.add(p);
            if (k < layers - 1 && r.nextBoolean()) anchor = p;
        }
        if (anchor != null) {

            for (int[] bd : TOWER_BD) {
                if (r.nextBoolean()) {
                    int br = (rot + bd[0]) % 4;
                    SP be = child(anchor, bd[1], bd[2], bd[3], T_BRIDGE_END, br);
                    L.add(be);
                    rc(L, r, 1, d + 1, be, br, ship);
                }
            }
            SP top = child(p, -1, 4, -1, T_TOWER_TOP, rot);
            L.add(top);
        } else {
            if (d != 7) rc(L, r, 3, d + 1, p, rot, ship);
            SP top = child(p, -1, 4, -1, T_TOWER_TOP, rot);
            L.add(top);
        }
        return true;
    }

    private static boolean bridge(List<SP> L, WorldgenRandom r, int d, SP par, int rot, SP[] ship) {
        int blen = r.nextInt(4) + 1;
        SP p = child(par, 0, 0, -4, T_BRIDGE_PIECE, rot);
        L.add(p);
        int k = 0;
        for (int l = 0; l < blen; l++) {
            if (r.nextBoolean()) {
                p = child(p, 0, k, -4, T_BRIDGE_PIECE, rot);
                L.add(p);
                k = 0;
            } else {
                if (r.nextBoolean()) {
                    p = child(p, 0, k, -4, T_BRIDGE_STEEP, rot);
                } else {
                    p = child(p, 0, k, -8, T_BRIDGE_GENTLE, rot);
                }
                L.add(p);
                k = 4;
            }
        }
        if (ship[0] == null && r.nextInt(10 - d) == 0) {
            SP s = child(p, -8 + r.nextInt(8), k, -70 + r.nextInt(10), T_SHIP, rot);
            L.add(s);
            ship[0] = s;
        } else if (!rc(L, r, 2, d + 1, p, rot, ship, -3, k + 1, -11)) {
            return false;
        }
        SP end = child(p, 4, k, 0, T_BRIDGE_END, (rot + 2) % 4);
        L.add(end);
        return true;
    }

    private static boolean house(List<SP> L, WorldgenRandom r, int d, SP par, int rot, SP[] ship,
                                 int bpX, int bpY, int bpZ) {
        if (d > 8) return false;
        SP p = child(par, bpX, bpY, bpZ, T_BASE_FLOOR, rot);
        L.add(p);
        int j = r.nextInt(3);
        if (j == 0) {
            p = child(p, -1, 4, -1, T_BASE_ROOF, rot);
            L.add(p);
        } else if (j == 1) {
            p = child(p, -1, 0, -1, T_FLOOR2_2, rot);
            L.add(p);
            p = child(p, -1, 8, -1, T_ROOF2, rot);
            L.add(p);
            rc(L, r, 0, d + 1, p, rot, ship);
        } else {
            p = child(p, -1, 0, -1, T_FLOOR2_2, rot);
            L.add(p);
            p = child(p, -1, 4, -1, T_FLOOR3_2, rot);
            L.add(p);
            p = child(p, -1, 8, -1, T_ROOF3, rot);
            L.add(p);
            rc(L, r, 0, d + 1, p, rot, ship);
        }
        return true;
    }

    private static boolean fat(List<SP> L, WorldgenRandom r, int d, SP par, int rot, SP[] ship) {
        SP p = child(par, -3, 4, -3, T_FAT_BASE, rot);
        L.add(p);
        p = child(p, 0, 4, 0, T_FAT_MID, rot);
        L.add(p);
        for (int j = 0; j < 2 && r.nextInt(3) != 0; j++) {
            p = child(p, 0, 8, 0, T_FAT_MID, rot);
            L.add(p);
            for (int[] bd : FAT_BD) {
                if (r.nextBoolean()) {
                    int br = (rot + bd[0]) % 4;
                    SP be = child(p, bd[1], bd[2], bd[3], T_BRIDGE_END, br);
                    L.add(be);
                    rc(L, r, 1, d + 1, be, br, ship);
                }
            }
        }
        SP top = child(p, -2, 8, -2, T_FAT_TOP, rot);
        L.add(top);
        return true;
    }

    private record SP(int px, int py, int pz,
                      int x1, int y1, int z1,
                      int x2, int y2, int z2,
                      int gd, int rot) {
    }

    public record ShipInfo(int relMinX, int relMinY, int relMinZ,
                           int relMaxX, int relMaxY, int relMaxZ,
                           int rot) {
        public int worldX(int chunkOriginX) {
            return chunkOriginX + relMinX;
        }

        public int worldZ(int chunkOriginZ) {
            return chunkOriginZ + relMinZ;
        }

        public int chestY() {
            return relMinY + 2;
        }
    }
}

