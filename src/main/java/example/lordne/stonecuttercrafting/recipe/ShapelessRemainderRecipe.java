package example.lordne.stonecuttercrafting.recipe;

import example.lordne.stonecuttercrafting.StonecutterCrafting;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraftforge.common.util.RecipeMatcher;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraft.item.crafting.ShapelessRecipe;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

public class ShapelessRemainderRecipe implements IShapelessRemainderCrafting {
    private final static int MAX_WIDTH = 3;
    private final static int MAX_HEIGHT = 3;

    private final ResourceLocation id;
    private final String group;
    private final ItemStack result;
    private final NonNullList<Ingredient> ingredients;
    private final NonNullList<Ingredient> ingredientsNormal;
    private final NonNullList<Ingredient> ingredientsUnused;
    private final boolean isSimple;

    public ShapelessRemainderRecipe(ResourceLocation id, String group, ItemStack result, NonNullList<Ingredient> ingredientsNormal, NonNullList<Ingredient> ingredientsUnused) {
        this.id = id;
        this.group = group;
        this.result = result;
        this.ingredientsNormal = ingredientsNormal;
        this.ingredientsUnused = ingredientsUnused;
        this.ingredients = NonNullList.create();
        this.ingredients.addAll(ingredientsNormal);
        this.ingredients.addAll(ingredientsUnused);
        this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    @Nonnull
    public ResourceLocation getId() {
        return this.id;
    }

    // TODO: Actual serializer
    @Nonnull
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.SHAPELESS_REMAINDER_SERIALIZER.get();
    }

    @Nonnull
    public String getGroup() {
        return this.group;
    }

    @Nonnull
    public ItemStack getResultItem() {
        return this.result;
    }

