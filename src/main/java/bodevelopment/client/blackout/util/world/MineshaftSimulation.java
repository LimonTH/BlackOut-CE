/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.util.world;

import bodevelopment.client.blackout.annotations.PublicAPI;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import java.util.ArrayList;
import java.util.List;

@PublicAPI
public final class MineshaftSimulation {
    public static final int MS_ROOM = 0;
    public static final int MS_CORRIDOR = 1;
    public static final int MS_CROSSING = 2;
    public static final int MS_STAIRS = 3;

    private MineshaftSimulation() {
    }

    @PublicAPI
    public static List<MSPiece> getMineshaftPieces(long worldSeed, int chunkX, int chunkZ) {
        int x = (chunkX << 4) + 2;
        int z = (chunkZ << 4) + 2;

        WorldgenRandom rng = chunkGenerateRandomXoroshiro(worldSeed, chunkX, chunkZ);
        rng.nextDouble();

        List<MSPiece> pieces = new ArrayList<>();
        MSPiece startRoom = new MSPiece(MS_ROOM, x, 50, z);
        startRoom.depth = 0;
        startRoom.bbMaxX = x + 7 + rng.nextInt(6);
        startRoom.bbMaxY = 50 + 4 + rng.nextInt(6);
        startRoom.bbMaxZ = z + 7 + rng.nextInt(6);
        pieces.add(startRoom);

        int[] count = {1};
        extendMineshaftPiece(pieces, rng, startRoom, count);
        return pieces;
    }

    private static WorldgenRandom chunkGenerateRandomXoroshiro(long worldSeed, int chunkX, int chunkZ) {
        WorldgenRandom rng = new WorldgenRandom(new XoroshiroRandomSource(0L));
        rng.setSeed(worldSeed);
        long a = rng.nextLong() | 1L;
        long b = rng.nextLong() | 1L;
        rng.setSeed(worldSeed ^ ((long) chunkX * a) ^ ((long) chunkZ * b));
        return rng;
    }

    private static void extendMineshaftPiece(List<MSPiece> pieces, WorldgenRandom rng,
                                             MSPiece piece, int[] count) {
        if (count[0] >= 256) return;
        switch (piece.type) {
            case MS_CORRIDOR -> extendCorridor(pieces, rng, piece, count);
            case MS_CROSSING -> extendCrossing(pieces, rng, piece, count);
            case MS_ROOM -> extendRoom(pieces, rng, piece, count);
            case MS_STAIRS -> extendStairs(pieces, rng, piece, count);
        }
    }

    private static void extendMineshaft(List<MSPiece> pieces, WorldgenRandom rng,
                                        int x, int y, int z, int facing, int depth, int[] count) {
        if (depth > 8 || count[0] >= 256) return;
        MSPiece start = pieces.get(0);
        if (Math.abs(x - start.posX) > 80 || Math.abs(z - start.posZ) > 80) return;
        depth++;
        int sel = rng.nextInt(100);

        if (sel >= 80) {
            int y1 = 2 + 4 * (rng.nextInt(4) == 0 ? 1 : 0);
            int[] box = getCrossingBox(facing, x, y, z, y1);
            if (!hasCollision(pieces, box[0], box[1], box[2], box[3], box[4], box[5])) {
                MSPiece p = new MSPiece(MS_CROSSING, x, y, z);
                p.bbMinX = box[0];
                p.bbMinY = box[1];
                p.bbMinZ = box[2];
                p.bbMaxX = box[3];
                p.bbMaxY = box[4];
                p.bbMaxZ = box[5];
                p.rot = facing;
                p.depth = depth;
                pieces.add(p);
                count[0]++;
                extendMineshaftPiece(pieces, rng, p, count);
            }
        } else if (sel >= 70) {
            int[] box = getStairsBox(facing, x, y, z);
            if (!hasCollision(pieces, box[0], box[1], box[2], box[3], box[4], box[5])) {
                MSPiece p = new MSPiece(MS_STAIRS, x, y, z);
                p.bbMinX = box[0];
                p.bbMinY = box[1];
                p.bbMinZ = box[2];
                p.bbMaxX = box[3];
                p.bbMaxY = box[4];
                p.bbMaxZ = box[5];
                p.rot = facing;
                p.depth = depth;
                pieces.add(p);
                count[0]++;
            }
        } else {
            for (int cl = rng.nextInt(3) + 2; cl > 0; cl--) {
                int bl = cl * 5;
                int[] box = getCorridorBox(facing, x, y, z, bl);
                if (!hasCollision(pieces, box[0], box[1], box[2], box[3], box[4], box[5])) {
                    MSPiece p = new MSPiece(MS_CORRIDOR, x, y, z);
                    p.bbMinX = box[0];
                    p.bbMinY = box[1];
                    p.bbMinZ = box[2];
                    p.bbMaxX = box[3];
                    p.bbMaxY = box[4];
                    p.bbMaxZ = box[5];
                    p.rot = facing;
                    p.depth = depth;
                    p.additionalData = (rng.nextInt(3) == 0 ? 1 : 0)
                            | ((rng.nextInt(23) == 0 && (p.additionalData & 1) == 0) ? 2 : 0);
                    pieces.add(p);
                    count[0]++;
                    extendMineshaftPiece(pieces, rng, p, count);
                    return;
                }
            }
        }
    }

