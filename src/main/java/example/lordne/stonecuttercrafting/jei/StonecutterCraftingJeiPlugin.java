package example.lordne.stonecuttercrafting.jei;

import example.lordne.stonecuttercrafting.StonecutterCrafting;
import example.lordne.stonecuttercrafting.item.ModItems;
import mcp.MethodsReturnNonnullByDefault;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@JeiPlugin
public class StonecutterCraftingJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(StonecutterCrafting.MOD_ID, StonecutterCrafting.MOD_ID);

    @Override
    @Nonnull
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.STONECUTTER_TEMPLATE_REUSABLE.get(), 1), VanillaRecipeCategoryUid.STONECUTTING);
        registration.addRecipeCatalyst(new ItemStack (ModItems.STONECUTTER_TEMPLATE_BASIC.get(), 1), VanillaRecipeCategoryUid.STONECUTTING);
    }
}
