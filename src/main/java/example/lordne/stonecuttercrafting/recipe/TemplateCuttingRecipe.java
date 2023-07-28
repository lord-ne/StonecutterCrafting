package example.lordne.stonecuttercrafting.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import example.lordne.stonecuttercrafting.item.ModItems;
import example.lordne.stonecuttercrafting.util.Utils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.RecipeMatcher;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class TemplateCuttingRecipe extends ShapelessRecipe {
    public static final String NAME = "template_cutting_crafting";

    private final int numInputs;
    private final NonNullList<Predicate<ItemStack>> testPredicateIngredients;
    private final int templateRecipeIndex = 0; // Assume first listed item is template

    public TemplateCuttingRecipe(ShapelessRecipe toConvert) {
        this(toConvert.getId(), toConvert.getGroup(), toConvert.getResultItem(), toConvert.getIngredients());
    }

    public TemplateCuttingRecipe(ResourceLocation id, String group, ItemStack result, NonNullList<Ingredient> ingredients) {
        this(id, group, result,  preconstructIngredients(ingredients));
    }

    protected TemplateCuttingRecipe(ResourceLocation id, String group, ItemStack result, PreconstructInfo preConstructInfo) {
        super(id, group, result, preConstructInfo.sortedIngredients);
        this.numInputs = preConstructInfo.numInputs;
        this.testPredicateIngredients = preConstructInfo.testPredicateIngredients;

        if (this.numInputs < 1) {
            throw new JsonSyntaxException("TemplateCuttingRecipe with no any_stonecutter_input");
        }

        if (this.numInputs >= this.getIngredients().size()) {
            throw new JsonSyntaxException("TemplateCuttingRecipe with no template input");
        }

        if (!this.getResultItem().getItem().equals(ModItems.ANY_STONECUTTER_OUTPUT.get())) {
            throw new JsonSyntaxException("TemplateCuttingRecipe output is not any_stonecutter_output");
        }
    }

    private static class PreconstructInfo {
        public NonNullList<Ingredient> sortedIngredients;
        public int numInputs;
        public NonNullList<Predicate<ItemStack>> testPredicateIngredients;
    }

    static PreconstructInfo preconstructIngredients(NonNullList<Ingredient> ingredients) {
        PreconstructInfo rtn = new PreconstructInfo();

        rtn.sortedIngredients = NonNullList.withSize(ingredients.size(), Ingredient.EMPTY);
        rtn.testPredicateIngredients = NonNullList.withSize(ingredients.size(), Ingredient.EMPTY);

        final ItemStack anyInputItem = new ItemStack(ModItems.ANY_STONECUTTER_INPUT.get(), 1);
        int frontIndex = 0;
        int backIndex = ingredients.size() - 1;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.test(anyInputItem)) {
                rtn.sortedIngredients.set(backIndex, ingredient);
                rtn.testPredicateIngredients.set(backIndex, new AnyNonempty());
                --backIndex;
            } else {
                rtn.sortedIngredients.set(frontIndex, ingredient);
                rtn.testPredicateIngredients.set(frontIndex,ingredient);
                ++frontIndex;
            }
        }
        assert backIndex + 1 == frontIndex;
        rtn.numInputs = ingredients.size() - frontIndex;

        return rtn;
    }

    private static class AnyNonempty implements Predicate<ItemStack> {
        @Override
        public boolean test(ItemStack itemStack) {
            return !itemStack.isEmpty();
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean matches(CraftingInventory inv, World world) {
        List<ItemStack> inputs = Utils.collectInventoryInputs(inv);
        int[] matches = RecipeMatcher.findMatches(inputs,  this.testPredicateIngredients);

        if (matches == null) {
            return false;
        }

        ItemStack template = inputs.get(Utils.indexOfAlways(matches, templateRecipeIndex));

        ItemStack outputStack = readOutputFromTemplate(template);
        if (outputStack == null || outputStack.getCount() * numInputs > outputStack.getMaxStackSize()) {
            // We can't craft with this many because it would be stacking more than the max
            return false;
        }

        Ingredient inputIngredient = readInputFromTemplate(template);
        if (inputIngredient == null) {
            return false;
        }

        int firstInputRecipeSlot = this.getIngredients().size() - this.numInputs;
        for (int inputSlot = 0; inputSlot < matches.length; ++inputSlot) {
            int recipeSlot = matches[inputSlot];
            if (recipeSlot >= firstInputRecipeSlot) {
                if (!inputIngredient.test(inputs.get(inputSlot))) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public ItemStack assemble(CraftingInventory inv) {
        List<ItemStack> inputs = Utils.collectInventoryInputs(inv);
        int[] matches = RecipeMatcher.findMatches(inputs,  this.testPredicateIngredients);
        if (matches == null) {
            throw new RuntimeException("TemplateCuttingRecipe did not match in assemble()");
        }

        ItemStack template = inputs.get(Utils.indexOfAlways(matches, templateRecipeIndex));

        ItemStack outputStack = readOutputFromTemplate(template);
        if (outputStack == null) {
            throw new RuntimeException("TemplateCuttingRecipe could not read NBT in assemble()");
        }

        outputStack.setCount(outputStack.getCount() * this.numInputs);

        return outputStack;
    }

    @ParametersAreNonnullByDefault
    private Ingredient readInputFromTemplate(ItemStack template) {
        try {
            CompoundNBT mainNBT = template.getTagElement("stonecuttercrafting/recipeData");
            if (mainNBT == null) {
                return null;
            }
            // 8 is the String NBT tag
            if (!mainNBT.contains("input_Ingredient_JsonString", 8)) {
                return null;
            }
            String jsonString = mainNBT.getString("input_Ingredient_JsonString");
            JsonElement json = JSONUtils.parse(jsonString);
            return Ingredient.fromJson(json);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    private ItemStack readOutputFromTemplate(ItemStack template) {
        try {
            CompoundNBT mainNBT = template.getTagElement("stonecuttercrafting/recipeData");
            if (mainNBT == null) {
                return null;
            }
            // 10 is the Compound NBT tag
            if (!mainNBT.contains("output_ItemStack_NBT", 10)) {
                return null;
            }
            CompoundNBT nbt = mainNBT.getCompound("output_ItemStack_NBT");
            return ItemStack.of(nbt);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    @Override
    @Nonnull
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.TEMPLATE_CUTTING_SERIALIZER.get();
    }

    @MethodsReturnNonnullByDefault
    @ParametersAreNonnullByDefault
    public static class Serializer
            extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>>
            implements IRecipeSerializer<TemplateCuttingRecipe>
    {
        public TemplateCuttingRecipe fromJson(ResourceLocation id, JsonObject jsonObj) {
            return new TemplateCuttingRecipe(IRecipeSerializer.SHAPELESS_RECIPE.fromJson(id, jsonObj));
        }

        public TemplateCuttingRecipe fromNetwork(ResourceLocation id, PacketBuffer packetBuf) {
            return new TemplateCuttingRecipe(Objects.requireNonNull(IRecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id, packetBuf)));
        }

        public void toNetwork(PacketBuffer packetBuf, TemplateCuttingRecipe recipe) {
            IRecipeSerializer.SHAPELESS_RECIPE.toNetwork(packetBuf, recipe);
        }
    }
}
