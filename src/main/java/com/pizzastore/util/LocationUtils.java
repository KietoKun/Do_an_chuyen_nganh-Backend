package com.pizzastore.util;

public class LocationUtils {

    // Bán kính trung bình của Trái Đất tính bằng Kilomet
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Tính khoảng cách đường chim bay giữa 2 điểm tọa độ (Haversine Formula)
     * @return Khoảng cách tính bằng Kilomet (km)
     */
    public static double calculateDistance(double startLat, double startLong, double endLat, double endLong) {

        // Chuyển đổi tọa độ độ (degree) sang radian
        double dLat = Math.toRadians(endLat - startLat);
        double dLong = Math.toRadians(endLong - startLong);

        startLat = Math.toRadians(startLat);
        endLat = Math.toRadians(endLat);

        // Áp dụng công thức Haversine
        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.pow(Math.sin(dLong / 2), 2) * Math.cos(startLat) * Math.cos(endLat);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Trả về kết quả đã làm tròn đến 2 chữ số thập phân (Ví dụ: 2.35 km)
        double distance = EARTH_RADIUS_KM * c;
        return Math.round(distance * 100.0) / 100.0;
    }
}