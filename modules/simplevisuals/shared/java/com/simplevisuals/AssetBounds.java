package com.simplevisuals;
/** Validate compressed inputs before any native image allocation. */
public final class AssetBounds {
 public static final int MAX_RULE_BYTES=65536, MAX_RULES=128, MAX_TEXTURE_BYTES=1048576, MAX_TEXTURE_SIZE=256;
 public static boolean pngHeader(byte[] header){
  byte[] signature={(byte)137,80,78,71,13,10,26,10};
  if(header.length<24)return false;for(int i=0;i<8;i++)if(header[i]!=signature[i])return false;
  var buffer=java.nio.ByteBuffer.wrap(header);
  if(buffer.getInt(8)!=13||buffer.getInt(12)!=0x49484452)return false;
  int width=buffer.getInt(16),height=buffer.getInt(20);
  return width>0&&height>0&&width<=MAX_TEXTURE_SIZE&&height<=MAX_TEXTURE_SIZE;
 }
 public static String readRule(java.io.InputStream input)throws java.io.IOException{
  byte[] data=input.readNBytes(MAX_RULE_BYTES+1);if(data.length>MAX_RULE_BYTES)throw new java.io.IOException("CIT rule exceeds safe byte limit");return new String(data,java.nio.charset.StandardCharsets.UTF_8);
 }
}
