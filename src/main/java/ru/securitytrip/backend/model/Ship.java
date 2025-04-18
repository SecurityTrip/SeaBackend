package ru.securitytrip.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ship {
    private int size;
    private int x;
    private int y;
    private boolean isHorizontal;
    private List<Boolean> hits = new ArrayList<>();
    
    public Ship(int size, int x, int y, boolean isHorizontal) {
        this.size = size;
        this.x = x;
        this.y = y;
        this.isHorizontal = isHorizontal;
        
        // Инициализируем список попаданий (false - целая часть корабля)
        for (int i = 0; i < size; i++) {
            hits.add(false);
        }
    }
    
    // Проверяет, потоплен ли корабль (все части поражены)
    public boolean isSunk() {
        return !hits.contains(false);
    }
    
    // Пытается поразить корабль по указанным координатам и возвращает true, если попадание успешно
    public boolean hit(int hitX, int hitY) {
        if (isHorizontal) {
            if (hitY == y && hitX >= x && hitX < x + size) {
                hits.set(hitX - x, true);
                return true;
            }
        } else {
            if (hitX == x && hitY >= y && hitY < y + size) {
                hits.set(hitY - y, true);
                return true;
            }
        }
        return false;
    }
    
    // Получает все координаты корабля
    public List<int[]> getCoordinates() {
        List<int[]> coordinates = new ArrayList<>();
        
        if (isHorizontal) {
            for (int i = 0; i < size; i++) {
                coordinates.add(new int[] {x + i, y});
            }
        } else {
            for (int i = 0; i < size; i++) {
                coordinates.add(new int[] {x, y + i});
            }
        }
        
        return coordinates;
    }
} 