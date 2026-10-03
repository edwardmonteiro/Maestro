package br.com.maestro;
import android.content.*;import android.security.keystore.*;import android.util.Base64;import java.security.*;import javax.crypto.*;import javax.crypto.spec.GCMParameterSpec;
final class Vault {
 private final Context context; Vault(Context c){context=c;}
 private SecretKey key() throws Exception {KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);if(!k.containsAlias("maestro.secrets")){KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("maestro.secrets",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}return (SecretKey)k.getKey("maestro.secrets",null);}
 void put(String name,String value)throws Exception{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());String v=Base64.encodeToString(c.getIV(),2)+":"+Base64.encodeToString(c.doFinal(value.getBytes("UTF-8")),2);context.getSharedPreferences("vault",0).edit().putString(name,v).commit();}
 String get(String name){try{String v=context.getSharedPreferences("vault",0).getString(name,"");if(v.isEmpty())return "";String[]p=v.split(":");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p[0],2)));return new String(c.doFinal(Base64.decode(p[1],2)),"UTF-8");}catch(Exception e){return "";}}
}
