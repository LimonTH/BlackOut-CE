package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.module.modules.visual.world.SeedFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Vanilla-accurate End City piece simulation for ship detection.
 * <p>
 * Extracted from {@link SeedFinder} for OOP clarity (пункт 1.8).
 * Replicates {@code EndCityPieces.startHouseTower()} + {@code recursiveChildren()}
 * exactly as in vanilla 1.21.4 MojMap, using only Random (seed-deterministic).
 */
public final class EndCitySimulator {

    private static final long REGION_X_MULT = 341873128712L;
    private static final long REGION_Z_MULT = 132897987541L;

    private static final int T_BASE_FLOOR = 0, T_BASE_ROOF = 1, T_BRIDGE_END = 2, T_BRIDGE_GENTLE = 3,
            T_BRIDGE_PIECE = 4, T_BRIDGE_STEEP = 5, T_FAT_BASE = 6, T_FAT_MID = 7, T_FAT_TOP = 8,
            T_FLOOR2_1 = 9, T_FLOOR2_2 = 10, T_ROOF2 = 11, T_SHIP = 12, T_FLOOR3_1 = 13, T_FLOOR3_2 = 14,
            T_ROOF3 = 15, T_TOWER_BASE = 16, T_TOWER_PIECE = 17, T_TOWER_TOP = 18;

    private static final int[][] TSIZES = {
            {10, 4, 10}, {12, 2, 12}, {5, 6, 2}, {5, 7, 8}, {5, 6, 4}, {5, 7, 4},
            {13, 4, 13}, {13, 8, 13}, {17, 6, 17},
            {12, 8, 12}, {12, 8, 12}, {14, 2, 14}, {13, 24, 29},
            {14, 8, 14}, {14, 8, 14}, {16, 2, 16},
            {7, 7, 7}, {7, 4, 7}, {9, 5, 9}
    };

    private static final int[][] TOWER_BD = {{0, 1, -1, 0}, {1, 6, -1, 1}, {3, 0, -1, 5}, {2, 5, -1, 6}};
    private static final int[][] FAT_BD = {{0, 4, -1, 0}, {1, 12, -1, 4}, {3, 0, -1, 8}, {2, 8, -1, 12}};

    private EndCitySimulator() {}

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

    private record SP(int px, int py, int pz, int x1, int y1, int z1, int x2, int y2, int z2, int gd, int rot) {}

    private static SP mkp(int px, int py, int pz, int t, int rot, int gd) {
        int sx = TSIZES[t][0] - 1, sy = TSIZES[t][1] - 1, sz = TSIZES[t][2] - 1;
        int cx = rx(sx, sz, rot), cz = rz(sx, sz, rot);
        return new SP(px, py, pz,
                px + Math.min(0, cx), py, pz + Math.min(0, cz),
                px + Math.max(0, cx), py + sy, pz + Math.max(0, cz),
                gd, rot);
    }

    private static SP child(SP par, int ox, int oy, int oz, int t, int rot) {
        return mkp(par.px + rx(ox, oz, par.rot), par.py + oy, par.pz + rz(ox, oz, par.rot), t, rot, par.gd);
    }

    private static boolean overlaps(SP a, SP b) {
        return a.x2 >= b.x1 && a.x1 <= b.x2 && a.y2 >= b.y1 && a.y1 <= b.y2 && a.z2 >= b.z1 && a.z1 <= b.z2;
    }

    private static SP findHit(List<SP> pieces, SP test) {
        for (SP p : pieces) if (overlaps(p, test)) return p;
        return null;
    }

    /**
     * Simulates End City piece generation for a given chunk and returns whether a ship exists.
     * Mirrors vanilla {@code EndCityPieces.startHouseTower()} + recursive piece generation.
     * <p>
     * <b>Seed derivation (vanilla 1.21.4 MojMap):</b><br>
     * {@code WorldgenRandom.setLargeFeatureSeed(worldSeed, chunkX, chunkZ)} does:
     * <ol>
     *   <li>{@code setSeed(worldSeed)}</li>
     *   <li>{@code nextLong() → seed1}</li>
     *   <li>{@code nextLong() → seed2}</li>
     *   <li>{@code setSeed(worldSeed ^ (chunkX * seed1) ^ (chunkZ * seed2))}</li>
     * </ol>
     * This is <b>NOT</b> the simple formula {@code chunkX * 341873128712L + chunkZ * 132897987541L + worldSeed}
     * (which is only used by {@code setLargeFeatureWithSalt} for structure placement, not piece generation).
     * <p>
     * After seeding, {@code EndCityStructure.findGenerationPoint()} consumes one {@code nextInt(4)}
     * for {@code Rotation.getRandom()} before passing the random to {@code startHouseTower}.
     *
     * @param worldSeed the world seed
     * @param chunkX    chunk X coordinate
     * @param chunkZ    chunk Z coordinate
     * @return true if this end city has a ship
     */
    public static boolean hasShip(long worldSeed, int chunkX, int chunkZ) {
        Random r = new Random(worldSeed);
        long seed1 = r.nextLong();
        long seed2 = r.nextLong();
        long finalSeed = worldSeed ^ ((long) chunkX * seed1) ^ ((long) chunkZ * seed2);
        r.setSeed(finalSeed);

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

        boolean[] ship = {false};
        rc(pieces, r, 0, 1, p, rot, ship);
        return ship[0];
    }

    private static boolean rc(List<SP> main, Random r, int gt, int depth, SP parent, int rot, boolean[] ship) {
        return rc(main, r, gt, depth, parent, rot, ship, 0, 0, 0);
    }

    private static boolean rc(List<SP> main, Random r, int gt, int depth, SP parent, int rot, boolean[] ship,
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
                SP tp2 = new SP(tp.px, tp.py, tp.pz, tp.x1, tp.y1, tp.z1, tp.x2, tp.y2, tp.z2, gd, tp.rot);
                SP hit = findHit(main, tp2);
                if (hit != null && hit.gd != parent.gd) {
                    collision = true;
                    break;
                }
            }
            if (!collision) {
                for (SP tp : tmp)
                    main.add(new SP(tp.px, tp.py, tp.pz, tp.x1, tp.y1, tp.z1, tp.x2, tp.y2, tp.z2, gd, tp.rot));
                return true;
            }
        }
        return false;
    }

    private static boolean tower(List<SP> L, Random r, int d, SP par, int rot, boolean[] ship) {
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

    private static boolean bridge(List<SP> L, Random r, int d, SP par, int rot, boolean[] ship) {
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
        if (!ship[0] && r.nextInt(10 - d) == 0) {
            SP s = child(p, -8 + r.nextInt(8), k, -70 + r.nextInt(10), T_SHIP, rot);
            L.add(s);
            ship[0] = true;
        } else if (!rc(L, r, 2, d + 1, p, rot, ship, -3, k + 1, -11)) {
            return false;
        }
        SP end = child(p, 4, k, 0, T_BRIDGE_END, (rot + 2) % 4);
        L.add(end);
        return true;
    }

    private static boolean house(List<SP> L, Random r, int d, SP par, int rot, boolean[] ship,
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

    private static boolean fat(List<SP> L, Random r, int d, SP par, int rot, boolean[] ship) {
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
}