    private static int[] getCrossingBox(int f, int x, int y, int z, int y1) {
        return switch (f) {
            case 0 -> new int[]{x - 1, y, z - 4, x + 3, y + y1, z};
            case 1 -> new int[]{x, y, z - 1, x + 4, y + y1, z + 3};
            case 2 -> new int[]{x - 1, y, z, x + 3, y + y1, z + 4};
            case 3 -> new int[]{x - 4, y, z - 1, x, y + y1, z + 3};
            default -> throw new IllegalStateException();
        };
    }

    private static int[] getStairsBox(int f, int x, int y, int z) {
        return switch (f) {
            case 0 -> new int[]{x, y - 5, z - 8, x + 2, y + 2, z};
            case 1 -> new int[]{x, y - 5, z, x + 8, y + 2, z + 2};
            case 2 -> new int[]{x, y - 5, z, x + 2, y + 2, z + 8};
            case 3 -> new int[]{x - 8, y - 5, z, x, y + 2, z + 2};
            default -> throw new IllegalStateException();
        };
    }

    private static int[] getCorridorBox(int f, int x, int y, int z, int bl) {
        return switch (f) {
            case 0 -> new int[]{x, y, z - (bl - 1), x + 2, y + 2, z};
            case 1 -> new int[]{x, y, z, x + (bl - 1), y + 2, z + 2};
            case 2 -> new int[]{x, y, z, x + 2, y + 2, z + (bl - 1)};
            case 3 -> new int[]{x - (bl - 1), y, z, x, y + 2, z + 2};
            default -> throw new IllegalStateException();
        };
    }

