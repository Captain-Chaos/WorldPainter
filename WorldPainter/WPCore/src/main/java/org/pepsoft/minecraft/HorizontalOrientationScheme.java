package org.pepsoft.minecraft;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.util.MathUtils.mod;

/**
 * Horizontal orientation schemes for materials supported by {@link Material}.
 */
enum HorizontalOrientationScheme {
    /**
     * {@code axis} property containing {@code x}, {@code y} or {@code z}
     */
    AXIS {
        @Override
        public Material rotate(Material material, int steps) {
            if (steps % 2 != 0) {
                String axis = material.getProperty(MC_AXIS);
                if (axis.equals("x")) {
                    return material.withProperty(MC_AXIS, "z");
                } else if (axis.equals("z")) {
                    return material.withProperty(MC_AXIS, "x");
                }
            }
            return material;
        }

        @Override
        public Direction getDirection(Material material) {
            switch (material.getProperty(Material.AXIS)) {
                case "x":
                    return Direction.EAST;
                case "z":
                    return Direction.SOUTH;
                default:
                    return null;
            }
        }

        @Override
        public Material setDirection(Material material, Direction direction) {
            switch (direction) {
                case NORTH:
                case SOUTH:
                    return material.withProperty(Material.AXIS, "z");
                case EAST:
                case WEST:
                    return material.withProperty(Material.AXIS, "x");
                default:
                    throw new InternalError();
            }
        }
    },

    /**
     * {@code north}, {@code east}, {@code south} <em>and</em> {@code west} properties, each containing some arbitrary value
     */
    CARDINAL_DIRECTIONS {
        @Override
        public Material mirror(Material material, Direction axis) {
            String north = material.getProperty(MC_NORTH);
            String east = material.getProperty(MC_EAST);
            String south = material.getProperty(MC_SOUTH);
            String west = material.getProperty(MC_WEST);
            if ((axis == Direction.WEST) || (axis == Direction.EAST)) {
                // Axis runs east-west, so mirror north to south and vice versa
                String tmp = north;
                north = south;
                south = tmp;
            } else {
                // Axis runs north-south, so mirror east to west and vice versa
                String tmp = west;
                west = east;
                east = tmp;
            }
            return material.withProperties(
                    MC_NORTH, north,
                    MC_EAST, east,
                    MC_SOUTH, south,
                    MC_WEST, west
            );
        }

        @Override
        public Material rotate(Material material, int steps) {
            String[] directions = {
                    material.getProperty(MC_NORTH),
                    material.getProperty(MC_EAST),
                    material.getProperty(MC_SOUTH),
                    material.getProperty(MC_WEST)
            };
            Collections.rotate(asList(directions), steps);
            return material.withProperties(
                    MC_NORTH, directions[0],
                    MC_EAST, directions[1],
                    MC_SOUTH, directions[2],
                    MC_WEST, directions[3]
            );
        }
    },

    /**
     * {@code facing} property containing the cardinal direction {@code north}, {@code east}, {@code south} or {@code west}
     */
    FACING {
        @Override
        public Material mirror(Material material, Direction axis) {
            return material.withProperty(Material.FACING, material.getProperty(Material.FACING).mirror(axis));
        }

        @Override
        public Material rotate(Material material, int steps) {
            return material.withProperty(Material.FACING, material.getProperty(Material.FACING).rotate(steps));
        }

        @Override
        public Direction getDirection(Material material) {
            return material.getProperty(Material.FACING);
        }

        @Override
        public Material setDirection(Material material, Direction direction) {
            return material.withProperty(Material.FACING, direction);
        }
    },

    /**
     * {code rotation} property containing an integer from 0 to 15 (inclusive) where 0 is south, 4 is west, etc.
     */
    ROTATION {
        @Override
        public Material mirror(Material material, Direction axis) {
            switch (axis) {
                case NORTH:
                case SOUTH:
                    // Axis runs north-south, so mirror east to west and vice versa
                    return material.withProperty(Material.ROTATION, 16 - material.getProperty(Material.ROTATION));
                case EAST:
                case WEST:
                    // Axis runs east-west, so mirror north to south and vice versa
                    int rotation = material.getProperty(Material.ROTATION);
                    if ((rotation > 0) && (rotation < 8)) {
                        return material.withProperty(Material.ROTATION, 8 - rotation);
                    } else if (rotation > 8) {
                        return material.withProperty(Material.ROTATION, 24 - rotation);
                    } else {
                        return material;
                    }
                default:
                    throw new InternalError();
            }
        }

        @Override
        public Material rotate(Material material, int steps) {
            return material.withProperty(Material.ROTATION, mod(material.getProperty(Material.ROTATION) + steps * 4, 16));
        }

        @Override
        public Direction getDirection(Material material) {
            switch ((material.getProperty(Material.ROTATION) + 1) / 4) {
                case 0:
                    return Direction.SOUTH;
                case 1:
                    return Direction.WEST;
                case 2:
                    return Direction.NORTH;
                case 3:
                    return Direction.EAST;
                default:
                    return null;
            }
        }

        @Override
        public Material setDirection(Material material, Direction direction) {
            switch (direction) {
                case SOUTH:
                    return material.withProperty(Material.ROTATION, 0);
                case WEST:
                    return material.withProperty(Material.ROTATION, 4);
                case NORTH:
                    return material.withProperty(Material.ROTATION, 8);
                case EAST:
                    return material.withProperty(Material.ROTATION, 12);
                default:
                    throw new InternalError();
            }
        }
    },

