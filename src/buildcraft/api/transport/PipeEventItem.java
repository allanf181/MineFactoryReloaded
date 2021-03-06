package buildcraft.api.transport;

import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import buildcraft.api.transport.neptune.IFlowItems;
import buildcraft.api.transport.neptune.IPipeHolder;

public abstract class PipeEventItem extends PipeEvent {

    public final IFlowItems flow;

    protected PipeEventItem(IPipeHolder holder, IFlowItems flow) {
        super(holder);
        this.flow = flow;
    }

    protected PipeEventItem(boolean canBeCancelled, IPipeHolder holder, IFlowItems flow) {
        super(canBeCancelled, holder);
        this.flow = flow;
    }

    // ################
    //
    // Misc events
    //
    // ################

    /** Fires whenever item insertion is attempted. The item might have been extracted by the pipe behaviour, inserted
     * by another pipe or even a different kind of tile. Note that you have no way of telling what caused this event. */
    public static class TryInsert extends PipeEventItem {
        public final EnumDyeColor colour;
        public final EnumFacing from;
        /** The itemstack that is attempting to be inserted. NEVER CHANGE THIS! */
        public final ItemStack attempting;
        /** The count of items that are being accepted into this pipe. Starts off at the stack count of
         * {@link #attempting} */
        public int accepted;

        public TryInsert(IPipeHolder holder, IFlowItems flow, EnumDyeColor colour, EnumFacing from, ItemStack attempting) {
            super(true, holder, flow);
            this.colour = colour;
            this.from = from;
            this.attempting = attempting;
            this.accepted = attempting.stackSize;
        }
    }

    /** Fired whenever an item reaches the centre of a pipe. Note that you *can* change the itemstack or the colour. */
    public static class ReachCenter extends PipeEventItem {
        public final ItemStack stack;
        public final EnumFacing from;
        public EnumDyeColor colour;

        public ReachCenter(IPipeHolder holder, IFlowItems flow, ItemStack stack, EnumFacing from, EnumDyeColor colour) {
            super(holder, flow);
            this.stack = stack;
            this.from = from;
            this.colour = colour;
        }
    }

    /** Fired whenever an item reaches the end of a pipe. Note that you *can* change the itemstack or the colour. */
    public static class ReachEnd extends PipeEventItem {
        public final ItemStack stack;
        public final EnumFacing from, to;
        public EnumDyeColor colour;

        public ReachEnd(IPipeHolder holder, IFlowItems flow, ItemStack stack, EnumFacing from, EnumFacing to, EnumDyeColor colour) {
            super(holder, flow);
            this.stack = stack;
            this.from = from;
            this.to = to;
            this.colour = colour;
        }
    }

    // ############################
    //
    // Destination related
    //
    // ############################

    /** Fired after {@link TryInsert} (if some items were allowed in) to determine what sides are the items NOT allowed
     * to go to, and the order of precedence for the allowed sides. */
    public static class SideCheck extends PipeEventItem {
        public final EnumDyeColor colour;
        public final EnumFacing from;
        public final ItemStack stack;

        private final int[] precedence = new int[6];
        private final EnumSet<EnumFacing> allowed = EnumSet.allOf(EnumFacing.class);

        public SideCheck(IPipeHolder holder, IFlowItems flow, EnumDyeColor colour, EnumFacing from, ItemStack stack) {
            super(holder, flow);
            this.colour = colour;
            this.from = from;
            this.stack = stack;
        }

        /** Checks to see if a side if allowed. Note that this may return true even though a later handler might
         * disallow a side, so you should only use this to skip checking a side (for example a diamond pipe might not
         * check the filters for a specific side if its already been disallowed) */
        public boolean isAllowed(EnumFacing side) {
            return allowed.contains(side);
        }

        public void disallow(EnumFacing... sides) {
            for (EnumFacing side : sides) {
                allowed.remove(side);
            }
        }

        public void disallowAll(Collection<EnumFacing> sides) {
            allowed.removeAll(sides);
        }

        public void disallowAllExcept(EnumFacing... sides) {
            allowed.retainAll(Lists.newArrayList(sides));
        }

        public void disallowAll() {
            allowed.clear();
        }

        public void increasePrecedence(EnumFacing side) {
            increasePrecedence(side, 1);
        }

        public void increasePrecedence(EnumFacing side, int by) {
            precedence[side.ordinal()] -= by;
        }

        public void decreasePrecedence(EnumFacing side) {
            decreasePrecedence(side, 1);
        }

        public void decreasePrecedence(EnumFacing side, int by) {
            increasePrecedence(side, -by);
        }

