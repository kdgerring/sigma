package com.articulate.sigma;

import org.junit.*;

import static org.junit.Assert.assertEquals;

/**
 * LanguageFormatter tests specifically targeted toward the htmlParaphrase( ) method.
 */
public class LanguageFormatterHtmlParaphraseMockTest extends SigmaMockTestBase  {
    KB kb = kbMock;

    @Test
    public void testHtmlParaphraseDriving1()     {
        String stmt =       "(exists (?D ?H)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H)))";

        String expectedResult = "a human drives";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDrivingNot1()     {
        String stmt =       "(not \n" +
                "               (exists (?D ?H)\n" +
                "                   (and\n" +
                "                       (instance ?D Driving)\n" +
                "                       (instance ?H Human)\n" +
                "                       (agent ?D ?H))))";


        String expectedResult = "there don't exist a process and an agent such that the process is an instance of Driving and the agent is an instance of human and the agent is an agent of the process";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDrivingHarryReified()     {
        String stmt =       "(exists (?D)\n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance Harry Human)\n" +
                "                   (agent ?D Harry)))";

        String expectedResult = "Harry drives";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDrivingHarryReifiedNot()     {
        String stmt =       "(not\n" +
                "               (exists (?D)\n" +
                "                   (and\n" +
                "                       (instance ?D Driving)\n" +
                "                       (instance Harry Human)\n" +
                "                       (agent ?D Harry))))";

        String expectedResult = "there doesn't exist a process such that the process is an instance of Driving and Harry is an instance of human and Harry is an agent of the process";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDriving2()     {
        String stmt =   "(exists (?D ?H)\n" +
                "           (and\n" +
                "           (instance ?D Driving)\n" +
                "           (instance ?H Human)\n" +
                "           (names \"John\" ?H)\n" +
                "           (agent ?D ?H)))";

        String expectedResult = "there exist a process and an agent such that the process is an instance of Driving and the agent is an instance of human and the agent has name \"John\" and the agent is an agent of the process";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDrivingNot2()     {
        String stmt =   "(not\n" +
                "           (exists (?D ?H)\n" +
                "               (and\n" +
                "               (instance ?D Driving)\n" +
                "               (instance ?H Human)\n" +
                "               (names \"John\" ?H)\n" +
                "               (agent ?D ?H))))";

        String expectedResult = "there don't exist a process and an agent such that the process is an instance of Driving and the agent is an instance of human and the agent has name \"John\" and the agent is an agent of the process";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal: "A human travels to (the) Sudan."
     */
    @Test
    public void testHumanTravels()     {
        String stmt =   "(exists (?he ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Transportation)\n" +
                "                    (instance ?he Human)\n" +
                "                    (agent ?event ?he)))";

        String expectedResult = "a human performs a transportation";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal: "A human travels to (the) Sudan."
     */
    @Test
    public void testHumanTravelsSudan()     {
        String stmt =   "(exists (?he ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Transportation)\n" +
                "                    (instance ?he Human)\n" +
                "                    (agent ?event ?he)\n" +
                "                    (destination ?event Sudan)))";

        String expectedResult = "there exist an agent and a process such that the process is an instance of Transportation and the agent is an instance of human and the agent is an agent of the process and the process ends at Sudan";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal: "He travels to (the) Sudan."
     */
    @Test
    public void testHeTravelsSudan()     {
        String stmt =   "(exists (?he ?event)\n" +
                "                  (and\n" +
                "                    (instance ?event Transportation)\n" +
                "                    (attribute ?he Male)\n" +
                "                    (instance ?he Human)\n" +
                "                    (agent ?event ?he)\n" +
                "                    (destination ?event Sudan)))";

        String expectedResult = "there exist an agent and a process such that the process is an instance of Transportation and male is an attribute of the agent and the agent is an instance of human and the agent is an agent of the process and the process ends at Sudan";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal: "Bell created the telephone."; also "The telephone was created by Bell."
     */
    @Test
    @Ignore
    public void testHtmlParaphraseBellCreateTelephone()     {
        String stmt =   "(exists \n" +
                "              (?event ?telephone) \n" +
                "              (and \n" +
                "                (instance Bell Human) \n" +
                "                (agent ?event Bell) \n" +
                "                (instance ?event Process) \n" +
                "                (instance ?telephone Telephone) \n" +
                "                (patient ?event ?telephone)))";

        String expectedResult = "Bell performs a process a telephone";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal: "Bell created the telephone."; also "The telephone was created by Bell."
     */
    @Test
    public void testHtmlParaphraseBlankenshipCreateTelephone()     {
        String stmt =   "(exists \n" +
                "              (?event ?telephone) \n" +
                "              (and \n" +
                "                (instance Blankenship Human) \n" +
                "                (agent ?event Blankenship) \n" +
                "                (instance ?event Process) \n" +
                "                (instance ?telephone Telephone) \n" +
                "                (patient ?event ?telephone)))";

        String expectedResult = "Blankenship performs a process a telephone";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal: "Bob eats and drinks on the desk."
     */
    @Test
    public void testHtmlParaphraseBobEatsDrinksDesk()     {
        String stmt =   "(exists \n" +
                "              (?desk ?event1 ?event2) \n" +
                "              (and \n" +
                "                   (attribute Robert-1 Male) \n" +
                "                   (exists \n" +
                "                       (?location) \n" +
                "                       (and \n" +
                "                           (located ?event2 ?location) \n" +
                "                           (orientation ?location ?desk On))) \n" +
                "                   (instance Robert-1 Human)     \n" +
                "                   (instance ?desk Desk) \n" +
                "                   (agent ?event1 Robert-1) \n" +
                "                   (instance ?event1 Eating) \n" +
                "                   (agent ?event2 Robert-1) \n" +
                "                   (instance ?event2 Drinking)))";

        String expectedResult = "there exist an entity, a process and another process such that male is an attribute of Robert-1 and there exists another entity such that the other process is located at the other entity and the other entity is On to the entity and Robert-1 is an instance of human and the entity is an instance of Desk and Robert-1 is an agent of the process and the process is an instance of Eating and Robert-1 is an agent of the other process and the other process is an instance of Drinking";

        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal: "If Mary gives John a book then he reads it."
     */
    @Test
    public void testHtmlParaphraseIfMaryGivesBookJohnThenHeReads()     {
        String stmt =   "(forall \n" +
                "              (?book ?event1) \n" +
                "              (=> \n" +
                "                (and \n" +
                "                  (attribute John-1 Male) \n" +
                "                  (attribute Mary-1 Female) \n" +
                "                  (instance John-1 Human) \n" +
                "                  (instance Mary-1 Human) \n" +
                "                  (instance ?book Book) \n" +
                "                  (agent ?event1 Mary-1) \n" +
                "                  (destination ?event1 John-1) \n" +
                "                  (instance ?event1 Giving) \n" +
                "                  (patient ?event1 ?book)) \n" +
                "                (exists \n" +
                "                  (?event2) \n" +
                "                  (and \n" +
                "                  (attribute John-1 Male) \n" +
                "                  (instance John-1 Human) \n" +
                "                  (instance ?book Object) \n" +
                "                  (agent ?event2 John-1) \n" +
                "                  (instance ?event2 Reading) \n" +
                "                  (patient ?event2 ?book)))))";

        String expectedResult = "for all an entity and a process if male is an attribute of John-1 and female is an attribute of Mary-1 and John-1 is an instance of human and Mary-1 is an instance of human and the entity is an instance of Book and Mary-1 is an agent of the process and the process ends at John-1 and the process is an instance of Giving and the entity is a patient of the process, then there exists another process such that male is an attribute of John-1 and John-1 is an instance of human and the entity is an instance of Object and John-1 is an agent of the other process and the other process is an instance of Reading and the entity is a patient of the other process";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal: "If Mary gives John a book then he doesn't read it."
     */
    @Test
    public void testHtmlParaphraseIfMaryGivesBookJohnThenHeReadsNot()     {
        String stmt =   "(forall \n" +
                "              (?book ?event1) \n" +
                "              (=> \n" +
                "                (and \n" +
                "                  (attribute John-1 Male) \n" +
                "                  (attribute Mary-1 Female) \n" +
                "                  (instance John-1 Human) \n" +
                "                  (instance Mary-1 Human) \n" +
                "                  (instance ?book Book) \n" +
                "                  (agent ?event1 Mary-1) \n" +
                "                  (destination ?event1 John-1) \n" +
                "                  (instance ?event1 Giving) \n" +
                "                  (patient ?event1 ?book)) \n" +
                "                (not \n" +
                "                   (exists \n" +
                "                       (?event2) \n" +
                "                       (and \n" +
                "                       (attribute John-1 Male) \n" +
                "                       (instance John-1 Human) \n" +
                "                       (instance ?book Object) \n" +
                "                       (agent ?event2 John-1) \n" +
                "                       (instance ?event2 Reading) \n" +
                "                       (patient ?event2 ?book))))))";

        String expectedResult = "for all an entity and a process if male is an attribute of John-1 and female is an attribute of Mary-1 and John-1 is an instance of human and Mary-1 is an instance of human and the entity is an instance of Book and Mary-1 is an agent of the process and the process ends at John-1 and the process is an instance of Giving and the entity is a patient of the process, then there doesn't exist another process such that male is an attribute of John-1 and John-1 is an instance of human and the entity is an instance of Object and John-1 is an agent of the other process and the other process is an instance of Reading and the entity is a patient of the other process";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testHtmlParaphraseDrivingThenSeeingIf()     {
        String stmt =       "(=> \n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H))\n" +
                "               (exists (?S)\n" +
                "                   (and\n" +
                "                       (instance ?S Seeing)\n" +
                "                       (agent ?S ?H))))";

        String expectedResult = "if a human drives, then the human performs a seeing";

        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * We use this to test how much the antecedent and the consequent affect each other.
     */
    @Test
    public void testHtmlParaphraseDrivingThenTransportedIf()     {
        String stmt =       "(=> \n" +
                "               (and\n" +
                "                   (instance ?D Driving)\n" +
                "                   (instance ?H Human)\n" +
                "                   (agent ?D ?H))\n" +
                "               (transported ?D ?H))";

        LanguageFormatter languageFormatter = new LanguageFormatter(stmt, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"),
                kb, "EnglishLanguage");
        languageFormatter.setDoInformalNLG(false);
        String expectedResult = "if a process is an instance of Driving and an agent is an instance of human and the agent is an agent of the process, then the agent is transported during the process";
        String actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));

        languageFormatter.setDoInformalNLG(true);
        expectedResult = "if a process is an instance of Driving and an agent is an instance of human and the agent is an agent of the process, then the agent is transported during the process";

        actualResult = languageFormatter.htmlParaphrase("");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }


}