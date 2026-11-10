package com.cobrachris.sanftwecker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Opt-in worldwide place lookup. The provider's GeoNames-based response includes
 * an IANA timezone, so no paid geocoding or timezone key/service is required.
 * Only the typed search string leaves the phone. Saved clocks remain offline.
 */
public final class WorldClockGeocoding {
    private static final String ENDPOINT="https://geocoding-api.open-meteo.com/v1/search";
    private static final int MAX_RESPONSE=256_000;
    private static final Set<String> US_STATES=new HashSet<>(Arrays.asList(
            "AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN",
            "IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE",
            "NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC",
            "SD","TN","TX","UT","VT","VA","WA","WV","WI","WY","DC"));
    private static final Pattern STATE_SUFFIX=Pattern.compile(
            "^(.+?)\\s+([A-Za-z]{2})$");
    private WorldClockGeocoding(){}

    static String qualifiedQuery(String input) {
        String clean=input==null?"":input.trim().replaceAll("\\s+"," ");
        Matcher match=STATE_SUFFIX.matcher(clean);
        if(!clean.contains(",") && match.matches() &&
                US_STATES.contains(match.group(2).toUpperCase(Locale.ROOT))) {
            return match.group(1)+", "+match.group(2).toUpperCase(Locale.ROOT);
        }
        return clean;
    }

    static String requestUrl(String query) {
        String clean=qualifiedQuery(query);
        if(clean.length()<2 || clean.length()>100)
            throw new IllegalArgumentException("Bitte 2 bis 100 Zeichen eingeben");
        try {
            return ENDPOINT+"?name="+URLEncoder.encode(clean,"UTF-8")
                    +"&count=50&language=en&format=json";
        } catch(UnsupportedEncodingException impossible){
            throw new IllegalStateException("UTF-8 nicht verfügbar",impossible);
        }
    }

    /** Called on a background thread, never by the Android UI thread. */
    public static List<WorldClockCatalog.City> search(String query) throws IOException {
        HttpURLConnection connection=(HttpURLConnection)new URL(requestUrl(query)).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(6500);
        connection.setReadTimeout(6500);
        connection.setRequestProperty("Accept","application/json");
        connection.setRequestProperty("User-Agent","Sanftwecker-WorldClock/1.0");
        try {
            if(connection.getResponseCode()!=HttpURLConnection.HTTP_OK)
                throw new IOException("Ortsdienst derzeit nicht erreichbar (HTTP "+
                        connection.getResponseCode()+")");
            StringBuilder text=new StringBuilder();
            try(InputStream stream=connection.getInputStream();
                InputStreamReader reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
                char[] buffer=new char[4096];
                int size;
                while((size=reader.read(buffer))!=-1){
                    if(text.length()+size>MAX_RESPONSE)
                        throw new IOException("Antwort des Ortsdiensts ist zu groß");
                    text.append(buffer,0,size);
                }
            }
            return parse(text.toString());
        } catch(JSONException e) {
            throw new IOException("Ungültige Antwort des Ortsdiensts",e);
        } finally {
            connection.disconnect();
        }
    }

    static List<WorldClockCatalog.City> parse(String response) throws JSONException {
        JSONArray results=new JSONObject(response).optJSONArray("results");
        List<WorldClockCatalog.City> cities=new ArrayList<>();
        if(results==null)return cities;
        Set<String> zones=ZoneId.getAvailableZoneIds();
        Set<String> ids=new HashSet<>();
        for(int i=0;i<results.length() && i<100;i++){
            JSONObject item=results.optJSONObject(i);
            if(item==null)continue;
            long geonameId=item.optLong("id",-1);
            String name=item.optString("name","").trim();
            String zone=item.optString("timezone","").trim();
            if(geonameId<=0 || name.isEmpty() || name.length()>100 ||
                    !zones.contains(zone) || zone.indexOf('/')<0)continue;
            String id="geo:"+geonameId;
            if(!ids.add(id))continue;
            String country=item.optString("country","").trim();
            if("US".equalsIgnoreCase(item.optString("country_code")))
                country="USA";
            String admin=item.optString("admin1","").trim();
            if(admin.equalsIgnoreCase(name))admin="";
            String location=admin.isEmpty()?country:
                    country.isEmpty()?admin:admin+", "+country;
            if(location.length()>140 || location.isEmpty())continue;
            cities.add(new WorldClockCatalog.City(name,location,zone,id));
        }
        return cities;
    }
}
