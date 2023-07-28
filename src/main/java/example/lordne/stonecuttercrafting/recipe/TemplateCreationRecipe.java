package example.lordne.stonecuttercrafting.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import example.lordne.stonecuttercrafting.clientserverutils.ClientOrServer;
import example.lordne.stonecuttercrafting.item.ModItems;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import java.util.Objects;
import java.util.Optional;

import mcp.MethodsReturnNonnullByDefault;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

public class TemplateCreationRecipe extends ShapedRecipe {
    public static final String NAME = "template_creation_crafting";

    private final int inputSlot;
    private final int outputSlot;

    public TemplateCreationRecipe(ShapedRecipe toConvert) {
        this(toConvert.getId(), toConvert.getGroup(), toConvert.getWidth(), toConvert.getHeight(), toConvert.getIngredients(), toConvert.getResultItem());
    }

    public TemplateCreationRecipe(ResourceLocation id, String group, int width, int height, NonNullList<Ingredient> recipeItems, ItemStack result) {
        super(id, group, width, height, recipeItems, result);

        ItemStack inputItem = new ItemStack(ModItems.ANY_STONECUTTER_INPUT.get(), 1);
        ItemStack outputItem = new ItemStack(ModItems.ANY_STONECUTTER_OUTPUT.get(), 1);

        int inputSlot = -1;
        int outputSlot = -1;

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

            if (itemStack.equals(inputItem, false)) {
                if (inputSlot != -1) {
                    throw new JsonSyntaxException("Multiple any_stonecutter_input blocks not supported");
                }
                inputSlot = slot;
            } else if (itemStack.equals(outputItem, false)) {
                if (outputSlot != -1) {
                    throw new JsonSyntaxException("Multiple any_stonecutter_output blocks not supported");
                }
                outputSlot = slot;
            }
        }

        if (inputSlot == -1) {
            throw new JsonSyntaxException("No any_stonecutter_input block in recipe");
        }

        if (outputSlot == -1) {
            throw new JsonSyntaxException("No any_stonecutter_output block in recipe");
        }

