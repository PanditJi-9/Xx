package dev.redlab.gamespace.recycler;

import android.graphics.Point;

/* loaded from: classes.dex */
public class QuadrangleHelper {
    public static boolean isPointInQuadrangle(Point point, Point point2, Point point3, Point point4, Point point5) {
        return ((triangleArea(point, point2, point5) + triangleArea(point2, point3, point5)) + triangleArea(point3, point4, point5)) + triangleArea(point4, point, point5) == triangleArea(point, point2, point3) + triangleArea(point3, point4, point);
    }

    public static boolean isPointInQuadrangle(Point[] pointArr, Point point) {
        if (pointArr == null || pointArr.length < 4) {
            return false;
        }
        return isPointInQuadrangle(pointArr[0], pointArr[1], pointArr[2], pointArr[3], point);
    }

    private static double triangleArea(Point point, Point point2, Point point3) {
        return Math.abs(((((((point.x * point2.y) + (point2.x * point3.y)) + (point3.x * point.y)) - (point2.x * point.y)) - (point3.x * point2.y)) - (point.x * point3.y)) / 2.0d);
    }
}
