package com.premierhub.minigame.wordle;

import com.premierhub.minigame.wordle.WordleNames.NormalizedName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordleNamesTest {
    @Test
    void normalizesMultiWordSingleWordDiacriticsSpecialLettersAndPunctuation() {
        assertEquals(new NormalizedName("VAN DIJK", "VANDIJK"), WordleNames.normalize("Virgil van Dijk"));
        assertEquals(new NormalizedName("MAC ALLISTER", "MACALLISTER"), WordleNames.normalize("Alexis Mac Allister"));
        assertEquals(new NormalizedName("ESTEVAO", "ESTEVAO"), WordleNames.normalize("Estêvão"));
        assertEquals(new NormalizedName("ODEGAARD", "ODEGAARD"), WordleNames.normalize("Martin Ødegaard"));
        assertEquals(new NormalizedName("ODEGAARD", "ODEGAARD"), WordleNames.normalize("Ødegaard"));
        assertEquals(new NormalizedName("MUNOZ", "MUNOZ"), WordleNames.normalize("Daniel Muñoz"));
        assertEquals(new NormalizedName("WAN BISSAKA", "WANBISSAKA"), WordleNames.normalize("Aaron Wan-Bissaka"));
        assertEquals(new NormalizedName("OMUR", "OMUR"), WordleNames.normalize("Abdülkadir Ömür"));
        assertEquals(new NormalizedName("TOTH", "TOTH"), WordleNames.normalize("Alex Tóth"));
    }

    @Test
    void foldsAllSpecialCharactersAndStripsApostrophesAndDots() {
        assertEquals(new NormalizedName("OREILLY", "OREILLY"), WordleNames.normalize("Nico O'Reilly"));
        assertEquals(new NormalizedName("OREILLY", "OREILLY"), WordleNames.normalize("Nico O’Reilly"));
        assertEquals(new NormalizedName("ST JUSTE", "STJUSTE"), WordleNames.normalize("Jeremiah St. Juste"));
        assertEquals(new NormalizedName("GROSS", "GROSS"), WordleNames.normalize("Pascal Groß"));
        assertEquals(new NormalizedName("BAENA", "BAENA"), WordleNames.normalize("Álex Bæna"));
        assertEquals(new NormalizedName("LUKASZ", "LUKASZ"), WordleNames.normalize("Jan Łukasz"));
        assertEquals(new NormalizedName("DORDJEVIC", "DORDJEVIC"), WordleNames.normalize("Marko Đordjević"));
        assertEquals(new NormalizedName("INCE", "INCE"), WordleNames.normalize("Tom İnce"));
    }

    @Test
    void resolvePrefersOverrideWordWithoutDroppingFirstToken() {
        assertEquals(new NormalizedName("ALISSON", "ALISSON"),
                WordleNames.resolve("Alisson Becker", "ALISSON"));
        assertEquals(new NormalizedName("BRUNO GUIMARAES", "BRUNOGUIMARAES"),
                WordleNames.resolve("Bruno Guimarães", "Bruno Guimarães"));
        assertEquals(new NormalizedName("BECKER", "BECKER"),
                WordleNames.resolve("Alisson Becker", null));
        assertEquals(new NormalizedName("BECKER", "BECKER"),
                WordleNames.resolve("Alisson Becker", "   "));
    }
}
