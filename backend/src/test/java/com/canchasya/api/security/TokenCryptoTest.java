package com.canchasya.api.security;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;
class TokenCryptoTest {
 final String key=Base64.getEncoder().encodeToString(new byte[32]);
 @Test void cifraConNonceAleatorioYRecupera(){var c=new TokenCrypto(key,true);String a=c.cifrar("credencial-sensible"),b=c.cifrar("credencial-sensible");assertThat(a).isNotEqualTo(b).doesNotContain("sensible");assertThat(c.descifrar(a)).isEqualTo("credencial-sensible");}
 @Test void rechazaManipulacion(){var c=new TokenCrypto(key,true);byte[] data=Base64.getDecoder().decode(c.cifrar("token"));data[data.length-1]^=1;assertThatThrownBy(()->c.descifrar(Base64.getEncoder().encodeToString(data))).isInstanceOf(IllegalStateException.class);}
 @Test void pagosHabilitadosRequierenClaveFuerte(){assertThatThrownBy(()->new TokenCrypto("",true)).isInstanceOf(IllegalStateException.class);}
}
