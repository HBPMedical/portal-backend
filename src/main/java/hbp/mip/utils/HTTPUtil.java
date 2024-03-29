package hbp.mip.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HTTPUtil {

    private HTTPUtil() {
        /* Hide implicit public constructor */
        throw new IllegalAccessError("HTTPUtil class");
    }

    public static void sendGet(String url, StringBuilder resp) throws IOException {
        sendHTTP(url, "", resp, "GET", null);
    }

    public static int sendPost(String url, String query, StringBuilder resp) throws IOException {
        return sendHTTP(url, query, resp, "POST", null);
    }

    private static int sendHTTP(String url, String query, StringBuilder resp, String httpVerb, String authorization)
            throws IOException {

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        if (authorization != null) {
            con.setRequestProperty("Authorization", authorization);
        }

        if (!"GET".equals(httpVerb)) {
            con.setRequestMethod(httpVerb);
            if (query != null && !query.isEmpty()) {
                con.addRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Content-Length", Integer.toString(query.length()));

                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.write(query.getBytes(StandardCharsets.UTF_8));
                wr.flush();
                wr.close();
            }
        }

        int respCode = con.getResponseCode();

        BufferedReader in;
        if (respCode == 200) {
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        } else {
            in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
        }
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        resp.append(response);

        return respCode;
    }
}
