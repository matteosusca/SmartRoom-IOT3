package com.example.smartroom.utils

import java.util.Queue
import java.util.ArrayDeque

class Graph(
    val maxSize: Int
) {
    // Una struttura di tipo FIFO in cui inserire i dati
    private val queue: Queue<Double> = ArrayDeque(maxSize)

    // Aggiunge un elemento alla coda
    fun add(element: Double) {
        if (queue.size == maxSize) {
            queue.remove()
        }
        queue.add(element)
    }

    // Restituisce la coda sotto forma di lista
    fun getValues(): List<Double> {
        return queue.toList()
    }

    fun getMinMaxValues(default: Double): Pair<Double, Double> {
        val list = this.getValues()
        if (list.isEmpty()) return Pair(default, default)
        return Pair(list.min(), list.max())
    }

    // Restituisce la dimensione della coda
    fun getSize(): Int {
        return queue.size
    }
}