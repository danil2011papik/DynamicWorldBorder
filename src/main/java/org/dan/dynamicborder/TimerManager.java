package org.dan.dynamicborder;

import java.util.HashMap;
import java.util.Map;

public class TimerManager {
    private final Map<String, Timer> activeTimers = new HashMap<>();

    public TimerManager() {
        // конструктор
    }

    public void checkTimers() {
        // проверка таймеров
    }

    class Timer {
        private String name;
        private int timeLeft;

        public Timer(String name, int duration) {
            this.name = name;
            this.timeLeft = duration;
        }
    }
}