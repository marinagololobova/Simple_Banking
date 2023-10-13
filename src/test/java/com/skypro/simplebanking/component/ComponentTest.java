package com.skypro.simplebanking.component;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class ComponentTest {

    public static JSONObject createUser() throws JSONException {
        JSONObject jsonUser = new JSONObject();
        jsonUser.put("id", "1");
        jsonUser.put("username", "user1");
        jsonUser.put("password", "user1");
        return jsonUser;
    }

    public static String getAuthenticationHeader(String username, String password) {

        String encoding = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoding;
    }

    public JSONObject getUser() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", "userName");
        jsonObject.put("password", "password");
        return jsonObject;
    }
}
