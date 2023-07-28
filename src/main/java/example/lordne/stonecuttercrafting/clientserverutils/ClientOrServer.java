package example.lordne.stonecuttercrafting.clientserverutils;

import net.minecraft.item.crafting.RecipeManager;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.thread.EffectiveSide;

import javax.annotation.Nonnull;

public class ClientOrServer {
    @Nonnull
    public static RecipeManager getRecipeManager() {
        LogicalSide side = EffectiveSide.get();
        RecipeManager rtn = null;
        if (side == LogicalSide.SERVER) {
            rtn = ServerSide.getRecipeManager();
        } else /* if (side == LogicalSide.Client) */ {
            rtn = ClientSide.getRecipeManager();
        }

        if (rtn == null) {
            throw new RuntimeException("Could not retrieve recipeManager from side");
        }
        return rtn;
    }
}
