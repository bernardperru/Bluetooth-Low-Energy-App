package com.example.igen

//Class made to convert JSON response from API to a class
data class Positions (val distances: HashMap<String, Double>, val position: Vector2)