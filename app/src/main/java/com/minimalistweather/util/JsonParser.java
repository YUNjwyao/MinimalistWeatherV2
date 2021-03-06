package com.minimalistweather.util;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.minimalistweather.database_entity.City;
import com.minimalistweather.database_entity.District;
import com.minimalistweather.database_entity.Province;
import com.minimalistweather.gson_entity.HeWeatherAirQuality;
import com.minimalistweather.gson_entity.HeWeatherForecast;
import com.minimalistweather.gson_entity.HeWeatherLifestyle;
import com.minimalistweather.gson_entity.HeWeatherNow;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonParser {
    /**
     * 解析和处理服务器返回的省级数据
     */
    public static boolean parseProvinceResponse(String response) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allProvinces = new JSONArray(response);
                for (int i = 0; i < allProvinces.length(); i++) {
                    JSONObject provinceObject = allProvinces.getJSONObject(i);
                    Province province = new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的市级数据
     */
    public static boolean parseCityResponse(String response, int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCities = new JSONArray(response);
                for (int i = 0; i < allCities.length(); i++) {
                    JSONObject cityObject = allCities.getJSONObject(i);
                    City city = new City();
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的县级数据
     */
    public static boolean parseDistrictResponse(String response, int cityId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCounties = new JSONArray(response);
                for (int i = 0; i < allCounties.length(); i++) {
                    JSONObject countyObject = allCounties.getJSONObject(i);
                    District district = new District();
                    district.setDistrictName(countyObject.getString("name"));
                    district.setWeatherId(countyObject.getString("weather_id"));
                    district.setCityId(cityId);
                    district.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 将返回的JSON数据解析成HeWeatherNow对象
     * @param response
     * @return
     */
    public static HeWeatherNow parseWeatherNowResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather6");
            String weatherNowContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherNowContent, HeWeatherNow.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /***
     * 将返回的JSON数据解析成HeWeatherForecast对象
     * @param response
     * @return
     */
    public static HeWeatherForecast parseWeatherForecastResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather6");
            String weatherForecastContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherForecastContent, HeWeatherForecast.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /***
     * 将返回的JSON数据解析成HeWeatherLifestyle对象
     * @param response
     * @return
     */
    public static HeWeatherLifestyle parseWeatherLifestyleResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather6");
            String weatherLifestyleContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherLifestyleContent, HeWeatherLifestyle.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将JSON数据解析成HeWeatherAirQuality对象
     * @param response
     * @return
     */
    public static HeWeatherAirQuality parseWeatherAirQuality(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather6");
            String weatherAirQualityContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherAirQualityContent, HeWeatherAirQuality.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
