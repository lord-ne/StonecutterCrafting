package example.lordne.stonecuttercrafting.clientserverutils;

import net.minecraft.client.Minecraft;
import net.minecraft.item.crafting.RecipeManager;

class ClientSide {
    public static RecipeManager getRecipeManager() {
        try {
            return Minecraft.getInstance().level.getRecipeManager();
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
}
