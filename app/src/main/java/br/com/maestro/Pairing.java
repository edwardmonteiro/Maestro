package br.com.maestro;
import java.security.*;import java.nio.charset.StandardCharsets;import java.util.function.LongSupplier;
/** Single-use install capability: no access to notes, actions or inference. */
final class Pairing {
 private final LongSupplier clock;private String token="";private long expiry;
 Pairing(){this(()->System.nanoTime()/1_000_000L);}
 Pairing(LongSupplier clock){this.clock=clock;}
 synchronized String begin(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format("%02x",v));token=s.toString();expiry=clock.getAsLong()+15*60*1000;return token;}
 synchronized boolean accepts(String path,String auth){return path.equals("/setup/install.sh")&&!token.isEmpty()&&clock.getAsLong()<expiry&&MessageDigest.isEqual(("Bearer "+token).getBytes(StandardCharsets.UTF_8),auth.getBytes(StandardCharsets.UTF_8));}
 synchronized void consume(){if(token.isEmpty()||clock.getAsLong()>=expiry)throw new IllegalStateException("Pareamento expirado. Gere outro no Maestro.");token="";expiry=0;}
}
