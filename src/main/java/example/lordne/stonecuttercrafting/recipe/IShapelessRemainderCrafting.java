package example.lordne.stonecuttercrafting.recipe;

import example.lordne.stonecuttercrafting.StonecutterCrafting;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import javax.annotation.Nonnull;

public interface IShapelessRemainderCrafting extends ICraftingRecipe {
    String NAME = "crafting_shapeless_remainder";

    @Override
    @Nonnull
    default IRecipeType<?> getType(){
        return IRecipeType.CRAFTING;
    }

    @Override
    default boolean isSpecial() {
        return false;
    }
}
