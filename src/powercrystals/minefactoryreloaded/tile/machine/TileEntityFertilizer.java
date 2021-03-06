package powercrystals.minefactoryreloaded.tile.machine;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import powercrystals.minefactoryreloaded.MFRRegistry;
import powercrystals.minefactoryreloaded.api.FertilizerType;
import powercrystals.minefactoryreloaded.api.IFactoryFertilizable;
import powercrystals.minefactoryreloaded.api.IFactoryFertilizer;
import powercrystals.minefactoryreloaded.gui.client.GuiFactoryInventory;
import powercrystals.minefactoryreloaded.gui.client.GuiUpgradeable;
import powercrystals.minefactoryreloaded.gui.container.ContainerUpgradeable;
import powercrystals.minefactoryreloaded.setup.MFRConfig;
import powercrystals.minefactoryreloaded.setup.Machine;
import powercrystals.minefactoryreloaded.tile.base.TileEntityFactoryPowered;

public class TileEntityFertilizer extends TileEntityFactoryPowered {

	private Random _rand;

	public TileEntityFertilizer() {

		super(Machine.Fertilizer);
		_rand = new Random();
		createHAM(this, 1);
		setManageSolids(true);
		setCanRotate(true);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiFactoryInventory getGui(InventoryPlayer inventoryPlayer) {

		return new GuiUpgradeable(getContainer(inventoryPlayer), this);
	}

	@Override
	public ContainerUpgradeable getContainer(InventoryPlayer inventoryPlayer) {

		return new ContainerUpgradeable(this, inventoryPlayer);
	}

	@Override
	public boolean activateMachine() {

		BlockPos bp = _areaManager.getNextBlock();
		if (!worldObj.isBlockLoaded(bp)) {
			setIdleTicks(getIdleTicksMax());
			return false;
		}

		Map<Block, IFactoryFertilizable> fertalizables = MFRRegistry.getFertilizables();

		Block target = worldObj.getBlockState(bp).getBlock();
		if (!fertalizables.containsKey(target)) {
			setIdleTicks(getIdleTicksMax());
			return false;
		}

		IFactoryFertilizable fertilizable = fertalizables.get(target);
		Map<Item, IFactoryFertilizer> fertilizers = MFRRegistry.getFertilizers();
		for (int stackIndex = 0, e = getSizeInventory(); stackIndex < e; stackIndex++) {
			ItemStack fertStack = getStackInSlot(stackIndex);
			if (fertStack == null || !fertilizers.containsKey(fertStack.getItem()))
				continue;

			IFactoryFertilizer fertilizer = fertilizers.get(fertStack.getItem());
			FertilizerType type = fertilizer.getFertilizerType(fertStack);

			if (type == FertilizerType.None)
				continue;
			if (!fertilizable.canFertilize(worldObj, bp, type))
				continue;

			if (fertilizable.fertilize(worldObj, _rand, bp, type)) {
				fertilizer.consume(fertStack);
				if (MFRConfig.playSounds.getBoolean(true)) // particles
					worldObj.playEvent(null, 2005, bp, _rand.nextInt(10) + 5);
				if (fertStack.stackSize <= 0)
					setInventorySlotContents(stackIndex, null);

				return true;
			}
		}

		setIdleTicks(getIdleTicksMax());
		return false;
	}

	@Override
	public int getSizeInventory() {

		return 10;
	}

	@Override
	public int getWorkMax() {

		return 1;
	}

	@Override
	public int getIdleTicksMax() {

		return 20;
	}

	@Override
	public int getStartInventorySide(EnumFacing side) {

		return 0;
	}

	@Override
	public int getSizeInventorySide(EnumFacing side) {

		return 9;
	}

	@Override
	public int getUpgradeSlot() {

		return 9;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, EnumFacing side) {

		if (stack != null) {
			if (slot < 9) {
				return MFRRegistry.getFertilizers().containsKey(stack.getItem());
			} else if (slot == 9) {
				return isUsableAugment(stack);
			}
		}
		return false;
	}
}
