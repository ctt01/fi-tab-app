package com.simetriagrupo.fictab.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FileUploader {
    private static final String LINE_FEED = "\r\n";
    private String boundary;
    private HttpURLConnection httpConn;
    private String charset = "UTF-8";
    private OutputStream outputStream;
    private PrintWriter writer;

    public String worker(String url, String params) {
        String requestURL = url;
        try {
            MultipartUtility multipart = new MultipartUtility(requestURL, charset);
            try {
                JSONObject obj = new JSONObject(params);
                Iterator<String> iter = obj.keys();
                while (iter.hasNext()) {
                    String key = iter.next();

                    try {
                        Object value = obj.get(key);
                        if (!key.equals("file"))
                            multipart.addFormField("" + key, "" + value.toString());
                        else{
                            if (value.toString().equals(""))
                                multipart.addFormField("" + key, "" + value.toString());
                            else
                                multipart.addFilePart(key,new File(value.toString()));
                        }
                    } catch (JSONException e) {
                        // Something went wrong!
                        return null;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            List<String> response = multipart.finish();

            StringBuilder sb = new StringBuilder();
            if (response == null) {
                return null;
            }
            for (String line : response) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException ex) {
            System.err.println(ex);
        }
        return null;
    }

    public class MultipartUtility {

        public MultipartUtility(String requestURL, String ch)
                throws IOException {
            charset = ch;
            boundary = "===" + System.currentTimeMillis() + "===";

            URL url = new URL(requestURL);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setUseCaches(false);
            httpConn.setRequestMethod("POST");
            httpConn.setDoOutput(true); // indicates POST method
            httpConn.setDoInput(true);

            httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            outputStream = httpConn.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);
        }


        public void addFormField(String name, String value) {
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
                    .append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=" + charset).append(
                    LINE_FEED);
            writer.append(LINE_FEED);
            writer.append(value).append(LINE_FEED);
            writer.flush();
        }

        public void addFilePart(String fieldName, File uploadFile)
                throws IOException {
            String fileName = uploadFile.getName();
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append(
                    "Content-Disposition: form-data; name=\"" + fieldName
                            + "\"; filename=\"" + fileName + "\"")
                    .append(LINE_FEED);
            writer.append(
                    "Content-Type: "
                            + URLConnection.guessContentTypeFromName(fileName))
                    .append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            FileInputStream inputStream = new FileInputStream(uploadFile);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            inputStream.close();

            writer.append(LINE_FEED);
            writer.flush();
        }

        public void addHeaderField(String name, String value) {
            writer.append(name + ": " + value).append(LINE_FEED);
            writer.flush();
        }

        public List<String> finish() throws IOException {
            List<String> response = new ArrayList<String>();

            writer.append(LINE_FEED).flush();
            writer.append("--" + boundary + "--").append(LINE_FEED);
            writer.close();

            int status = httpConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        httpConn.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    response.add(line);
                }
                reader.close();
                httpConn.disconnect();
            } else {
                BufferedReader r = new BufferedReader(new InputStreamReader(httpConn.getErrorStream()));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line).append('\n');
                }
                return null;
            }
            return response;
        }
    }
}