    private static void extendCorridor(List<MSPiece> pieces, WorldgenRandom rng,
                                       MSPiece piece, int[] count) {
        int es = rng.nextInt(4);
        int rot = piece.rot;
        switch (rot) {
            case 0 -> {
                if (es <= 1)
                    extendMineshaft(pieces, rng, piece.bbMinX, piece.bbMinY - 1 + rng.nextInt(3), piece.bbMinZ - 1, 0, piece.depth, count);
                else if (es == 2)
                    extendMineshaft(pieces, rng, piece.bbMinX - 1, piece.bbMinY - 1 + rng.nextInt(3), piece.bbMinZ, 3, piece.depth, count);
                else
                    extendMineshaft(pieces, rng, piece.bbMaxX + 1, piece.bbMinY - 1 + rng.nextInt(3), piece.bbMinZ, 1, piece.depth, count);
            }
            case 1 -> {
                if (es <= 1)
                    extendMineshaft(pieces, rng, piece.bbMaxX + 1, piece.bbMinY - 1 + rng.nextInt(3), piece.bbMinZ, 1, piece.depth, count);
                else if (es == 2)
                    extendMineshaft(pieces, rng, piece.bbMaxX - 3, piece.bbMinY - 1 + rng.nextInt(3), piece.bbMinZ - 1, 0, piece.depth, count);
                else
                    extendMineshaft(pieces, rng, piece.bbMaxX - 3, piece.bbMinY - 1 + rng.nextInt(3), piece.bbMaxZ + 1, 2, piece.depth, count);
            }
            case 2 -> {
                if (es <= 1)
                    extendMineshaft(pieces, rng, piece.bbMinX, piece.bbMinY - 1 + rng.nextInt(3), piece.bbMaxZ + 1, 2, piece.depth, count);
                else if (es == 2)
                    extendMineshaft(pieces, rng, piece.bbMinX - 1, piece.bbMinY - 1 + rng.nextInt(3), piece.bbMaxZ - 3, 3, piece.depth, count);
                else
                    extendMineshaft(pieces, rng, piece.bbMaxX + 1, piece.bbMinY - 1 + rng.nextInt(3), piece.bbMaxZ - 3, 1, piece.depth, count);
            }
            case 3 -> {
                if (es <= 1)
                    extendMineshaft(pieces, rng, piece.bbMinX - 1, piece.bbMinY - 1 + rng.nextInt(3), piece.bbMinZ, 3, piece.depth, count);
                else if (es == 2)
                    extendMineshaft(pieces, rng, piece.bbMinX, piece.bbMinY - 1 + rng.nextInt(3), piece.bbMinZ - 1, 0, piece.depth, count);
                else
                    extendMineshaft(pieces, rng, piece.bbMinX, piece.bbMinY - 1 + rng.nextInt(3), piece.bbMaxZ + 1, 2, piece.depth, count);
            }
        }
        if (piece.depth >= 8) return;
        if (rot == 1 || rot == 3) {
            for (int sx = piece.bbMinX + 3; sx + 3 <= piece.bbMaxX; sx += 5) {
                int s = rng.nextInt(5);
                if (s == 0) extendMineshaft(pieces, rng, sx, piece.bbMinY, piece.bbMinZ - 1, 0, piece.depth + 1, count);
                else if (s == 1)
                    extendMineshaft(pieces, rng, sx, piece.bbMinY, piece.bbMaxZ + 1, 2, piece.depth + 1, count);
            }
        } else {
            for (int sz = piece.bbMinZ + 3; sz + 3 <= piece.bbMaxZ; sz += 5) {
                int s = rng.nextInt(5);
                if (s == 0) extendMineshaft(pieces, rng, piece.bbMinX - 1, piece.bbMinY, sz, 3, piece.depth + 1, count);
                else if (s == 1)
                    extendMineshaft(pieces, rng, piece.bbMaxX + 1, piece.bbMinY, sz, 1, piece.depth + 1, count);
            }
        }
    }

