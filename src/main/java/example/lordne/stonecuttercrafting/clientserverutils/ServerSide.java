package example.lordne.stonecuttercrafting.clientserverutils;

import net.minecraft.item.crafting.RecipeManager;
import net.minecraftforge.fml.server.ServerLifecycleHooks;


class ServerSide {
    public static RecipeManager getRecipeManager() {
        try {
            return ServerLifecycleHooks.getCurrentServer().getRecipeManager();
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
}
