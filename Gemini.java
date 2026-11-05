package com.flowstudio.app;

import org.json.*;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.*;

final class Gemini {
    static final class QuotaException extends IOException { QuotaException() { super("Provider quota reached. Wait for the allowance to reset; no key cycling is performed."); } }
    static String story(String key, String model, String idea) throws Exception {
        if (!model.matches("gemini-[a-zA-Z0-9.\\-]+")) throw new IOException("Enter a valid Gemini model name.");
        String prompt = "Write an original 20-second English visual animal story for a YouTube Short. " +
            "Foxy is a friendly orange fox with brown eyes, cream muzzle and belly, dark paws, fluffy white-tipped tail, and orange/brown striped scarf. " +
            "Keep this design consistent. Four scenes of 5 seconds each, each with a clear image prompt, short caption, and action. " +
            "Immediate hook, understandable cause and effect, satisfying ending. No violence, borrowed characters, or misleading claims. " +
            "Return plain text, a title then numbered scenes. Idea: " + idea;
        HttpsURLConnection conn = (HttpsURLConnection) new URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent").openConnection();
        conn.setInstanceFollowRedirects(false); conn.setRequestMethod("POST"); conn.setConnectTimeout(15000); conn.setReadTimeout(60000);
        conn.setRequestProperty("Content-Type", "application/json"); conn.setRequestProperty("x-goog-api-key", key); conn.setDoOutput(true);
        try {
            JSONObject body = new JSONObject().put("contents", new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", prompt)))));
            try (OutputStream out = conn.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            int status = conn.getResponseCode();
            if (status == 429) throw new QuotaException();
            if (status != 200) throw new IOException("Provider returned HTTP " + status + ". Check your key, model access, and allowance.");
            StringBuilder result = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line; int size = 0;
                while ((line = in.readLine()) != null) { size += line.length(); if (size > 1000000) throw new IOException("Provider response too large"); result.append(line); }
            }
            JSONArray parts = new JSONObject(result.toString()).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts");
            StringBuilder text = new StringBuilder(); for (int i=0;i<parts.length();i++) text.append(parts.getJSONObject(i).optString("text", ""));
            if (text.toString().trim().isEmpty()) throw new IOException("Provider returned no story. Try a different idea.");
            return text.toString();
        } finally { conn.disconnect(); }
    }
}