    private static void extendCrossing(List<MSPiece> pieces, WorldgenRandom rng, MSPiece p, int[] count) {
        switch (p.rot) {
            case 0 -> {
                extendMineshaft(pieces, rng, p.bbMinX + 1, p.bbMinY, p.bbMinZ - 1, 0, p.depth, count);
                extendMineshaft(pieces, rng, p.bbMinX - 1, p.bbMinY, p.bbMinZ + 1, 3, p.depth, count);
                extendMineshaft(pieces, rng, p.bbMaxX + 1, p.bbMinY, p.bbMinZ + 1, 1, p.depth, count);
            }
            case 1 -> {
                extendMineshaft(pieces, rng, p.bbMinX + 1, p.bbMinY, p.bbMinZ - 1, 0, p.depth, count);
                extendMineshaft(pieces, rng, p.bbMinX + 1, p.bbMinY, p.bbMaxZ + 1, 2, p.depth, count);
                extendMineshaft(pieces, rng, p.bbMaxX + 1, p.bbMinY, p.bbMinZ + 1, 1, p.depth, count);
            }
            case 2 -> {
                extendMineshaft(pieces, rng, p.bbMinX + 1, p.bbMinY, p.bbMaxZ + 1, 2, p.depth, count);
                extendMineshaft(pieces, rng, p.bbMinX - 1, p.bbMinY, p.bbMinZ + 1, 3, p.depth, count);
                extendMineshaft(pieces, rng, p.bbMaxX + 1, p.bbMinY, p.bbMinZ + 1, 1, p.depth, count);
            }
            case 3 -> {
                extendMineshaft(pieces, rng, p.bbMinX + 1, p.bbMinY, p.bbMinZ - 1, 0, p.depth, count);
                extendMineshaft(pieces, rng, p.bbMinX + 1, p.bbMinY, p.bbMaxZ + 1, 2, p.depth, count);
                extendMineshaft(pieces, rng, p.bbMinX - 1, p.bbMinY, p.bbMinZ + 1, 3, p.depth, count);
            }
        }
        if (p.bbMaxY - p.bbMinY + 1 <= 3) return;
        if (rng.nextBoolean())
            extendMineshaft(pieces, rng, p.bbMinX + 1, p.bbMinY + 4, p.bbMinZ - 1, 0, p.depth, count);
        if (rng.nextBoolean())
            extendMineshaft(pieces, rng, p.bbMinX - 1, p.bbMinY + 4, p.bbMinZ + 1, 3, p.depth, count);
        if (rng.nextBoolean())
            extendMineshaft(pieces, rng, p.bbMaxX + 1, p.bbMinY + 4, p.bbMinZ + 1, 1, p.depth, count);
        if (rng.nextBoolean())
            extendMineshaft(pieces, rng, p.bbMinX + 1, p.bbMinY + 4, p.bbMaxZ + 1, 2, p.depth, count);
    }

    private static void extendRoom(List<MSPiece> pieces, WorldgenRandom rng, MSPiece p, int[] count) {
        int hs = Math.max(p.bbMaxY - p.bbMinY + 1 - 4, 1);
        int xs = p.bbMaxX - p.bbMinX + 1, zs = p.bbMaxZ - p.bbMinZ + 1;
        for (int pos = 0; pos < xs; pos += 4) {
            pos += rng.nextInt(xs);
            if (pos + 3 > xs) break;
            extendMineshaft(pieces, rng, p.bbMinX + pos, p.bbMinY + rng.nextInt(hs) + 1, p.bbMinZ - 1, 0, p.depth, count);
        }
        for (int pos = 0; pos < xs; pos += 4) {
            pos += rng.nextInt(xs);
            if (pos + 3 > xs) break;
            extendMineshaft(pieces, rng, p.bbMinX + pos, p.bbMinY + rng.nextInt(hs) + 1, p.bbMaxZ + 1, 2, p.depth, count);
        }
        for (int pos = 0; pos < zs; pos += 4) {
            pos += rng.nextInt(zs);
            if (pos + 3 > zs) break;
            extendMineshaft(pieces, rng, p.bbMinX - 1, p.bbMinY + rng.nextInt(hs) + 1, p.bbMinZ + pos, 3, p.depth, count);
        }
        for (int pos = 0; pos < zs; pos += 4) {
            pos += rng.nextInt(zs);
            if (pos + 3 > zs) break;
            extendMineshaft(pieces, rng, p.bbMaxX + 1, p.bbMinY + rng.nextInt(hs) + 1, p.bbMinZ + pos, 1, p.depth, count);
        }
    }

    private static void extendStairs(List<MSPiece> pieces, WorldgenRandom rng, MSPiece p, int[] count) {
        switch (p.rot) {
            case 0 -> extendMineshaft(pieces, rng, p.bbMinX, p.bbMinY, p.bbMinZ - 1, 0, p.depth, count);
            case 1 -> extendMineshaft(pieces, rng, p.bbMaxX + 1, p.bbMinY, p.bbMinZ, 1, p.depth, count);
            case 2 -> extendMineshaft(pieces, rng, p.bbMinX, p.bbMinY, p.bbMaxZ + 1, 2, p.depth, count);
            case 3 -> extendMineshaft(pieces, rng, p.bbMinX - 1, p.bbMinY, p.bbMinZ, 3, p.depth, count);
        }
    }

