/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Direction;
import org.pepsoft.worldpainter.heightMaps.TransformingHeightMap;

import javax.vecmath.Point3i;
import java.awt.*;

/**
 *
 * @author pepijn
 */
public abstract class CoordinateTransform {
    public Point transform(int x, int y) {
        Point rc = new Point(x, y);
        transformInPlace(rc);
        return rc;
    }

    public Point3i transform(int x, int y, int z) {
        Point3i rc = new Point3i(x, y, z);
        transformInPlace(rc);
        return rc;
    }
    
    public final Point transform(Point coords) {
        return transform(coords.x, coords.y);
    }
    
    public final Point3i transform(Point3i coords) {
        return transform(coords.x, coords.y, coords.z);
    }
    
    public abstract void transformInPlace(Point coords);
    
    public abstract void transformInPlace(Point3i coords);
    
    public Rectangle transform(Rectangle rectangle) {
        Point corner1 = rectangle.getLocation();
        Point corner2 = new Point(rectangle.x + rectangle.width - 1, rectangle.y + rectangle.height - 1);
        transformInPlace(corner1);
        transformInPlace(corner2);
        return new Rectangle(Math.min(corner1.x, corner2.x), Math.min(corner1.y, corner2.y), Math.abs(corner2.x - corner1.x) + 1, Math.abs(corner2.y - corner1.y) + 1);
    }
    
    public abstract Direction transform(Direction direction);
    
    public abstract Direction inverseTransform(Direction direction);
    
    public abstract float transformAngle(float angle);

    public abstract float transformScalar(float scalar);

    public abstract HeightMap transform(HeightMap heightMap);

    public boolean isScaling() {
        return false;
    }

    public float getScale() {
        return 1.0f;
    }

    public static CoordinateTransform getScalingInstance(float scale) {
        if (scale == 1.0f) {
            return NOOP;
        } else {
            return new CoordinateTransform() {
                @Override
                public void transformInPlace(Point coords) {
                    coords.x = Math.round(coords.x * scale);
                    coords.y = Math.round(coords.y * scale);
                }

                @Override
                public void transformInPlace(Point3i coords) {
                    coords.x = Math.round(coords.x * scale);
                    coords.y = Math.round(coords.y * scale);
                }

                @Override
                public Direction transform(Direction direction) {
                    return direction;
                }

                @Override
                public Direction inverseTransform(Direction direction) {
                    return direction;
                }

                @Override
                public float transformAngle(float angle) {
                    return angle;
                }

                @Override
                public float transformScalar(float scalar) {
                    return scalar * scale;
                }

                @Override
                public HeightMap transform(HeightMap heightMap) {
                    return heightMap.scaled(scale);
                }

                @Override
                public boolean isScaling() {
                    return true;
                }

                @Override
                public float getScale() {
                    return scale;
                }
            };
        }
    }

    public static final CoordinateTransform ROTATE_CLOCKWISE_90_DEGREES = new CoordinateTransform() {
        @Override
        public Point transform(int x, int y) {
            return new Point(-y - 1, x);
        }

        @Override
        public Point3i transform(int x, int y, int z) {
            return new Point3i(-y - 1, x, z);
        }
        
        @Override
        public void transformInPlace(Point coords) {
            int tmp = coords.x;
            coords.x = -coords.y - 1;
            coords.y = tmp;
        }

        @Override
        public void transformInPlace(Point3i coords) {
            int tmp = coords.x;
            coords.x = -coords.y - 1;
            coords.y = tmp;
        }
        
        @Override
        public Direction transform(Direction direction) {
            return direction.right();
        }

        @Override
        public Direction inverseTransform(Direction direction) {
            return direction.left();
        }
        
        @Override
        public float transformAngle(float angle) {
            angle = angle - HALF_PI;
            while (angle < 0) {
                angle += TWO_PI;
            }
            return angle;
        }

        @Override
        public float transformScalar(float scalar) {
            return scalar;
        }

        @Override
        public HeightMap transform(HeightMap heightMap) {
            return TransformingHeightMap.build().withHeightMap(heightMap).withName(heightMap.getName()).withRotation(HALF_PI).now();
        }
    };

