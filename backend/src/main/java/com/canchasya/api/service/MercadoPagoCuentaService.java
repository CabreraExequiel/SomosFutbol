package com.canchasya.api.service;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.*;
import com.canchasya.api.repository.*;
import com.canchasya.api.security.TokenCrypto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class MercadoPagoCuentaService {
 private final MercadoPagoCuentaRepository cuentas;
 private final OAuthEstadoRepository estados;
 private final TokenCrypto crypto;
 private final WebClient webClient;
 private final Clock clock;
 @Value("${app.payments.enabled:false}") private boolean enabled;
 @Value("${app.mercadopago.test-mode:true}") private boolean testMode;
 @Value("${app.mercadopago.client-id:}") private String clientId;
 @Value("${app.mercadopago.client-secret:}") private String clientSecret;
 @Value("${app.backend-public-url:}") private String backend;
 @Value("${app.frontend-url:http://localhost:4200}") private String frontend;
 @PostConstruct void validar(){if(enabled && (clientId.isBlank()||clientSecret.isBlank()||!backend.startsWith("https://")))throw new IllegalStateException("Mercado Pago requiere CLIENT_ID, CLIENT_SECRET y BACKEND_PUBLIC_URL HTTPS");}
 public record Estado(boolean habilitado,boolean vinculada,String cuenta,boolean pruebas){}
 public record Inicio(String url,String browser){}
 public Estado estado(User user){var c=cuentas.findById(user.getId());return new Estado(enabled,c.isPresent(),c.map(MercadoPagoCuenta::getMpUserId).orElse(null),testMode);}
 public boolean vinculada(Long id){return cuentas.existsById(id);}
 @Transactional
 public Inicio iniciar(User user){
  if(!enabled)throw error("El cobro online todavía no está habilitado",409);
  if(user.getRole()!=Role.DUENO)throw error("Solo un dueño puede vincular una cuenta",403);
  estados.deleteByExpiresAtBefore(clock.instant());
  String state=aleatorio(),browser=aleatorio(),verifier=aleatorio();
  var e=new OAuthEstado();e.setHash(hash(state));e.setDuenoId(user.getId());e.setBrowserHash(hash(browser));e.setVerifier(crypto.cifrar(verifier));e.setExpiresAt(clock.instant().plusSeconds(600));estados.save(e);
  String url=UriComponentsBuilder.fromUriString("https://auth.mercadopago.com/authorization").queryParam("client_id",clientId).queryParam("response_type","code").queryParam("platform_id","mp").queryParam("state",state).queryParam("redirect_uri",callback()).queryParam("code_challenge",challenge(verifier)).queryParam("code_challenge_method","S256").build().encode().toUriString();
  return new Inicio(url,browser);
 }
 @Transactional
 public void completar(String state,String code,String browser){
  if(!enabled||state==null||code==null||browser==null)throw error("La vinculación venció. Volvé a intentarlo desde tu perfil",400);
  var e=estados.bloquear(hash(state)).orElseThrow(()->error("Vinculación inválida o ya utilizada",400));
  if(!e.getExpiresAt().isAfter(clock.instant())||!MessageDigest.isEqual(e.getBrowserHash().getBytes(StandardCharsets.UTF_8),hash(browser).getBytes(StandardCharsets.UTF_8)))throw error("La vinculación venció o pertenece a otro navegador",400);
  var body=new LinkedHashMap<String,Object>();body.put("grant_type","authorization_code");body.put("code",code);body.put("redirect_uri",callback());body.put("code_verifier",crypto.descifrar(e.getVerifier()));
  var response=token(body);var anterior=cuentas.bloquear(e.getDuenoId()).orElse(null);
  var c=anterior==null?new MercadoPagoCuenta():anterior;c.setDuenoId(e.getDuenoId());
  aplicar(c,response);cuentas.save(c);estados.delete(e);
 }
 @Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
 public String accessToken(Long duenoId){
  var c=cuentas.bloquear(duenoId).orElseThrow(()->error("El dueño debe vincular su cuenta de Mercado Pago",409));
  if(c.isLiveMode()==testMode)throw error("La cuenta de cobro no corresponde al entorno configurado",409);
  if(!c.getExpiresAt().isAfter(clock.instant().plusSeconds(120))){
   var body=new LinkedHashMap<String,Object>();body.put("grant_type","refresh_token");body.put("refresh_token",crypto.descifrar(c.getRefreshToken()));aplicar(c,token(body));cuentas.save(c);
  }
  return crypto.descifrar(c.getAccessToken());
 }
 public String collector(Long id){return cuentas.findById(id).orElseThrow(()->error("Cuenta no vinculada",409)).getMpUserId();}
 public Long duenoPorCuenta(String id){return cuentas.findByMpUserId(id).map(MercadoPagoCuenta::getDuenoId).orElse(null);}
 public URI retorno(boolean ok){return URI.create(frontend.replaceAll("/+$","")+"/perfil?mercadopago="+(ok?"vinculado":"error"));}
 private String callback(){return backend.replaceAll("/+$","")+"/api/mercadopago/callback";}
 private void aplicar(MercadoPagoCuenta c,Map<String,Object> r){
  String id=Objects.toString(r.get("user_id"),"");String access=Objects.toString(r.get("access_token"),"");String refresh=Objects.toString(r.get("refresh_token"),"");
  if(id.isBlank()||access.isBlank()||refresh.isBlank()||!(r.get("expires_in") instanceof Number seconds)||!(r.get("live_mode") instanceof Boolean live)||live==testMode)throw error("Mercado Pago devolvió una cuenta o entorno inválido",502);
  if(c.getMpUserId()!=null&&!c.getMpUserId().equals(id))throw error("Debés volver a vincular la misma cuenta para conservar la trazabilidad de los cobros",409);
  c.setMpUserId(id);c.setAccessToken(crypto.cifrar(access));c.setRefreshToken(crypto.cifrar(refresh));c.setExpiresAt(clock.instant().plusSeconds(seconds.longValue()));c.setLiveMode(live);
 }
 @SuppressWarnings("unchecked")
 private Map<String,Object> token(Map<String,Object> body){
  body.put("client_id",clientId);body.put("client_secret",clientSecret);
  try{var response=webClient.post().uri("https://api.mercadopago.com/oauth/token").bodyValue(body).retrieve().bodyToMono(Map.class).block(Duration.ofSeconds(12));if(response==null)throw error("No hubo respuesta de Mercado Pago",502);return response;}
  catch(RuntimeException e){throw error("No se pudo vincular o renovar Mercado Pago. Volvé a autorizar desde tu perfil",502);}
 }
 private static String aleatorio(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 private static byte[] digest(String value){try{return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private static String hash(String value){return HexFormat.of().formatHex(digest(value));}
 private static String challenge(String value){return Base64.getUrlEncoder().withoutPadding().encodeToString(digest(value));}
 private static ApiException error(String m,int s){return new ApiException(m,HttpStatus.valueOf(s));}
}
