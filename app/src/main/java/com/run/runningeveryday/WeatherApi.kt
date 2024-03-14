package com.run.runningeverytime.api

import com.run.runningeveryday.api.ApiKey.Companion.API_KEY
import com.run.runningeveryday.model.Weather
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("getVilageFcst?serviceKey=$API_KEY")
    fun getWeather(
        @Query("dataType") dataType : String,
        @Query("numOfRows") numOfRows : Int,
        @Query("pageNo") pageNo : Int,
        @Query("base_date") baseDate : String,
        @Query("base_time") baseTime : String,
        @Query("nx") nx : String,
        @Query("ny") ny : String
    ) : Call<Weather>
}