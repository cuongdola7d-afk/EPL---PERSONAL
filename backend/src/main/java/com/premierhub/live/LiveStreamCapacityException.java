package com.premierhub.live;

public class LiveStreamCapacityException extends RuntimeException {
    private final int maxConnections;

    public LiveStreamCapacityException(int maxConnections) {
        super("Hệ thống đã đạt giới hạn kết nối trực tiếp tối đa (" + maxConnections + "). Vui lòng thử lại sau.");
        this.maxConnections = maxConnections;
    }

    public int maxConnections() {
        return maxConnections;
    }
}