    /**
     * {@code shape} property containing {@code north_south}, {@code east_west}, etc. as used by Minecraft for rails
     */
    SHAPE {
        @Override
        public Material mirror(Material material, Direction axis) {
            switch (axis) {
                case NORTH:
                case SOUTH:
                    // Axis runs north-south, so mirror east to west and vice versa
                    switch (material.getProperty(Material.SHAPE)) {
                        case "south_west":
                            return material.withProperty(Material.SHAPE, "south_east");
                        case "north_west":
                            return material.withProperty(Material.SHAPE, "northeast");
                        case "north_east":
                            return material.withProperty(Material.SHAPE, "north_west");
                        case "south_east":
                            return material.withProperty(Material.SHAPE, "south_west");
                        case "ascending_east":
                            return material.withProperty(Material.SHAPE, "ascending_west");
                        case "ascending_west":
                            return material.withProperty(Material.SHAPE, "ascending_east");
                        default:
                            return material;
                    }
                case EAST:
                case WEST:
                    // Axis runs east-west, so mirror north to south and vice versa
                    switch (material.getProperty(Material.SHAPE)) {
                        case "south_west":
                            return material.withProperty(Material.SHAPE, "north_west");
                        case "north_west":
                            return material.withProperty(Material.SHAPE, "south_west");
                        case "north_east":
                            return material.withProperty(Material.SHAPE, "south_east");
                        case "south_east":
                            return material.withProperty(Material.SHAPE, "north_east");
                        case "ascending_north":
                            return material.withProperty(Material.SHAPE, "ascending_south");
                        case "ascending_south":
                            return material.withProperty(Material.SHAPE, "ascending_north");
                        default:
                            return material;
                    }
                default:
                    throw new InternalError();
            }
        }

        @Override
        public Material rotate(Material material, int steps) {
            int index = asList(SHAPE_VALUES).indexOf(material.getProperty(Material.SHAPE));
            return material.withProperty(Material.SHAPE, SHAPE_VALUES[mod(index + steps * 3, 12)]);
        }

        private final String[] SHAPE_VALUES = {
                "south_west", "ascending_north", "east_west",
                "north_west", "ascending_east", "north_south",
                "north_east", "ascending_south", "east_west",
                "south_east", "ascending_west", "north_south"};
    },

    /**
     * {@code shape} property set to {@code inner_left}, {@code inner_right},
     * {@code outer_left} or {@code outer_right} as used by e.g. Minecraft stair
     * corner blocks.
     */
    STAIR_CORNER {
        @Override
        public Material mirror(Material material, Direction axis) {
            switch (material.getProperty(Material.SHAPE)) {
                case "inner_left":
                    return material.withProperty(Material.SHAPE, "inner_right");
                case "inner_right":
                    return material.withProperty(Material.SHAPE, "inner_left");
                case "outer_left":
                    return material.withProperty(Material.SHAPE, "outer_right");
                case "outer_right":
                    return material.withProperty(Material.SHAPE, "outer_left");
                default:
                    return material;
            }
        }
    },

    /**
     * {@code type} property containing {@code left} or {@code right}.
     */
    TYPE {
        @Override
        public Material mirror(Material material, Direction axis) {
            if (material.getProperty(Material.TYPE).equals("left")) {
                return material.withProperty(Material.TYPE, "right");
            } else {
                return material.withProperty(Material.TYPE, "left");
            }
        }
    },

    /**
     * {@code hinge} property containing {@code left} or {@code right}.
     */
    HINGE {
        @Override
        public Material mirror(Material material, Direction axis) {
            if (material.getProperty(Material.HINGE).equals("left")) {
                return material.withProperty(Material.HINGE, "right");
            } else {
                return material.withProperty(Material.HINGE, "left");
            }
        }
    };

    public Material mirror(Material material, Direction axis) {
        return material;
    }

    public Material rotate(Material material, int steps) {
        return material;
    }

    public Direction getDirection(Material material) {
        return null;
    }

    public Material setDirection(Material material, Direction direction) {
        return material;
    }
}