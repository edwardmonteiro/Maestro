#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <mutex>
#include "llama.h"
static std::atomic<bool> stopped{false};
static std::mutex lock;
static void fail(JNIEnv* env,const char* message){env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),message);}
extern "C" JNIEXPORT void JNICALL Java_br_com_maestro_LocalModel_cancel(JNIEnv*,jclass){stopped=true;}
extern "C" JNIEXPORT void JNICALL Java_br_com_maestro_LocalModel_resetCancel(JNIEnv*,jclass){stopped=false;}
extern "C" JNIEXPORT jstring JNICALL Java_br_com_maestro_LocalModel_generate(JNIEnv* env,jclass,jstring file,jstring input,jint maxTokens){
 std::lock_guard<std::mutex> guard(lock);
 if(stopped){fail(env,"Cancelado");return nullptr;}
 const char* f=env->GetStringUTFChars(file,nullptr);std::string path(f);env->ReleaseStringUTFChars(file,f);
 const char* p=env->GetStringUTFChars(input,nullptr);std::string prompt(p);env->ReleaseStringUTFChars(input,p);
 llama_backend_init();
 auto mp=llama_model_default_params();mp.n_gpu_layers=0;
 auto* model=llama_model_load_from_file(path.c_str(),mp);
 if(!model){fail(env,"Modelo nao carregou. Verifique memoria e arquivo.");return nullptr;}
 auto cp=llama_context_default_params();cp.n_ctx=4096;cp.n_batch=256;cp.n_ubatch=128;cp.n_threads=4;cp.n_threads_batch=4;
 auto* ctx=llama_init_from_model(model,cp);
 if(!ctx){llama_model_free(model);fail(env,"Memoria insuficiente para o modelo");return nullptr;}
 auto* vocab=llama_model_get_vocab(model);
 std::string wrapped="<|im_start|>system\nVoce e Maestro. Responda em portugues, de forma curta e util. Nao invente precos, locais ou pesquisas. Nao diga que executou acoes. /no_think<|im_end|>\n<|im_start|>user\n"+prompt+" /no_think<|im_end|>\n<|im_start|>assistant\n<think>\n\n</think>\n\n";
 int n=-llama_tokenize(vocab,wrapped.c_str(),wrapped.size(),nullptr,0,true,true);
 std::vector<llama_token> tokens(n);n=llama_tokenize(vocab,wrapped.c_str(),wrapped.size(),tokens.data(),n,true,true);
 if(n<0 || n>3000){llama_free(ctx);llama_model_free(model);fail(env,"Pedido longo demais. Reduza o texto.");return nullptr;}
 bool ok=true;
 for(int i=0;i<n&&!stopped;i+=256){auto b=llama_batch_get_one(tokens.data()+i,std::min(256,n-i));if(llama_decode(ctx,b)!=0){ok=false;break;}}
 auto* sampler=llama_sampler_chain_init(llama_sampler_chain_default_params());
 llama_sampler_chain_add(sampler,llama_sampler_init_top_k(20));llama_sampler_chain_add(sampler,llama_sampler_init_top_p(0.8f,1));llama_sampler_chain_add(sampler,llama_sampler_init_temp(0.6f));llama_sampler_chain_add(sampler,llama_sampler_init_dist(42));
 std::string output;
 for(int i=0;ok&&!stopped&&i<std::min((int)maxTokens,768);i++){
  llama_token t=llama_sampler_sample(sampler,ctx,-1);if(llama_vocab_is_eog(vocab,t))break;
  char piece[512];int len=llama_token_to_piece(vocab,t,piece,sizeof(piece),0,true);if(len>0)output.append(piece,len);
  auto b=llama_batch_get_one(&t,1);if(llama_decode(ctx,b)!=0)ok=false;
 }
 llama_sampler_free(sampler);llama_free(ctx);llama_model_free(model);
 if(stopped){fail(env,"Cancelado");return nullptr;}if(!ok){fail(env,"Falha durante inferencia");return nullptr;}
 // Decode UTF-8 with Java, avoiding JNI modified-UTF8 corruption on supplementary characters.
 jbyteArray bytes=env->NewByteArray(output.size());env->SetByteArrayRegion(bytes,0,output.size(),reinterpret_cast<const jbyte*>(output.data()));
 jclass str=env->FindClass("java/lang/String");jmethodID ctor=env->GetMethodID(str,"<init>","([BLjava/lang/String;)V");
 return (jstring)env->NewObject(str,ctor,bytes,env->NewStringUTF("UTF-8"));
}
