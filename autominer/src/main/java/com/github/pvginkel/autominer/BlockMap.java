package com.github.pvginkel.autominer;

import com.github.pvginkel.autominer.nbt.Block;
import com.github.pvginkel.autominer.nbt.Blocks;
import com.github.pvginkel.autominer.nbt.SectionData;
import com.github.pvginkel.autominer.support.Rectangle;
import com.github.pvginkel.autominer.support.Vector;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BlockMap {
    public static BlockMap build(List<Chunk> chunks, Rectangle box, int minY, int maxY) {
        Vector offset = new Vector(box.getX1(), minY, box.getY1());
        int cx = box.getX2() - box.getX1() + 1;
        int cy = maxY - minY + 1;
        int cz = box.getY2() - box.getY1() + 1;

        byte[] blocks = new byte[cx * cy * cz];

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);

            SectionData sectionData = new SectionData(chunk.getBlocks());

            int bx = chunk.getPosition().getX();
            int by = chunk.getPosition().getY();
            int bz = chunk.getPosition().getZ();

            for (int ly = 0; ly < 16; ly++) {
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        int x = bx * 16 + lx;
                        int y = by * 16 + ly;
                        int z = bz * 16 + lz;

                        minX = Math.min(x, minX);
                        maxX = Math.max(x, maxX);
                        minZ = Math.min(z, minZ);
                        maxZ = Math.max(z, maxZ);

                        if (
                            x >= offset.getX() && y >= offset.getY() && z >= offset.getZ() &&
                            x < (offset.getX() + cx) && y < (offset.getY() + cy) && z < (offset.getZ() + cz)
                        ) {
                            int index = (x - offset.getX()) * cy * cz + (y - offset.getY()) * cz + (z - offset.getZ());
                            blocks[index] = sectionData.getType(lx, ly, lz);
                        }
                    }
                }
            }
        }

        return new BlockMap(blocks, offset, cx, cy, cz);
    }

    private final byte[] blocks;
    private final Vector offset;
    private final int cx;
    private final int cy;
    private final int cz;

    private BlockMap(byte[] blocks, Vector offset, int cx, int cy, int cz) {
        this.blocks = blocks;
        this.offset = offset;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
    }

    public Vector getOffset() {
        return offset;
    }

    public int getCx() {
        return cx;
    }

    public int getCy() {
        return cy;
    }

    public int getCz() {
        return cz;
    }

    public Block get(int x, int y, int z) {
        int index = getIndex(x, y, z);
        if (index == -1) {
            return Blocks.UNKNOWN;
        }

        return Block.get(blocks[index]);
    }

    public void set(int x, int y, int z, Block block) {
        blocks[getIndex(x, y, z)] = block.getType();
    }

    private int getIndex(int x, int y, int z) {
        x -= offset.getX();
        y -= offset.getY();
        z -= offset.getZ();

        if (x < 0 || x >= cx || y < 0 || y >= cy || z < 0 || z >= cz) {
            return -1;
        }

        return x * cy * cz + y * cz + z;
    }

    public void save(File file) throws IOException {
        try (DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))))) {
            os.writeInt(offset.getX());
            os.writeInt(offset.getY());
            os.writeInt(offset.getZ());
            os.writeInt(cx);
            os.writeInt(cy);
            os.writeInt(cz);
            os.writeInt(blocks.length);
            os.write(blocks, 0, blocks.length);
        }
    }

    public static BlockMap load(File file) throws IOException {
        Vector offset;
        int cx;
        int cy;
        int cz;
        byte[] blocks;

        try (DataInputStream is = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))))) {
            offset = new Vector(is.readInt(), is.readInt(), is.readInt());
            cx = is.readInt();
            cy = is.readInt();
            cz = is.readInt();
            int length = is.readInt();
            blocks = new byte[length];

            int read = 0;
            while (read < blocks.length) {
                read += is.read(blocks, read, blocks.length - read);
            }
        }

        return new BlockMap(blocks, offset, cx, cy, cz);
    }

    public BlockMap clone() {
        return new BlockMap(
            Arrays.copyOf(blocks, blocks.length),
            offset,
            cx,
            cy,
            cz
        );
    }
}