        public List<EnumSet<EnumFacing>> getOrder() {
            if (allowed.isEmpty()) {
                return ImmutableList.of();
            }
            if (allowed.size() == 1) {
                return ImmutableList.of(allowed);
            }
            outer_loop: while (true) {
                int val = precedence[0];
                for (int i = 1; i < precedence.length; i++) {
                    if (precedence[i] != val) {
                        break outer_loop;
                    }
                }
                // No need to work out the order when all destinations have the same precedence
                return ImmutableList.of(allowed);
            }

            int[] ordered = Arrays.copyOf(precedence, 6);
            Arrays.sort(ordered);
            int last = 0;
            List<EnumSet<EnumFacing>> list = Lists.newArrayList();
            for (int i = 0; i < 6; i++) {
                int current = ordered[i];
                if (i != 0 && current == last) {
                    continue;
                }
                last = current;
                EnumSet<EnumFacing> set = EnumSet.noneOf(EnumFacing.class);
                for (EnumFacing face : EnumFacing.VALUES) {
                    if (allowed.contains(face)) {
                        if (precedence[face.ordinal()] == current) {
                            set.add(face);
                        }
                    }
                }
                if (set.size() > 0) {
                    list.add(set);
                }
            }
            return list;
        }
    }

    /** Fired after {@link SideCheck} (if all sides were disallowed) to see if the item is allowed to bounce back to
     * where it was inserted. */
    public static class TryBounce extends PipeEventItem {
        public final EnumDyeColor colour;
        public final EnumFacing from;
        public final ItemStack stack;
        public boolean canBounce = false;

        public TryBounce(IPipeHolder holder, IFlowItems flow, EnumDyeColor colour, EnumFacing from, ItemStack stack) {
            super(holder, flow);
            this.colour = colour;
            this.from = from;
            this.stack = stack;
        }
    }

    /** Base class for {@link Split} and {@link FindDest}. Do not listen to this directly! */
    public static abstract class OrderedEvent extends PipeEventItem {
        public final List<EnumSet<EnumFacing>> orderedDestinations;

        public OrderedEvent(IPipeHolder holder, IFlowItems flow, List<EnumSet<EnumFacing>> orderedDestinations) {
            super(holder, flow);
            this.orderedDestinations = orderedDestinations;
        }

        public ImmutableList<EnumFacing> generateRandomOrder() {
            ImmutableList.Builder<EnumFacing> builder = ImmutableList.builder();
            for (EnumSet<EnumFacing> set : orderedDestinations) {
                List<EnumFacing> faces = new ArrayList<>(set);
                Collections.shuffle(faces);
                builder.addAll(faces);
            }
            return builder.build();
        }
    }

    /** Fired after {@link SideCheck} (if at least one valid side was found) or after {@link TryBounce} if no valid
     * sides were detected, but it was allowed to bounce back. This event is for splitting up (or modifying) the input
     * itemstack. This is most helpful for implementing full round-robin behaviour, or diamond-pipe based splitting. If
     * you need to generate a random facing for each one then use {@link OrderedEvent#generateRandomOrder()}. */
    public static class Split extends OrderedEvent {
        public final List<ItemEntry> items = new ArrayList<>();

        public Split(IPipeHolder holder, IFlowItems flow, List<EnumSet<EnumFacing>> order, ItemEntry toSplit) {
            super(holder, flow, order);
            items.add(toSplit);
        }
    }

    /** Fired after {@link Split}. This event is for assigning a destination to each {@link ItemEntry} in
     * {@link Split#items}. If you need to generate a random facing for each one then use
     * {@link OrderedEvent#generateRandomOrder()}. */
    public static class FindDest extends OrderedEvent {
        public final ImmutableList<ItemEntry> items;

        public FindDest(IPipeHolder holder, IFlowItems flow, List<EnumSet<EnumFacing>> orderedDestinations, ImmutableList<ItemEntry> items) {
            super(holder, flow, orderedDestinations);
            this.items = items;
        }
    }

    /** Fired after {@link FindDest}. */
    public static class ModifySpeed extends PipeEventItem {
        public final ItemEntry item;
        public final double currentSpeed;
        public double targetSpeed = 0;
        public double maxSpeedChange = 0;

        public ModifySpeed(IPipeHolder holder, IFlowItems flow, ItemEntry item, double initSpeed) {
            super(holder, flow);
            this.item = item;
            currentSpeed = initSpeed;
        }

        public void modifyTo(double target, double maxDelta) {
            targetSpeed = target;
            maxSpeedChange = maxDelta;
        }
    }

    /** Mostly immutable holding class for item stacks. */
    public static class ItemEntry {
        public final EnumDyeColor colour;
        public final ItemStack stack;
        public final EnumFacing from;
        /** An list of the destinations to try, in order. */
        public List<EnumFacing> to;

        public ItemEntry(EnumDyeColor colour, ItemStack stack, EnumFacing from) {
            this.colour = colour;
            this.stack = stack;
            this.from = from;
        }
    }
}
