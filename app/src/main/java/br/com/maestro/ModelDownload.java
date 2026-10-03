package br.com.maestro;
import java.io.*;import java.net.*;import java.security.*;
final class ModelDownload {
 static final String NAME="Qwen3-1.7B-Q4_K_M.gguf";
 static final long SIZE=1107409472L;
 static final String SHA="b139949c5bd74937ad8ed8c8cf3d9ffb1e99c866c823204dc42c0d91fa181897";
 static final String URL="https://huggingface.co/unsloth/Qwen3-1.7B-GGUF/resolve/d7f544eead698dbd1f15126ef60b45a1e1933222/"+NAME;
 interface Progress{void update(long current,long total);}
 static File file(File dir){return new File(dir,NAME);}
 static void download(File dir,Net.Job job,Progress progress)throws Exception{
  File target=file(dir);if(target.exists()&&target.length()==SIZE)return;
  File part=new File(dir,NAME+".part");long existing=part.exists()?part.length():0;if(existing>SIZE){part.delete();existing=0;}
  if(dir.getUsableSpace()<SIZE-existing+200_000_000L)throw new IOException("Libere pelo menos 1,4 GB de armazenamento.");
  if(existing<SIZE){
  java.net.URL url=new java.net.URL(URL);HttpURLConnection c=null;
  for(int redirects=0;redirects<8;redirects++){job.check();c=(HttpURLConnection)url.openConnection();job.active=c;c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(30000);if(existing>0)c.setRequestProperty("Range","bytes="+existing+"-");int code=c.getResponseCode();if(code>=300&&code<400){String loc=c.getHeaderField("Location");java.net.URL next=new java.net.URL(url,loc);if(!next.getProtocol().equals("https"))throw new IOException("Redirecionamento inseguro");c.disconnect();url=next;continue;}break;}
  if(c==null)throw new IOException("Falha no download");
  try{int code=c.getResponseCode();if(code!=200&&code!=206)throw new IOException("Download HTTP "+code);if(code==200)existing=0;if(code==206&&!c.getHeaderField("Content-Range").startsWith("bytes "+existing+"-"))throw new IOException("Retomada invalida");
   try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(part,existing>0)){byte[]b=new byte[65536];int n;long count=existing,last=0;while((n=in.read(b))!=-1){job.check();count+=n;if(count>SIZE)throw new IOException("Modelo excedeu tamanho esperado");out.write(b,0,n);if(System.currentTimeMillis()-last>500){progress.update(count,SIZE);last=System.currentTimeMillis();}}}
  }finally{c.disconnect();job.active=null;}
  }
  job.check();if(part.length()!=SIZE)throw new IOException("Download incompleto. Toque novamente para continuar.");
  MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(part)){byte[]b=new byte[65536];int n;while((n=in.read(b))!=-1){job.check();md.update(b,0,n);}}
  StringBuilder hash=new StringBuilder();for(byte b:md.digest())hash.append(String.format("%02x",b&255));if(!hash.toString().equals(SHA)){part.delete();throw new IOException("Integridade do modelo invalida. Baixe novamente.");}
  if(!part.renameTo(target))throw new IOException("Nao foi possivel salvar modelo");progress.update(SIZE,SIZE);
 }
}
