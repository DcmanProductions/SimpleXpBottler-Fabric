package com.drewchaseproject.mc.fabric.xp_bottler;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.CauldronBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.GiveCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public abstract class XPBottler implements ModInitializer
{
	public static void EntryPoint ()
	{
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
		{
			dispatcher.register(CommandManager.literal("bottler").executes(sender ->
			{
				long start = System.currentTimeMillis();
				Bottle(sender.getSource().getPlayer(), true);
				long end = System.currentTimeMillis() - start;
				
				sender.getSource().getPlayer().sendMessage(Text.of(String.format("Process took %sms", end)), false);
				return 1;
			}));
		});
		UseItemCallback.EVENT.register((player, world, hand) ->
		{
			if (player.getMainHandStack().isItemEqual(Items.GLASS_BOTTLE.getDefaultStack()) && player.isSneaking() && player.totalExperience >= 6)
			{
				Bottle(player, false);
				return TypedActionResult.success(player.getMainHandStack());
			}
			return TypedActionResult.pass(player.getMainHandStack());
		});
	}
	
	public static void Bottle (PlayerEntity player, boolean all)
	{
		PlayerInventory inventory = player.getInventory();
		if (player.totalExperience <= 6)
		{
			player.sendMessage(Text.Serializer.fromJson("{\"text\": \"You do NOT have enough XP points\"}"), false);
		}
		else if (inventory.getSlotWithStack(Items.GLASS_BOTTLE.getDefaultStack()) == -1)
		{
			player.sendMessage(Text.Serializer.fromJson("{\"text\": \"You do NOT have any Glass Bottles in your Inventory\"}"), false);
		}
		else
		{
			int slot = inventory.getSlotWithStack(Items.GLASS_BOTTLE.getDefaultStack());
			int bottles = 0;
			if (all)
			{
				while (slot != -1 && player.totalExperience >= 6)
				{
					bottles++;
					inventory.getStack(slot).decrement(1);
					player.addExperience(-6);
					slot = inventory.getSlotWithStack(Items.GLASS_BOTTLE.getDefaultStack());
				}
			}
			else
			{
				bottles = 1;
				player.getMainHandStack().decrement(1);
				player.addExperience(-6);
			}
			
			ItemEntity droppedItem = new ItemEntity(player.world, player.getX(), player.getY() + 1, player.getZ(), new ItemStack(Items.EXPERIENCE_BOTTLE, bottles));
			droppedItem.setPickupDelay(1);
			player.world.spawnEntity(droppedItem);
		}
	}
}
