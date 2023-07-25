package example.lordne.stonecuttercrafting.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ReusableItem extends Item {
    public ReusableItem(Properties p_i48487_1_) {
        super(p_i48487_1_);
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack itemStack) {
        return itemStack.copy();
    }
}
