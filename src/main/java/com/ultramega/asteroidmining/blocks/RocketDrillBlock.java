package com.ultramega.asteroidmining.blocks;

public class RocketDrillBlock extends AbstractMultiblockBlock {
    public RocketDrillBlock(final Properties properties, final DrillType type) {
        super(properties, type.getWidth(), type.getHeight());
    }

    public enum DrillType {
        IRON(1, 1),
        DIAMOND(1, 2),
        EMERALD(1, 2),
        NETHERITE(1, 2);

        private final int width;
        private final int height;

        DrillType(final int width, final int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }
    }
}