        this.inputSlot = inputSlot;
        this.outputSlot = outputSlot;

    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean matches(CraftingInventory inv, World world) {
        MatchResults results = basicMatch(inv);
        if (!results.matches) {
            return false;
        }

        ItemStack inputItem = inv.getItem(results.invSlotInput);
        ItemStack outputItem = inv.getItem(results.invSlotOutput);

        Optional<StonecuttingRecipe> optR = tryFindMatchingRecipe(world.getRecipeManager(), inputItem, outputItem);
        return optR.isPresent();
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public ItemStack assemble(CraftingInventory inv) {
        MatchResults results = basicMatch(inv);
        if (!results.matches) {
            throw new RuntimeException("TemplateCreationRecipe failed to match in assemble()");
        }

        ItemStack inputItem = inv.getItem(results.invSlotInput);
        ItemStack outputItem = inv.getItem(results.invSlotOutput);

        Optional<StonecuttingRecipe> optR = tryFindMatchingRecipe(ClientOrServer.getRecipeManager(), inputItem, outputItem);

        if (!optR.isPresent()) {
            throw new RuntimeException("TemplateCreationRecipe failed to match in assemble()");
        }

        StonecuttingRecipe recipe = optR.get();

        CompoundNBT outputItemNBT = new CompoundNBT();
        ItemStack outputItemStack = recipe.getResultItem();
        outputItemStack.save(outputItemNBT);

        Ingredient inputIngredient = recipe.getIngredients().get(0);
        String inputItemJsonString = inputIngredient.toJson().toString();

        ItemStack rtn = getResultItem().copy();
        rtn.addTagElement("stonecuttercrafting/output_ItemStack_NBT", outputItemNBT);
        rtn.getTag().putString("stonecuttercrafting/input_Ingredient_JsonString", inputItemJsonString);

        String loreString = "[" +
                ingredientDisplayComponent(
                        inputIngredient,
                        ",\"color\":\"aqua\",\"italic\":false",
                        ", {\"text\":\", \", \"color\":\"white\",\"italic\":false}, {\"text\":\"...\", \"color\":\"aqua\",\"italic\":false}"
                ) + "," +
                "{\"text\":\" -> \",\"color\":\"white\",\"italic\":false}," +
                itemStackDisplayComponent(outputItemStack, ",\"color\":\"aqua\",\"italic\":false") +
            "]";

        ListNBT loreLines = new ListNBT();
        loreLines.add(StringNBT.valueOf(loreString));
        rtn.getOrCreateTagElement("display").put("Lore", loreLines);

        return rtn;
    }

    private String itemStackDisplayComponent(ItemStack itemStack, String formatSuffix) {
        String countPrefix = (itemStack.getCount() == 1 ? "" : "{\"text\":\""+ itemStack.getCount() + "x \"" + formatSuffix + "},");
        return countPrefix + "{\"translate\":\"" + itemStack.getDescriptionId() + "\"" + formatSuffix + "}";
    }

    // This is very unnecessary
    private String ingredientDisplayComponent(Ingredient ingredient, String formatSuffix, String etcSuffix) {
        int length = ingredient.getItems().length;
        String fallbackName = null;
        if (length == 0) {
            fallbackName = "{\"text\":\"n/a\"" + formatSuffix + "}";
        } else if (length == 1) {
            fallbackName = "{\"translate\":\"" + ingredient.getItems()[0].getDescriptionId() + "\"" + formatSuffix + "}";
        } else {
            fallbackName = "{\"translate\":\"" + ingredient.getItems()[0].getDescriptionId() + "\"" + formatSuffix + "}"
                    + etcSuffix;
        }
        try {
            JsonElement ingJson = ingredient.toJson();
            JsonArray ingJsonArr;
            if (!ingJson.isJsonArray()) {
                ingJsonArr = new JsonArray();
                ingJsonArr.add(ingJson);
            } else {
                ingJsonArr = ingJson.getAsJsonArray();
            }

            if (ingJsonArr.size() == 0) {
                return fallbackName;
            }

            if (!ingJsonArr.get(0).isJsonObject()) {
                return fallbackName;
            }

            JsonObject jsonObj = ingJsonArr.get(0).getAsJsonObject();

            if (jsonObj.size() == 1 && jsonObj.has("tag")
                    && jsonObj.get("tag").isJsonPrimitive()
                    && jsonObj.get("tag").getAsJsonPrimitive().isString()) {

                return "{\"text\":\"Any #" + jsonObj.get("tag").getAsString() + "\"" + formatSuffix + "}" + (ingJsonArr.size() > 1 ? etcSuffix : "");
            }

        } catch (Exception e) {
            System.out.println(e);
            return fallbackName;
        }
        return fallbackName;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
        MatchResults results = basicMatch(inv);
        if (!results.matches) {
            throw new RuntimeException("TemplateCreationRecipe failed to match in getRemainingItems()");
        }

        NonNullList<ItemStack> remainingItems = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

        for(int slot = 0; slot < remainingItems.size(); ++slot) {
            ItemStack item = inv.getItem(slot);
            if (slot == results.invSlotInput || slot == results.invSlotOutput) {
                ItemStack remainder = item.copy();
                remainder.setCount(1);
                remainingItems.set(slot, remainder);
            } else if (item.hasContainerItem()) {
                remainingItems.set(slot, item.getContainerItem());
            }
        }

        return remainingItems;
    }

    Optional<StonecuttingRecipe> tryFindMatchingRecipe(RecipeManager recipeManager, ItemStack inputItem, ItemStack outputItem) {
        return recipeManager.getAllRecipesFor(IRecipeType.STONECUTTING).stream().filter(
                r -> r.getIngredients().get(0).test(inputItem)
                        && matchExcludingSize(r.getResultItem(), outputItem)
        ).findFirst();
    }

    private boolean matchExcludingSize(ItemStack first, ItemStack second) {
        return !first.isEmpty() && !second.isEmpty()
                && first.getItem() == second.getItem()
                && ItemStack.tagMatches(first, second);
    }

    private static class MatchResults {
        public boolean matches = false;
        public int invSlotInput = -1;
        public int invSlotOutput = -1;

        public MatchResults() {};
    }

    private MatchResults basicMatch(CraftingInventory inv) {
        MatchResults results = new MatchResults();
        for(int offsetX = 0; offsetX <= inv.getWidth() - this.getWidth(); ++offsetX) {
            for(int offsetY = 0; offsetY <= inv.getHeight() - this.getHeight(); ++offsetY) {
                this.tryBasicMatchAtOffset(inv, offsetX, offsetY, results);
                if (results.matches) {
                    return results;
                }
            }
        }

        results.matches = false;
        return results;
    }

    private void tryBasicMatchAtOffset(CraftingInventory inv, int offsetX, int offsetY, MatchResults outResults) {
        for(int invX = 0; invX < inv.getWidth(); ++invX) {
            for(int invY = 0; invY < inv.getHeight(); ++invY) {
                int recipeX = invX - offsetX;
                int recipeY = invY - offsetY;

                int recipeSlot = -1;
                Ingredient ingredient = Ingredient.EMPTY;
                if (recipeX >= 0 && recipeY >= 0 && recipeX < this.getWidth() && recipeY < this.getHeight()) {
                    recipeSlot = recipeX + recipeY * this.getWidth();
                    ingredient = this.getIngredients().get(recipeSlot);
                }

                int invSlot = invX + invY * inv.getWidth();
                ItemStack item = inv.getItem(invSlot);
                if (recipeSlot == inputSlot) {
                    if (item.isEmpty()) {
                        outResults.matches = false;
                        return;
                    }
                    outResults.invSlotInput = invSlot;
                    // continue;
                } else if (recipeSlot == outputSlot) {
                    if (item.isEmpty()) {
                        outResults.matches = false;
                        return;
                    }
                    outResults.invSlotOutput = invSlot;
                    // continue;
                } else if (!ingredient.test(item)) {
                    outResults.matches = false;
                    return;
                }
                // else, continue;
            }
        }

        outResults.matches = true;
        return;
    }

    @Override
    @Nonnull
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.TEMPLATE_CREATION_SERIALIZER.get();
    }

    @MethodsReturnNonnullByDefault
    @ParametersAreNonnullByDefault
    public static class Serializer
            extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>>
            implements IRecipeSerializer<TemplateCreationRecipe>
    {
        public TemplateCreationRecipe fromJson(ResourceLocation id, JsonObject jsonObj) {
            return new TemplateCreationRecipe(IRecipeSerializer.SHAPED_RECIPE.fromJson(id, jsonObj));
        }

        public TemplateCreationRecipe fromNetwork(ResourceLocation id, PacketBuffer packetBuf) {
            return new TemplateCreationRecipe(Objects.requireNonNull(IRecipeSerializer.SHAPED_RECIPE.fromNetwork(id, packetBuf)));
        }

        public void toNetwork(PacketBuffer packetBuf, TemplateCreationRecipe recipe) {
            IRecipeSerializer.SHAPED_RECIPE.toNetwork(packetBuf, recipe);
        }
    }

}