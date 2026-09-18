package com.apiturnos.autogestion;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CifrarEnlaceAutogestionTest {
    @Test void recuperaTokenYUsaNonceDistinto() {
        var cipher=new CifrarEnlaceAutogestion("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        String token=TokensAutogestion.nuevo();
        String a=cipher.cifrar(token),b=cipher.cifrar(token);
        assertThat(a).isNotEqualTo(b);assertThat(cipher.descifrar(a)).isEqualTo(token);
        assertThatThrownBy(()->cipher.descifrar(a.substring(0,a.length()-5)+"AAAAA"))
            .isInstanceOf(AutogestionException.class);
    }
    @Test void noGeneraEnlacesSinClaveDuradera() {
        assertThatThrownBy(()->new CifrarEnlaceAutogestion("").cifrar("token"))
            .isInstanceOfSatisfying(AutogestionException.class,e->assertThat(e.getStatus()).isEqualTo(503));
    }
}
