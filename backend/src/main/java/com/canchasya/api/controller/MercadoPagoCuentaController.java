package com.canchasya.api.controller;
import com.canchasya.api.model.User;
import com.canchasya.api.service.MercadoPagoCuentaService;
import com.canchasya.api.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.Map;
@RestController @RequestMapping("/api/mercadopago") @RequiredArgsConstructor
public class MercadoPagoCuentaController {
 private final MercadoPagoCuentaService service;
 @GetMapping("/estado") @PreAuthorize("hasRole('DUENO')")
 public MercadoPagoCuentaService.Estado estado(@AuthenticationPrincipal User user){return service.estado(user);}
 @PostMapping("/vincular") @PreAuthorize("hasRole('DUENO')")
 public ResponseEntity<Map<String,String>> vincular(@AuthenticationPrincipal User user){var inicio=service.iniciar(user);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.SET_COOKIE,cookie(inicio.browser(),600).toString()).body(Map.of("url",inicio.url()));}
 @GetMapping("/callback")
 public ResponseEntity<Void> callback(@RequestParam(required=false) String state,@RequestParam(required=false) String code,@CookieValue(name="mp_oauth",required=false) String browser){
  boolean ok;try{service.completar(state,code,browser);ok=true;}catch(ApiException e){ok=false;}
  return ResponseEntity.status(HttpStatus.FOUND).cacheControl(CacheControl.noStore()).header(HttpHeaders.SET_COOKIE,cookie("",0).toString()).location(service.retorno(ok)).build();
 }
 private ResponseCookie cookie(String value,int seconds){return ResponseCookie.from("mp_oauth",value).httpOnly(true).secure(true).sameSite("Lax").path("/api/mercadopago").maxAge(Duration.ofSeconds(seconds)).build();}
}