    @Nonnull
    public NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    @ParametersAreNonnullByDefault
    public boolean matches(CraftingInventory inv, World world) {
        RecipeItemHelper recipeitemhelper = new RecipeItemHelper();
        List<ItemStack> inputs = new ArrayList<>();
        int count = 0;

        for(int j = 0; j < inv.getContainerSize(); ++j) {
            ItemStack itemstack = inv.getItem(j);
            if (!itemstack.isEmpty()) {
                ++count;
                if (isSimple) {
                    recipeitemhelper.accountStack(itemstack, 1);
                }
                else inputs.add(itemstack);
            }
        }

        return ((count == this.ingredients.size())
                && (isSimple
                        ? recipeitemhelper.canCraft(this, (IntList)null)
                        : RecipeMatcher.findMatches(inputs,  this.ingredients) != null
                )
        );
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    public ItemStack assemble(CraftingInventory inv) {
        return this.result.copy();
    }

    public boolean canCraftInDimensions(int dim1, int dim2) {
        return dim1 * dim2 >= this.ingredients.size();
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {

        List<ItemStack> inputs = new ArrayList<>();
        int[] slotToInput = new int[inv.getContainerSize()];
        for(int slot = 0; slot < inv.getContainerSize(); ++slot) {
            ItemStack itemstack = inv.getItem(slot);
            if (!itemstack.isEmpty()) {
                slotToInput[slot] = inputs.size();
                inputs.add(itemstack);
            }
        }

        int[] inputToIngredient = RecipeMatcher.findMatches(inputs,  this.ingredients);
        if (inputToIngredient == null) {
            throw new IllegalArgumentException("getRemainingItems() called when not matching");
        }

        NonNullList<ItemStack> remainderList = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

        for(int slot = 0; slot < inv.getContainerSize(); ++slot) {
            ItemStack itemStack = inv.getItem(slot);
            if (!itemStack.isEmpty()) {
                int inputIndex = slotToInput[slot];
                int ingredientIndex = inputToIngredient[inputIndex];
                if (ingredientIndex >= ingredientsNormal.size()) {
                    // This is an unused ingredient
                    ItemStack remainderItemStack = itemStack.copy();
                    remainderItemStack.setCount(1);
                    remainderList.set(slot, remainderItemStack);
                } else {
                    // This is a normal ingredient
                    if (itemStack.hasContainerItem()) {
                        remainderList.set(slot, itemStack.getContainerItem());
                    }
                }
            }
        }

        return remainderList;
    }


//    public static class ShaplessRemainderRecipeType implements IRecipeType<ShapelessRemainderRecipe> {
//        @Override
//        public String toString() {
//            return ShapelessRemainderRecipe.TYPE_ID.toString();
//        }
//    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ShapelessRemainderRecipe> {
        private static final ResourceLocation NAME = new ResourceLocation(StonecutterCrafting.MOD_ID, IShapelessRemainderCrafting.ID_NAME);
        @Nonnull
        @ParametersAreNonnullByDefault
        public ShapelessRemainderRecipe fromJson(ResourceLocation id, JsonObject jsonObj) {
            String s = JSONUtils.getAsString(jsonObj, "group", "");
            NonNullList<Ingredient> ingredientsNormal = itemsFromJson(JSONUtils.getAsJsonArray(jsonObj, "ingredientsNormal"));
            NonNullList<Ingredient> ingredientsUnused = itemsFromJson(JSONUtils.getAsJsonArray(jsonObj, "ingredientsUnused"));

            if (ingredientsNormal.isEmpty() && ingredientsUnused.isEmpty()) {
                throw new JsonParseException("No ingredients for shapeless recipe");
            } else if (ingredientsNormal.size() + ingredientsUnused.size() > MAX_WIDTH * MAX_HEIGHT) {
                throw new JsonParseException("Too many ingredients for shapeless recipe the max is " + (MAX_WIDTH * MAX_HEIGHT));
            } else {
                ItemStack result = ShapedRecipe.itemFromJson(JSONUtils.getAsJsonObject(jsonObj, "result"));
                return new ShapelessRemainderRecipe(id, s, result, ingredientsNormal, ingredientsUnused);
            }
        }

        private static NonNullList<Ingredient> itemsFromJson(JsonArray jsonArr) {
            NonNullList<Ingredient> nonnulllist = NonNullList.create();

            for(int i = 0; i < jsonArr.size(); ++i) {
                Ingredient ingredient = Ingredient.fromJson(jsonArr.get(i));
                if (!ingredient.isEmpty()) {
                    nonnulllist.add(ingredient);
                }
            }

            return nonnulllist;
        }

        @Nonnull
        @ParametersAreNonnullByDefault
        public ShapelessRemainderRecipe fromNetwork(ResourceLocation id, PacketBuffer packetBuf) {
            String s = packetBuf.readUtf(32767);

            int sizeNormal = packetBuf.readVarInt();
            NonNullList<Ingredient> ingredientsNormal = NonNullList.withSize(sizeNormal, Ingredient.EMPTY);
            ingredientsNormal.replaceAll(ignored -> Ingredient.fromNetwork(packetBuf));

            int sizeUnused = packetBuf.readVarInt();
            NonNullList<Ingredient> ingredientsUnused = NonNullList.withSize(sizeUnused, Ingredient.EMPTY);
            ingredientsNormal.replaceAll(ignored -> Ingredient.fromNetwork(packetBuf));

            ItemStack result = packetBuf.readItem();

            return new ShapelessRemainderRecipe(id, s, result, ingredientsNormal, ingredientsUnused);
        }

        public void toNetwork(PacketBuffer packetBuf, ShapelessRemainderRecipe recipe) {
            packetBuf.writeUtf(recipe.group);

            packetBuf.writeVarInt(recipe.ingredientsNormal.size());
            for(Ingredient ingredient : recipe.ingredientsNormal) {
                ingredient.toNetwork(packetBuf);
            }

            packetBuf.writeVarInt(recipe.ingredientsUnused.size());
            for(Ingredient ingredient : recipe.ingredientsUnused) {
                ingredient.toNetwork(packetBuf);
            }

            packetBuf.writeItem(recipe.result);
        }
    }
}