    @PublicAPI
    public static List<StructurePieceSimulation.ChestInfo> getMineshaftChests(
            long worldSeed, int chunkX, int chunkZ) {

        List<MSPiece> pieces = getMineshaftPieces(worldSeed, chunkX, chunkZ);

        PopulationSeedUtils.StructureSaltConfig ssconf = PopulationSeedUtils.getSaltConfig(
                bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher.StructureType.MINESHAFT);

        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (MSPiece p : pieces) {
            minX = Math.min(minX, p.bbMinX);
            minZ = Math.min(minZ, p.bbMinZ);
            maxX = Math.max(maxX, p.bbMaxX);
            maxZ = Math.max(maxZ, p.bbMaxZ);
        }

        List<StructurePieceSimulation.ChestInfo> chests = new ArrayList<>();

        for (int cx = minX & ~15; cx <= (maxX & ~15); cx += 16) {
            for (int cz = minZ & ~15; cz <= (maxZ & ~15); cz += 16) {
                scanChunkForChests(worldSeed, cx, cz, pieces, ssconf, chests);
            }
        }

        return chests;
    }

    private static void scanChunkForChests(long worldSeed, int cx, int cz,
                                           List<MSPiece> pieces,
                                           PopulationSeedUtils.StructureSaltConfig ssconf,
                                           List<StructurePieceSimulation.ChestInfo> chests) {

        long populationSeed = PopulationSeedUtils.getPopulationSeed(worldSeed, cx, cz);
        WorldgenRandom rng = PopulationSeedUtils.createLootRng(populationSeed, ssconf);

        for (MSPiece piece : pieces) {
            if (piece.type != MS_CORRIDOR) continue;
            if (piece.bbMaxX < cx || piece.bbMinX > cx + 15 ||
                    piece.bbMaxZ < cz || piece.bbMinZ > cz + 15) continue;

            int numSections;
            if (piece.rot == 0 || piece.rot == 2) {
                numSections = (piece.bbMaxZ - piece.bbMinZ + 1) / 5;
            } else {
                numSections = (piece.bbMaxX - piece.bbMinX + 1) / 5;
            }
            int length = numSections * 5 - 1;

            int boxCalls = 3 * (length + 1);
            for (int i = 0; i < boxCalls; i++) maybeGenerateBlock(rng);

            if ((piece.additionalData >> 1 & 1) != 0) {
                for (int i = 0; i < boxCalls; i++) maybeGenerateBlock(rng);
            }

            for (int section = 0; section < numSections; section++) {
                int sectionZ = 2 + section * 5;

                consumeSupportCobweb(rng, piece, cx, cz, sectionZ);

                if (rng.nextInt(100) == 0) {
                    int[] rp = rotateLocal(piece, 2, sectionZ - 1);
                    if (rp[0] >= cx && rp[0] < cx + 16 && rp[1] >= cz && rp[1] < cz + 16) {
                        rng.nextBoolean();
                        chests.add(new StructurePieceSimulation.ChestInfo(
                                rp[0], piece.bbMinY + 1, rp[1], rng.nextLong(), "chests/abandoned_mineshaft"));
                    }
                }

                if (rng.nextInt(100) == 0) {
                    int[] rp = rotateLocal(piece, 0, sectionZ + 1);
                    if (rp[0] >= cx && rp[0] < cx + 16 && rp[1] >= cz && rp[1] < cz + 16) {
                        rng.nextBoolean();
                        chests.add(new StructurePieceSimulation.ChestInfo(
                                rp[0], piece.bbMinY + 1, rp[1], rng.nextLong(), "chests/abandoned_mineshaft"));
                    }
                }

                if ((piece.additionalData >> 1 & 0b11) == 0b01) {
                    int[] rp = rotateLocal(piece, 1, sectionZ - 1 + rng.nextInt(3));
                    if (rp[0] >= cx && rp[0] < cx + 16 && rp[1] >= cz && rp[1] < cz + 16) {
                        piece.additionalData |= 1 << 2;
                    }
                }
            }

            if ((piece.additionalData & 1) != 0) {
                for (int zx = 0; zx <= length; zx++) {
                    int[] rp = rotateLocal(piece, 1, zx);
                    if (rp[0] >= cx && rp[0] < cx + 16 && rp[1] >= cz && rp[1] < cz + 16) {
                        maybeGenerateBlock(rng);
                    }
                }
            }
        }
    }

