package com.voiddim.item;

import com.voiddim.handler.TeleportHelper;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/**
 * Cristal de retorno: use para voltar ao overworld a partir de qualquer dimensão.
 * Consome 1 do stack ao usar. Não faz nada se já estiver no overworld.
 */
public class ItemReturnHome extends Item {

    public ItemReturnHome() {
        setMaxStackSize(1);
        setUnlocalizedName("returnHomeCrystal");
        setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    public void registerIcons(IIconRegister iconRegister) {
        this.itemIcon = iconRegister.registerIcon("voiddim:return_home");
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            if (player.dimension != 0) {
                TeleportHelper.sendToOverworld((EntityPlayerMP) player);
                stack.stackSize--;
            } else {
                player.addChatMessage(new ChatComponentText("Você já está no mundo normal."));
            }
        }
        return stack;
    }
}
