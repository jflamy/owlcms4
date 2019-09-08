package app.owlcms.relay.backend;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

class StaticData {

    private static final String MINERAL_WATER = "Mineral Water";
    private static final String SOFT_DRINK = "Soft Drink";
    private static final String COFFEE = "Coffee";
    private static final String TEA = "Tea";
    private static final String DAIRY = "Dairy";
    private static final String CIDER = "Cider";
    private static final String BEER = "Beer";
    private static final String WINE = "Wine";
    private static final String OTHER = "Other";

    public static final String UNDEFINED = "Undefined";
    
    static final Map<String, String> BEVERAGES = new LinkedHashMap<>();

    static {
        Stream.of("Evian",
                "Voss",
                "Veen",
                "San Pellegrino",
                "Perrier")
                .forEach(name -> BEVERAGES.put(name, MINERAL_WATER));

        Stream.of("Coca-Cola",
                "Fanta",
                "Sprite")
                .forEach(name -> BEVERAGES.put(name, SOFT_DRINK));

        Stream.of("Maxwell Ready-to-Drink Coffee",
                "Nescafé Gold",
                "Starbucks East Timor Tatamailau")
                .forEach(name -> BEVERAGES.put(name, COFFEE));

        Stream.of("Prince Of Peace Organic White Tea",
                "Pai Mu Tan White Peony Tea",
                "Tazo Zen Green Tea",
                "Dilmah Sencha Green Tea",
                "Twinings Earl Grey",
                "Twinings Lady Grey",
                "Classic Indian Chai")
                .forEach(name -> BEVERAGES.put(name, TEA));

        Stream.of("Cow's Milk",
                "Goat's Milk",
                "Unicorn's Milk",
                "Salt Lassi",
                "Mango Lassi",
                "Airag")
                .forEach(name -> BEVERAGES.put(name, DAIRY));

        Stream.of("Crowmoor Extra Dry Apple",
                "Golden Cap Perry",
                "Somersby Blueberry",
                "Kopparbergs Naked Apple Cider",
                "Kopparbergs Raspberry",
                "Kingstone Press Wild Berry Flavoured Cider",
                "Crumpton Oaks Apple",
                "Frosty Jack's",
                "Ciderboys Mad Bark",
                "Angry Orchard Stone Dry",
                "Walden Hollow",
                "Fox Barrel Wit Pear")
                .forEach(name -> BEVERAGES.put(name, CIDER));

        Stream.of("Budweiser",
                "Miller",
                "Heineken",
                "Holsten Pilsener",
                "Krombacher",
                "Weihenstephaner Hefeweissbier",
                "Ayinger Kellerbier",
                "Guinness Draught",
                "Kilkenny Irish Cream Ale",
                "Hoegaarden White",
                "Barbar",
                "Corsendonk Agnus Dei",
                "Leffe Blonde",
                "Chimay Tripel",
                "Duvel",
                "Pilsner Urquell",
                "Kozel",
                "Staropramen",
                "Lapin Kulta IVA",
                "Kukko Pils III",
                "Finlandia Sahti")
                .forEach(name -> BEVERAGES.put(name, BEER));

        Stream.of("Jacob's Creek Classic Shiraz",
                "Chateau d’Yquem Sauternes",
                "Oremus Tokaji Aszú 5 Puttonyos")
                .forEach(name -> BEVERAGES.put(name, WINE));

        Stream.of("Pan Galactic Gargle Blaster",
                "Mead",
                "Soma")
                .forEach(name -> BEVERAGES.put(name, OTHER));

        BEVERAGES.put("", UNDEFINED);
    }

    /** This class is not meant to be instantiated. */
    private StaticData() {
    }
}
