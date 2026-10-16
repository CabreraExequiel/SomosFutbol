package com.canchasya.api.security;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
@Component
public class TokenCrypto {
 private final byte[] key;
 public TokenCrypto(@Value("${app.mercadopago.encryption-key:}") String encoded,@Value("${app.payments.enabled:false}") boolean enabled) {
  try {key=encoded.isBlank()?null:Base64.getDecoder().decode(encoded);} catch(IllegalArgumentException e){throw new IllegalStateException("MP_ENCRYPTION_KEY debe estar en Base64");}
  if(enabled && (key==null||key.length!=32))throw new IllegalStateException("MP_ENCRYPTION_KEY debe contener 32 bytes aleatorios en Base64");
 }
 public String cifrar(String plain) {
  try {byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));byte[] data=c.doFinal(plain.getBytes(StandardCharsets.UTF_8));byte[] result=new byte[iv.length+data.length];System.arraycopy(iv,0,result,0,iv.length);System.arraycopy(data,0,result,iv.length,data.length);return Base64.getEncoder().encodeToString(result);}catch(Exception e){throw new IllegalStateException("No se pudo proteger la credencial");}
 }
 public String descifrar(String encoded) {
  try {byte[] data=Base64.getDecoder().decode(encoded);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,data,0,12));return new String(c.doFinal(data,12,data.length-12),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("No se pudo leer la credencial protegida");}
 }
}
