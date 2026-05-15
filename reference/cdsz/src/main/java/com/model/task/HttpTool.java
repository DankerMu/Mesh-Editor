package com.model.task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpTool {
	
	public static String doPost(String pathUrl, String data) {
        OutputStreamWriter out = null;
        BufferedReader br = null;
        String result = "";

        try {
            URL url = new URL(pathUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000000);
            conn.setReadTimeout(3000000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.connect();
            out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            out.write(data);
            out.flush();
            InputStream is = conn.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));

            for(String str = ""; (str = br.readLine()) != null; result = result + str) {
            }

//            System.out.println(result);
            is.close();
            conn.disconnect();
        } catch (Exception var17) {
            var17.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }

                if (br != null) {
                    br.close();
                }
            } catch (IOException var16) {
                var16.printStackTrace();
            }

        }

        return result;
    }
	
	public static String doGet(String pathUrl, String data) {
        OutputStreamWriter out = null;
        BufferedReader br = null;
        String result = "";

        try {
            URL url = new URL(pathUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000000);
            conn.setReadTimeout(3000000);
//            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
//            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.connect();
//            out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
//            out.write(data);
//            out.flush();
            InputStream is = conn.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));

            for(String str = ""; (str = br.readLine()) != null; result = result + str) {
            }

//            System.out.println(result);
            is.close();
            conn.disconnect();
        } catch (Exception var17) {
            var17.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }

                if (br != null) {
                    br.close();
                }
            } catch (IOException var16) {
                var16.printStackTrace();
            }

        }

        return result;
    }
}