    public static final CoordinateTransform ROTATE_180_DEGREES = new CoordinateTransform() {
        @Override
        public Point transform(int x, int y) {
            return new Point(-x - 1, -y - 1);
        }

        @Override
        public Point3i transform(int x, int y, int z) {
            return new Point3i(-x - 1, -y - 1, z);
        }

        @Override
        public void transformInPlace(Point coords) {
            coords.x = -coords.x - 1;
            coords.y = -coords.y - 1;
        }

        @Override
        public void transformInPlace(Point3i coords) {
            coords.x = -coords.x - 1;
            coords.y = -coords.y - 1;
        }
        
        @Override
        public Direction transform(Direction direction) {
            return direction.opposite();
        }

        @Override
        public Direction inverseTransform(Direction direction) {
            return direction.opposite();
        }

        @Override
        public float transformAngle(float angle) {
            angle = angle + PI;
            while (angle >= TWO_PI) {
                angle -= TWO_PI;
            }
            return angle;
        }

        @Override
        public float transformScalar(float scalar) {
            return scalar;
        }

        @Override
        public HeightMap transform(HeightMap heightMap) {
            return TransformingHeightMap.build().withHeightMap(heightMap).withName(heightMap.getName()).withRotation(PI).now();
        }
    };

    public static final CoordinateTransform ROTATE_CLOCKWISE_270_DEGREES = new CoordinateTransform() {
        @Override
        public Point transform(int x, int y) {
            return new Point(y, -x - 1);
        }

        @Override
        public Point3i transform(int x, int y, int z) {
            return new Point3i(y, -x - 1, z);
        }

        @Override
        public void transformInPlace(Point coords) {
            int tmp = -coords.x - 1;
            coords.x = coords.y;
            coords.y = tmp;
        }

        @Override
        public void transformInPlace(Point3i coords) {
            int tmp = -coords.x - 1;
            coords.x = coords.y;
            coords.y = tmp;
        }

        @Override
        public Direction transform(Direction direction) {
            return direction.left();
        }

        @Override
        public Direction inverseTransform(Direction direction) {
            return direction.right();
        }

        @Override
        public float transformAngle(float angle) {
            angle = angle + HALF_PI;
            while (angle >= TWO_PI) {
                angle -= TWO_PI;
            }
            return angle;
        }

        @Override
        public float transformScalar(float scalar) {
            return scalar;
        }

        @Override
        public HeightMap transform(HeightMap heightMap) {
            return TransformingHeightMap.build().withHeightMap(heightMap).withName(heightMap.getName()).withRotation(PI * 3 / 2).now();
        }
    };

    public static final CoordinateTransform NOOP = new CoordinateTransform() {
        @Override
        public Point transform(int x, int y) {
            return new Point(x, y);
        }

        @Override
        public Point3i transform(int x, int y, int z) {
            return new Point3i(x, y, z);
        }

        @Override
        public void transformInPlace(Point coords) {
            // Do nothing
        }

        @Override
        public void transformInPlace(Point3i coords) {
            // Do nothing
        }

        @Override
        public Rectangle transform(Rectangle rectangle) {
            return rectangle;
        }

        @Override
        public Direction transform(Direction direction) {
            return direction;
        }

        @Override
        public Direction inverseTransform(Direction direction) {
            return direction;
        }

        @Override
        public float transformAngle(float angle) {
            return angle;
        }

        @Override
        public float transformScalar(float scalar) {
            return scalar;
        }

        @Override
        public HeightMap transform(HeightMap heightMap) {
            return heightMap;
        }

        @Override
        public boolean isScaling() {
            return false;
        }
    };

    private static final float HALF_PI = (float) (Math.PI / 2);
    private static final float PI      = (float)  Math.PI;
    private static final float TWO_PI  = (float) (Math.PI * 2);
}