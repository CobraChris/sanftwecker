package com.cobrachris.sanftwecker;

import java.text.Normalizer;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Offline city picker using the Android IANA time-zone database, including DST. */
public final class WorldClockCatalog {
    public static final class City {
        public final String name;
        public final String country;
        public final String zone,id;
        City(String name,String country,String zone,String id){
            this.name=name;this.country=country;this.zone=zone;this.id=id;
        }
    }

    private static volatile List<City> cached;
    private static volatile Map<String,City> byZone;
    private WorldClockCatalog(){}

    private static final String[][] POPULAR={
        {"Tokio","Japan","Asia/Tokyo"},
        {"Berlin","Deutschland","Europe/Berlin"},
        {"Hamburg","Deutschland","Europe/Berlin"},
        {"München","Deutschland","Europe/Berlin"},
        {"London","Vereinigtes Königreich","Europe/London"},
        {"New York","USA","America/New_York"},
        {"Los Angeles","USA","America/Los_Angeles"},
        {"Sydney","Australien","Australia/Sydney"},
        {"Singapur","Singapur","Asia/Singapore"},
        {"Dubai","Vereinigte Arabische Emirate","Asia/Dubai"},
        {"Paris","Frankreich","Europe/Paris"},
        {"Bangkok","Thailand","Asia/Bangkok"},
        {"Seoul","Südkorea","Asia/Seoul"},
        {"Shanghai","China","Asia/Shanghai"},
        {"Hongkong","China","Asia/Hong_Kong"},
        {"Taipeh","Taiwan","Asia/Taipei"},
        {"Neu-Delhi","Indien","Asia/Kolkata"},
        {"Mumbai","Indien","Asia/Calcutta"},
        {"Istanbul","Türkei","Europe/Istanbul"},
        {"Rom","Italien","Europe/Rome"},
        {"Madrid","Spanien","Europe/Madrid"},
        {"Lissabon","Portugal","Europe/Lisbon"},
        {"Amsterdam","Niederlande","Europe/Amsterdam"},
        {"Zürich","Schweiz","Europe/Zurich"},
        {"Wien","Österreich","Europe/Vienna"},
        {"Prag","Tschechien","Europe/Prague"},
        {"Warschau","Polen","Europe/Warsaw"},
        {"Stockholm","Schweden","Europe/Stockholm"},
        {"Oslo","Norwegen","Europe/Oslo"},
        {"Kopenhagen","Dänemark","Europe/Copenhagen"},
        {"Helsinki","Finnland","Europe/Helsinki"},
        {"Athen","Griechenland","Europe/Athens"},
        {"Reykjavík","Island","Atlantic/Reykjavik"},
        {"Moskau","Russland","Europe/Moscow"},
        {"Kairo","Ägypten","Africa/Cairo"},
        {"Kapstadt","Südafrika","Africa/Johannesburg"},
        {"Nairobi","Kenia","Africa/Nairobi"},
        {"Lagos","Nigeria","Africa/Lagos"},
        {"Riad","Saudi-Arabien","Asia/Riyadh"},
        {"Tel Aviv","Israel","Asia/Jerusalem"},
        {"Karatschi","Pakistan","Asia/Karachi"},
        {"Dhaka","Bangladesch","Asia/Dhaka"},
        {"Kathmandu","Nepal","Asia/Kathmandu"},
        {"Hanoi","Vietnam","Asia/Ho_Chi_Minh"},
        {"Jakarta","Indonesien","Asia/Jakarta"},
        {"Manila","Philippinen","Asia/Manila"},
        {"Perth","Australien","Australia/Perth"},
        {"Melbourne","Australien","Australia/Melbourne"},
        {"Auckland","Neuseeland","Pacific/Auckland"},
        {"Honolulu","USA","Pacific/Honolulu"},
        {"San Francisco","USA","America/Los_Angeles"},
        {"Chicago","USA","America/Chicago"},
        {"Denver","USA","America/Denver"},
        {"Toronto","Kanada","America/Toronto"},
        {"Vancouver","Kanada","America/Vancouver"},
        {"Mexiko-Stadt","Mexiko","America/Mexico_City"},
        {"São Paulo","Brasilien","America/Sao_Paulo"},
        {"Rio de Janeiro","Brasilien","America/Sao_Paulo"},
        {"Buenos Aires","Argentinien","America/Argentina/Buenos_Aires"},
        {"Santiago","Chile","America/Santiago"},
        {"Lima","Peru","America/Lima"},
        {"Bogotá","Kolumbien","America/Bogota"},
        {"Caracas","Venezuela","America/Caracas"}
    };

    private static void init(){
        if(cached!=null)return;
        synchronized(WorldClockCatalog.class){
            if(cached!=null)return;
            Set<String> available=ZoneId.getAvailableZoneIds();
            Map<String,City> by=new HashMap<>();
            List<City> all=new ArrayList<>();
            for(String[] row:POPULAR){
                if(!available.contains(row[2]))continue;
                String key=by.containsKey(row[2])?
                        row[2]+"#"+normalize(row[0]).replaceAll("[^a-z0-9]+","_"):
                        row[2];
                City city=new City(row[0],row[1],row[2],key);
                all.add(city);by.put(city.id,city);
            }
            List<String> remaining=new ArrayList<>(available);
            remaining.sort(String.CASE_INSENSITIVE_ORDER);
            for(String zone:remaining){
                if(by.containsKey(zone)||!isRegionZone(zone))continue;
                String[] segments=zone.split("/");
                String city=segments[segments.length-1].replace('_',' ');
                City other=new City(city,segments[0],zone,zone);
                all.add(other);by.put(other.id,other);
            }
            cached=Collections.unmodifiableList(all);
            byZone=Collections.unmodifiableMap(by);
        }
    }

    private static boolean isRegionZone(String zone){
        if(!zone.contains("/"))return false;
        String region=zone.substring(0,zone.indexOf('/'));
        return region.equals("Africa")||region.equals("America")||
                region.equals("Antarctica")||region.equals("Asia")||
                region.equals("Atlantic")||region.equals("Australia")||
                region.equals("Europe")||region.equals("Indian")||
                region.equals("Pacific");
    }

    public static City get(String zone){
        if(zone==null)return null;
        init();
        return byZone.get(zone);
    }

    public static List<City> search(String query,Set<String> excluded,int limit){
        init();
        if(limit<=0)return Collections.emptyList();
        String needle=normalize(query==null?"":query);
        List<City> found=new ArrayList<>();
        for(City city:cached){
            if(excluded!=null&&excluded.contains(city.id))continue;
            if(!needle.isEmpty()&&!normalize(city.name+" "+city.country+" "+city.zone)
                    .contains(needle))continue;
            found.add(city);
            if(found.size()>=limit)break;
        }
        return found;
    }

    static String normalize(String value){
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
                .replaceAll("\\p{M}+","").replace("ß","ss");
    }

    public static boolean isSelectable(String zone){
        return get(zone)!=null;
    }
}
