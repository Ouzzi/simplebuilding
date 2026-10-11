package com.simplequalityoflife.client;
import com.simplequalityoflife.config.SimplequalityoflifeConfig;
public final class ClientNetworking {
 private static volatile SimplequalityoflifeConfig synced;
 public static void receive(String json){if(json.length()>32768)return;try{var c=new com.google.gson.Gson().fromJson(json,SimplequalityoflifeConfig.class);if(c!=null){c.normalize();synced=c;}}catch(RuntimeException ignored){}}
 public static void clear(){synced=null;}
 public static SimplequalityoflifeConfig getSyncedConfig(){return synced;}
}
