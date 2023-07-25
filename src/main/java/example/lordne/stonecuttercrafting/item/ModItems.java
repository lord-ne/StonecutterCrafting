package example.lordne.stonecuttercrafting.item;

import example.lordne.stonecuttercrafting.StonecutterCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS
            = DeferredRegister.create(ForgeRegistries.ITEMS, StonecutterCrafting.MOD_ID);

    public static final RegistryObject<Item> STONECUTTER_NUGGET = ITEMS.register("stonecutter_nugget",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_MISC)));

    public static final RegistryObject<Item> STONECUTTER_TEMPLATE_BASIC = ITEMS.register("stonecutter_template_basic",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> STONECUTTER_TEMPLATE_REUSABLE = ITEMS.register("stonecutter_template_reusable",
            () -> new ReusableItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
