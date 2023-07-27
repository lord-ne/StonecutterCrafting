package example.lordne.stonecuttercrafting.recipe;

import example.lordne.stonecuttercrafting.StonecutterCrafting;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModRecipeTypes {
    public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZER =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, StonecutterCrafting.MOD_ID);

    public static final RegistryObject<ShapelessRemainderRecipe.Serializer> SHAPELESS_REMAINDER_SERIALIZER
            = RECIPE_SERIALIZER.register(IShapelessRemainderCrafting.NAME, ShapelessRemainderRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZER.register(eventBus);
    }
}