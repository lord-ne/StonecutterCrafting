package example.lordne.stonecuttercrafting.util;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class Utils {
    private Utils() {}; // Cannot be instantiated

    public static List<ItemStack> collectInventoryInputs(IInventory inv) {
        List<ItemStack> inputs = new ArrayList<>();

        for(int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack itemstack = inv.getItem(i);
            if (!itemstack.isEmpty()) {
                inputs.add(itemstack);
            }
        }

        return inputs;
    }

    public static int indexOfAlways(int[] arr, int x) {
        for (int i = 0; i < arr.length; ++i) {
            if (arr[i] == x) {
                return i;
            }
        }
        throw new RuntimeException("Index not found");
    }
}
