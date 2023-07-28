package example.lordne.stonecuttercrafting.recipe;

import com.google.gson.JsonObject;
import example.lordne.stonecuttercrafting.util.Utils;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.RecipeMatcher;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;

import java.util.List;
import java.util.Objects;

public class TemplateDuplicationRecipe extends ShapelessRecipe {
    public static final String NAME = "template_duplication_crafting";

    private final int templateRecipeIndex;

    public TemplateDuplicationRecipe(ShapelessRecipe toConvert) {
        this(toConvert.getId(), toConvert.getGroup(), toConvert.getResultItem(), toConvert.getIngredients());
    }

    public TemplateDuplicationRecipe(ResourceLocation id, String group, ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, result, ingredients);

        // Just assume that the first ingredient is the template. This is kind of hacky, but
        // in practice there should be no need to create instances of these with more than
        // one ingredient
        this.templateRecipeIndex = 0; // findTemplateIndex();
    }

    /*
    private int findTemplateIndex() {
        int templateRecipeIndex = -1;
        for (int i = 0; i < getIngredients().size(); ++i) {
            Ingredient ingredient = getIngredients().get(i);
            if (ingredientIsTemplate(ingredient)) {
                if (templateRecipeIndex == -1) {
                    templateRecipeIndex = i;
                } else {
                    throw new JsonSyntaxException("Multiple template items in TemplateDuplicationRecipe: slot " + templateRecipeIndex + " and slot " + i);
                }
            }
        }

        if (templateRecipeIndex == -1) {
            throw new JsonSyntaxException("Not template item found in TemplateDuplicationRecipe");
        }

        return templateRecipeIndex;
    }

    private static boolean ingredientIsTemplate(Ingredient ingredient) {
        ItemStack[] itemStacks = ingredient.getItems();
        if (itemStacks.length == 0) {
            return false;
        }
        for (ItemStack itemStack : itemStacks) {
            if (!itemStack.hasTag() ||
                    !itemStack.getTag().contains("stonecuttercrafting/recipeData")) {
                return false;
            }
        }
        return true;
    }
    */

    @Override
    @ParametersAreNonnullByDefault
    public boolean matches(CraftingInventory inv, World world) {
        return findTemplate(inv) != null;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public ItemStack assemble(CraftingInventory inv) {
        ItemStack template = findTemplate(inv);
        if (template == null) {
            throw new RuntimeException("Did not match in assemble()");
        }
        ItemStack rtn = getResultItem().copy();
        rtn.addTagElement("stonecuttercrafting/recipeData",
                Objects.requireNonNull(template.getTagElement("stonecuttercrafting/recipeData")));

        CompoundNBT display = template.getTagElement("display");
        if (display != null) {
            INBT lore = display.get("Lore");
            if (lore != null) {
                rtn.getOrCreateTagElement("display").put("Lore", lore);
            }
        }
        return rtn;
    }

    private ItemStack findTemplate(CraftingInventory inv) {
        List<ItemStack> inputs = Utils.collectInventoryInputs(inv);

        ItemStack templateCandidate;
        if (this.getIngredients().size() == 1) {
            if (inputs.size() != 1
                    || !this.getIngredients().get(this.templateRecipeIndex).test(inputs.get(0))) {
                return null;
            }
            templateCandidate = inputs.get(0);
        } else {
            int[] matches = RecipeMatcher.findMatches(inputs,  this.getIngredients());
            if (matches == null) {
                return null;
            }
            int inputSlot = Utils.indexOfAlways(matches, templateRecipeIndex);
            templateCandidate = inputs.get(inputSlot);
        }

        if (templateCandidate.hasTag() && templateCandidate.getTag().contains("stonecuttercrafting/recipeData")) {
            return templateCandidate;
        } else {
            return null;
        }
    }

    @Override
    @Nonnull
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.TEMPLATE_DUPLICATION_SERIALIZER.get();
    }

    @MethodsReturnNonnullByDefault
    @ParametersAreNonnullByDefault
    public static class Serializer
            extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>>
            implements IRecipeSerializer<TemplateDuplicationRecipe>
    {
        public TemplateDuplicationRecipe fromJson(ResourceLocation id, JsonObject jsonObj) {
            return new TemplateDuplicationRecipe(IRecipeSerializer.SHAPELESS_RECIPE.fromJson(id, jsonObj));
        }

        public TemplateDuplicationRecipe fromNetwork(ResourceLocation id, PacketBuffer packetBuf) {
            return new TemplateDuplicationRecipe(Objects.requireNonNull(IRecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id, packetBuf)));
        }

        public void toNetwork(PacketBuffer packetBuf, TemplateDuplicationRecipe recipe) {
            IRecipeSerializer.SHAPELESS_RECIPE.toNetwork(packetBuf, recipe);
        }
    }
}
