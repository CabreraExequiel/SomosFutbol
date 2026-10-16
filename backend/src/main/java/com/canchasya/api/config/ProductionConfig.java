package com.canchasya.api.config;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import lombok.RequiredArgsConstructor;
import java.net.URI;
@Component @Profile("prod") @RequiredArgsConstructor
public class ProductionConfig {
 private final Environment env;
 @PostConstruct void validar(){
  String secret=required("app.jwt.secret");
  if(secret.length()<48||secret.toLowerCase().matches(".*(cambiar|desarrollo|example).*"))throw new IllegalStateException("JWT_SECRET debe ser aleatoria y de al menos 48 caracteres en producción");
  for(String key:new String[]{"app.frontend-url","app.public-base-url","app.backend-public-url"}){
   URI uri=URI.create(required(key));if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getHost().equals("localhost")||uri.getHost().contains("tu-dominio")||uri.getQuery()!=null||uri.getFragment()!=null)throw new IllegalStateException(key+" debe ser una URL HTTPS de tu dominio");
  }
  String cors=required("app.cors.allowed-origins");if(cors.contains("*"))throw new IllegalStateException("CORS no admite comodines en producción");
  for(String key:new String[]{"spring.datasource.password","app.legal.operator","app.legal.tax-id","app.legal.address","app.legal.contact"}){
   String value=required(key);if(value.toLowerCase().matches(".*(completar|cambiar|example|ejemplo).*"))throw new IllegalStateException("Completá "+key+" con el valor real antes de desplegar");
  }
 }
 private String required(String key){String value=env.getProperty(key,"");if(value.isBlank())throw new IllegalStateException("Falta configurar "+key);return value;}
}
