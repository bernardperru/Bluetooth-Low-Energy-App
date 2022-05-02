package com.example.igen


data class Positions (val distances: HashMap<String, Double>,
                      val oldPosition: Vector2,
                      val oldAveragePosition: Vector2,
                      val position: Vector2)