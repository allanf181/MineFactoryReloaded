package powercrystals.minefactoryreloaded.item.base;

import java.util.Arrays;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemMulti extends ItemFactory {

	protected TIntObjectHashMap<String> _names = new TIntObjectHashMap<String>(16);
	protected TIntArrayList _indicies = new TIntArrayList(16);

	public ItemMulti() {

		setHasSubtypes(true);
		setMaxDamage(0);
	}

	protected void setNames(String... names) {

		setNames(0, names);
	}

	protected void setNames(int baseIndex, String... names) {

		for (int i = 0; i < names.length; ++i) {
			_names.put(baseIndex + i, names[i]);
			_indicies.add(baseIndex + i);
		}
	}

	public int[] getMetadataValues() {

		return _indicies.toArray();
	}

	public String getName(int meta) {

		return _names.get(meta);
	}

	public void getSubItems(Item item, List<ItemStack> subTypes) {

		for (int i = 0, e = _indicies.size(); i < e; ++i) {
			subTypes.add(new ItemStack(item, 1, _indicies.get(i)));
		}
	}
	
	@Override
	public int getMetadata(int meta) {

		return meta;
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {

		return getName(getUnlocalizedName(), _names.get(stack.getItemDamage()));
	}

	public static String getName(String name, String postfix) {

		return name + (postfix != null ? "." + postfix : "");
	}

	@Override
	public String toString() {

		StringBuilder b = new StringBuilder(getClass().getName());
		b.append('@').append(System.identityHashCode(this)).append('{');
		b.append("l:").append(getUnlocalizedName()).append(", n:");
		b.append(_names).append('}');
		return b.toString();
	}

}
