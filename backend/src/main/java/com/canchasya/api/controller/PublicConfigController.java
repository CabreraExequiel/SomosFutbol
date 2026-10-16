package com.canchasya.api.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequiredArgsConstructor
public class PublicConfigController {
 private final Environment env;
 @GetMapping("/api/config") public Map<String,Object> config(){return Map.of(
  "operador",env.getProperty("app.legal.operator",""),"cuit",env.getProperty("app.legal.tax-id",""),
  "domicilio",env.getProperty("app.legal.address",""),"contacto",env.getProperty("app.legal.contact",""),
  "versionTerminos",env.getProperty("app.legal.terms-version"),"versionPrivacidad",env.getProperty("app.legal.privacy-version"),
  "pagosOnline",env.getProperty("app.payments.enabled",Boolean.class,false));}
}
