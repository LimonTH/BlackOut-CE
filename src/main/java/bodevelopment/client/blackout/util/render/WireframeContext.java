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

package bodevelopment.client.blackout.util.render;

import bodevelopment.client.blackout.annotations.Internal;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@Internal
public class WireframeContext {
    public final List<Vec3[]> lines = new ArrayList<>();
    public final List<Vec3[]> quads = new ArrayList<>();

    public static WireframeContext of(List<Vec3[]> positions) {
        WireframeContext context = new WireframeContext();
        context.quads.addAll(positions);

        for (Vec3[] arr : positions) {
            for (int i = 0; i < 4; i++) {
                Vec3 v1 = arr[i];
                Vec3 v2 = arr[(i + 1) % 4];

                Vec3[] line = new Vec3[]{v1, v2};
                if (!contains(context.lines, line)) {
                    context.lines.add(line);
                }
            }
        }
        return context;
    }

    protected static boolean contains(List<Vec3[]> list, Vec3[] line) {
        for (Vec3[] arr : list) {
            if (arr[0].equals(line[0]) && arr[1].equals(line[1])) {
                return true;
            }

            if (arr[0].equals(line[1]) && arr[1].equals(line[0])) {
                return true;
            }
        }

        return false;
    }
}
