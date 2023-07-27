package example.lordne.stonecuttercrafting.recipe;

import com.google.gson.JsonSyntaxException;
import example.lordne.stonecuttercrafting.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Stream;

public class TemplateCreationRecipe extends ShapedRecipe {
    private final int input_slot;
    private final int output_slot;

    public TemplateCreationRecipe(ShapedRecipe toConvert) {
        this(toConvert.getId(), toConvert.getGroup(), toConvert.getWidth(), toConvert.getHeight(), toConvert.getIngredients(), toConvert.getResultItem());
    }

    public TemplateCreationRecipe(ResourceLocation id, String group, int width, int height, NonNullList<Ingredient> recipeItems, ItemStack result) {
        super(id, group, width, height, recipeItems, result);

        ItemStack input_item = new ItemStack(ModItems.ANY_STONECUTTER_INPUT.get(), 1);
        ItemStack output_item = new ItemStack(ModItems.ANY_STONECUTTER_OUTPUT.get(), 1);

        int input_slot = -1;
        int output_slot = -1;

        for (int slot = 0; slot < this.getWidth() * this.getHeight(); ++slot) {
            Ingredient ingredient = this.getIngredients().get(slot);
            if (ingredient.isEmpty()) {
                continue;
            }
            ItemStack[] itemStacks = ingredient.getItems();
            if (itemStacks.length != 1) {
                continue;
            }
            ItemStack itemStack = itemStacks[0];
            if (itemStack.getCount() != 1) {
                continue;
            }

            if (itemStack.equals(input_item, false)) {
                if (input_slot != -1) {
                    throw new JsonSyntaxException("Multiple any_stonecutter_input blocks not supported");
                }
                input_slot = slot;
                // this.getIngredients().set(slot);
            } else if (itemStack.equals(output_item, false)) {
                if (input_slot != -1) {
                    throw new JsonSyntaxException("Multiple any_stonecutter_output blocks not supported");
                }
                output_slot = slot;
            }
        }

        this.input_slot = input_slot;
        this.output_slot = output_slot;
    }
/*
    private static class IngredientAnyNonempty extends Ingredient {
        final Ingredient base;

        public IngredientAnyNonempty(Ingredient base) {
            super(Stream.of());
            this.base = base;
        }

        @Override
        public boolean test(@Nullable ItemStack other) {
            return other != null && !other.isEmpty();
        }

        override
    }
*/
}