    private static void consumeSupportCobweb(WorldgenRandom rng, MSPiece p, int cx, int cz, int sectionZ) {

        int supportRng = rng.nextInt(4);

        if (isInChunk(p, cx, cz, 0, sectionZ) || isInChunk(p, cx, cz, 2, sectionZ)) {
            if (supportRng != 0) {
                maybeGenerateBlock(rng);
                maybeGenerateBlock(rng);
            }
        }

        int[][] cobwebPoses = {
                {0, sectionZ - 1}, {2, sectionZ - 1}, {0, sectionZ + 1}, {2, sectionZ + 1},
                {0, sectionZ - 2}, {2, sectionZ - 2}, {0, sectionZ + 2}, {2, sectionZ + 2}
        };
        for (int[] cwp : cobwebPoses) {
            if (isInChunk(p, cx, cz, cwp[0], cwp[1])) {
                maybeGenerateBlock(rng);
            }
        }
    }

    private static boolean isInChunk(MSPiece p, int cx, int cz, int localX, int localZ) {
        int[] rp = rotateLocal(p, localX, localZ);
        return rp[0] >= cx && rp[0] < cx + 16 && rp[1] >= cz && rp[1] < cz + 16;
    }

    private static void maybeGenerateBlock(WorldgenRandom rng) {

        rng.nextFloat();
    }

    private static int[] rotateLocal(MSPiece p, int localX, int localZ) {
        return switch (p.rot) {
            case 0 -> new int[]{p.bbMinX + localX, p.bbMaxZ - localZ};
            case 1 -> new int[]{p.bbMinX + localZ, p.bbMinZ + localX};
            case 2 -> new int[]{p.bbMinX + localX, p.bbMinZ + localZ};
            case 3 -> new int[]{p.bbMaxX - localZ, p.bbMinZ + localX};
            default -> new int[]{localX, localZ};
        };
    }

    private static boolean hasCollision(List<MSPiece> pieces, int minX, int minY, int minZ,
                                        int maxX, int maxY, int maxZ) {
        for (MSPiece p : pieces) {
            if (maxX >= p.bbMinX && minX <= p.bbMaxX &&
                    maxY >= p.bbMinY && minY <= p.bbMaxY &&
                    maxZ >= p.bbMinZ && minZ <= p.bbMaxZ) return true;
        }
        return false;
    }

    @PublicAPI
    public static List<StructurePieceSimulation.ChestInfo> getMineshaftChests(
            long worldSeed, int chunkX, int chunkZ,
            List<MSPiece> pieces,
            PopulationSeedUtils.StructureSaltConfig ssconf) {
        return getMineshaftChests(worldSeed, chunkX, chunkZ);
    }

    public static class MSPiece {
        public int type;
        public int posX, posY, posZ;
        public int bbMinX, bbMinY, bbMinZ;
        public int bbMaxX, bbMaxY, bbMaxZ;
        public int rot;
        public int depth;
        public int additionalData;

        public MSPiece(int type, int x, int y, int z) {
            this.type = type;
            this.posX = x;
            this.posY = y;
            this.posZ = z;
            this.bbMinX = x;
            this.bbMinY = y;
            this.bbMinZ = z;
            this.bbMaxX = x;
            this.bbMaxY = y;
            this.bbMaxZ = z;
        }
    }
}

