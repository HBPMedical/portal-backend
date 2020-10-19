package eu.hbp.mip.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import java.lang.reflect.Type;

public class JsonConverters {
    Gson gson = new Gson();

    public static String convertObjectToJsonString(Object object)  {
        ObjectMapper mapper = new ObjectMapper();
        //Converting the Object to JSONString
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return e.getMessage();
        }
    }

    public static  <T> T convertJsonStringToObject(String jsonString, Type typeOfT)  {
        if(jsonString == null || jsonString.isEmpty())
            return null;
        return new Gson().fromJson(jsonString, typeOfT);
    }
}
