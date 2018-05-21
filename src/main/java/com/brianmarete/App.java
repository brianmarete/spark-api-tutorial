package com.brianmarete;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.brianmarete.models.WeatherResult;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import static spark.debug.DebugScreen.enableDebugScreen;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.port;
import static spark.Spark.staticFileLocation;

import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;


public class App {
    public static WeatherResult processWeatherResults(Response response) {
        WeatherResult result = null;

        try {
            String jsonData = response.body().string();

            if (response.isSuccessful()) {
                JSONObject responseJson = new JSONObject(jsonData);

                Gson gson = new GsonBuilder().create();
                result = gson.fromJson(responseJson.toString(), WeatherResult.class);
            }
        } catch (JSONException | NullPointerException | IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void main(String[] args) {
        OkHttpClient client = new OkHttpClient();

        ProcessBuilder process = new ProcessBuilder();
        Integer port;

        if (process.environment().get("PORT") != null) {
            port = Integer.parseInt(process.environment().get("PORT"));
        } else {
            port = 4567;
        }

        port(port);
        enableDebugScreen();

        // staticFileLocation("/public");
        String layout = "templates/layout.vtl";


        get("/", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("template", "templates/index.vtl");
            return new VelocityTemplateEngine().render(new ModelAndView(model, layout));
        });

        post("/", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            HttpUrl.Builder urlBuilder = HttpUrl.parse(Constants.WEATHER_BASE_URL).newBuilder();
            urlBuilder.addQueryParameter(Constants.LOCATION_PARAMETER, req.queryParams("city"));
            urlBuilder.addQueryParameter(Constants.API_KEY_PARAMETER, Constants.API_KEY);
            urlBuilder.addQueryParameter(Constants.UNITS_PARAMETER, Constants.UNITS);

            String url = urlBuilder.build().toString();

            Request request = new Request.Builder()
                .url(url)
                .build();

            try (Response response = client.newCall(request).execute()) {
                WeatherResult result = processWeatherResults(response);
                if (result != null) {
                    model.put("result", result);
                }
            } catch(IOException e) {
                e.getStackTrace();
            }

            model.put("template", "templates/weather.vtl");
            return new VelocityTemplateEngine().render(new ModelAndView(model, layout));
        });
    }
}
