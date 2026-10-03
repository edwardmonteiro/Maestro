package br.com.maestro;
public final class LocalModel {
 public static final boolean AVAILABLE;
 static { boolean ok; try {System.loadLibrary("maestro_llm");ok=true;}catch(Throwable e){ok=false;} AVAILABLE=ok; }
 public static native String generate(String path,String prompt,int maxTokens);
 public static native void cancel();
 public static native void resetCancel();
}
