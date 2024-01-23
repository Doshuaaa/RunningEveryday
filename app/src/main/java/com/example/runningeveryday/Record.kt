package com.example.runningeveryday

class Record() {

    fun timeFormat(time: Long) : String {
        return String.format("%2d : %2d", time / 60, time % 60)
    }

    fun getGrade(sex: String, age: Int, time: Long) {

        when(sex) {

            "남" -> {

            }

            "여" -> {

            }
        }
    }
}