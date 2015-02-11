package com.articulate.sigma;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * LanguageFormatter tests specifically targeted toward the htmlParaphrase( ) method.
 */
public class LanguageFormatterHtmlParaphraseIntegrationTest extends IntegrationTestBase {


    /**
     * Ideal: "The oldest customer enters an invalid card."
     */
    @Test
    public void testOldestCustomerEntersCard()     {
        String stmt =   "(exists \n" +
                "                  (?card ?customer ?event ?salesperson) \n" +
                "                  (and \n" +
                "                    (forall \n" +
                "                      (?X) \n" +
                "                      (=> \n" +
                "                        (and \n" +
                "                          (instance ?X customer) \n" +
                "                          (not \n" +
                "                            (equal ?X ?customer))) \n" +
                "                        (and \n" +
                "                          (greaterThan ?val1 ?val2) \n" +
                "                          (age ?customer ?val1) \n" +
                "                          (age ?X ?val2)))) \n" +
                "                    (attribute ?card Incorrect) \n" +
                "                    (instance ?card BankCard) \n" +
                "                    (instance ?customer CognitiveAgent) \n" +
                "                    (instance ?event Motion) \n" +
                "                    (instance ?salesperson CognitiveAgent) \n" +
                "                    (patient ?event ?card) \n" +
                "                    (agent ?event ?customer) \n" +
                "                    (customer ?customer ?salesperson)))";

        String expectedResult = "there exist an object, a cognitive agent, , , a process and another cognitive agent such that for all another object if the other object is an instance of customer and the other object is not equal to the cognitive agent, then a quantity is greater than another quantity and the age of the cognitive agent is the quantity and the age of the other object is the other quantity and Incorrect is an attribute of the object and the object is an instance of BankCard and the cognitive agent is an instance of cognitive agent and the process is an instance of motion and the other cognitive agent is an instance of cognitive agent and the object is a patient of the process and the cognitive agent is an agent of the process and customer the cognitive agent and the other cognitive agent";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
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

        String expectedResult = "Bell processes a telephone";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
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
                "                (instance Bell Human) \n" +
                "                (agent ?event Blankenship) \n" +
                "                (instance ?event Process) \n" +
                "                (instance ?telephone Telephone) \n" +
                "                (patient ?event ?telephone)))";

//        String expectedResult = "there exist a process and an entity such that Bell is an instance of human and Bell is an agent of the process and the process is an instance of process and the entity is an instance of Telephone and the entity is a patient of the process";
        String expectedResult = "Blankenship processes a telephone";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
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

        String expectedResult = "for all an entity and a process if Male is an attribute of John-1 and Female is an attribute of Mary-1 and John-1 is an instance of human and Mary-1 is an instance of human and the entity is an instance of book and Mary-1 is an agent of the process and the process ends at John-1 and the process is an instance of giving and the entity is a patient of the process, then there exists another process such that Male is an attribute of John-1 and John-1 is an instance of human and the entity is an instance of object and John-1 is an agent of the other process and the other process is an instance of reading and the entity is a patient of the other process";

        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal: "An old, tall, hungry and thirsty man went to the shop."
     */
    @Test
    public void testHtmlParaphraseManGoToShop()     {
        String stmt =   "(exists \n" +
                "              (?event ?man ?shop) \n" +
                "              (and \n" +
                "                (instance ?event Transportation) \n" +
                "                (attribute ?man Hungry) \n" +
                "                (attribute ?man Old) \n" +
                "                (attribute ?man Tall) \n" +
                "                (attribute ?man Thirsty) \n" +
                "                (instance ?man Man) \n" +
                "                (instance ?shop RetailStore) \n" +
                "                (agent ?event ?man) \n" +
                "                (destination ?event ?shop)))";

        String expectedResult = "there exist a process, an agent and an entity such that the process is an instance of transportation and Hungry is an attribute of the agent and Old is an attribute of the agent and Tall is an attribute of the agent and Thirsty is an attribute of the agent and the agent is an instance of Man and the entity is an instance of RetailStore and the agent is an agent of the process and the process ends at the entity";

        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal: "The waiter pours soup into the bowl."
     */
    @Test
    public void testWaiterPoursSoupBowl()     {
        String stmt =   "(exists \n" +
                "              (?bowl ?event ?soup ?waiter) \n" +
                "              (and \n" +
                "                (instance ?bowl Artifact) \n" +
                "                (instance ?event Pouring) \n" +
                "                (instance ?soup Food) \n" +
                "                (attribute ?waiter ServicePosition) \n" +
                "                (destination ?event ?bowl) \n" +
                "                (patient ?event ?soup) \n" +
                "                (agent ?event ?waiter)))";

        String expectedResult = "there exist an entity, a process, , , another entity and an agent such that the entity is an instance of artifact and the process is an instance of Pouring and the other entity is an instance of Food and ServicePosition is an attribute of the agent and the process ends at the entity and the other entity is a patient of the process and the agent is an agent of the process";

        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal: "The man John arrives."
     */
    @Test
    public void testManJohnArrives()     {
        String stmt =   "(exists (?event)\n" +
                "              (and\n" +
                "               (instance ?event Arriving)\n" +
                "               (attribute John-1 Male)\n" +
                "               (instance John-1 Human)\n" +
                "               (agent ?event John-1)))";

        String expectedResult = "there exists a process such that the process is an instance of Arriving and Male is an attribute of John-1 and John-1 is an instance of human and John-1 is an agent of the process";

        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal: "John arrives."
     */
    @Test
    public void testJohnArrives()     {
        String stmt =   "(exists (?event)\n" +
                "              (and\n" +
                "               (instance ?event Arriving)\n" +
                "               (instance John-1 Human)\n" +
                "               (agent ?event John-1)))";

//        String expectedResult = "there exists a process such that the process is an instance of Arriving and Male is an attribute of John-1 and John-1 is an instance of human and John-1 is an agent of the process";
        String expectedResult = "John-1 arrives";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     *
     */
    @Test
    public void testFishingFish()     {
        String stmt =   "(=>\n" +
                "           (and\n" +
                "               (instance ?FISHING Fishing)\n" +
                "               (patient ?FISHING ?TARGET)\n" +
                "               (instance ?TARGET Animal))\n" +
                "           (instance ?TARGET Fish))";


//        String expectedResult = "if a process is an instance of fishing and an entity is a patient of the process and the entity is an instance of animal, then the entity is an instance of fish";
        String expectedResult = "if a process is an instance of Fishing and an entity is a patient of the process and the entity is an instance of animal, then the entity is an instance of fish";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    /**
     * Ideal: FoodForFn animal is an industry product type of food manufacturing
     */
    @Test
    public void testFoodManufacturing()     {
        String stmt =   "(industryProductType FoodManufacturing\n" +
                "           (FoodForFn Animal))";


//        String expectedResult = "FoodForFn animal is an industry product type of food manufacturing";
        String expectedResult = "industryProductType FoodManufacturing and FoodForFn animal";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

    @Test
    public void testAnimalShell()     {
        String stmt =   "(=>\n" +
                "           (and\n" +
                "               (instance ?A Animal)\n" +
                "               (instance ?S AnimalShell)\n" +
                "               (part ?S ?A))\n" +
                "           (or\n" +
                "               (instance ?A Invertebrate)\n" +
                "               (instance ?A Reptile)))";


//        String expectedResult = "if an object is an instance of animal and another object is an instance of animal shell and the other object is a part of the object, then the object is an instance of invertebrate or the object is an instance of reptile";
        String expectedResult = "if an object is an instance of animal and another object is an instance of AnimalShell and the other object is a part of the object, then the object is an instance of invertebrate or the object is an instance of reptile";
        String actualResult = LanguageFormatter.htmlParaphrase("", stmt, SigmaTestBase.kb.getFormatMap("EnglishLanguage"),
                SigmaTestBase.kb.getTermFormatMap("EnglishLanguage"),
                SigmaTestBase.kb, "EnglishLanguage");
        assertEquals(expectedResult, LanguageFormatter.filterHtml(actualResult));
    }

}