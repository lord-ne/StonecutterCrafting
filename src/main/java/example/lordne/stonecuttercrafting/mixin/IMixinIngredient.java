package example.lordne.stonecuttercrafting.mixin;

import net.minecraft.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Ingredient.class)
public interface IMixinIngredient {
    @Accessor @Final
    Ingredient.IItemList[] getValues();
}
