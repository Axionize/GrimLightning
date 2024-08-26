package ac.grim.grimac.utils.collisions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicFence;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicWall;
import ac.grim.grimac.utils.collisions.datatypes.*;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.enums.*;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.*;


// Expansion to the CollisionData class, which is different than regular ray tracing hitboxes
public enum HitboxData {
    SCAFFOLDING((player, item, version, data, x, y, z) -> {
        // If is holding scaffolding
        if (item == StateTypes.SCAFFOLDING) {
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
        }

        // STABLE_SHAPE for the scaffolding
        ComplexCollisionBox box = new ComplexCollisionBox(
                new HexCollisionBox(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                new HexCollisionBox(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 2.0D),
                new HexCollisionBox(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D),
                new HexCollisionBox(0.0D, 0.0D, 14.0D, 2.0D, 16.0D, 16.0D),
                new HexCollisionBox(14.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D));

        if (data.getHalf() == Half.LOWER) { // Add the unstable shape to the collision boxes
            box.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 2.0D, 2.0D, 16.0D));
            box.add(new HexCollisionBox(14.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D));
            box.add(new HexCollisionBox(0.0D, 0.0D, 14.0D, 16.0D, 2.0D, 16.0D));
            box.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 2.0D));
        }

        return box;
    }, StateTypes.SCAFFOLDING),

    DRIPLEAF((player, item, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_16_4))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        ComplexCollisionBox box = new ComplexCollisionBox();

        if (data.getFacing() == BlockFace.NORTH) { // Stem
            box.add(new HexCollisionBox(5.0D, 0.0D, 9.0D, 11.0D, 15.0D, 15.0D));
        } else if (data.getFacing() == BlockFace.SOUTH) {
            box.add(new HexCollisionBox(5.0D, 0.0D, 1.0D, 11.0D, 15.0D, 7.0D));
        } else if (data.getFacing() == BlockFace.EAST) {
            box.add(new HexCollisionBox(1.0D, 0.0D, 5.0D, 7.0D, 15.0D, 11.0D));
        } else {
            box.add(new HexCollisionBox(9.0D, 0.0D, 5.0D, 15.0D, 15.0D, 11.0D));
        }

        if (data.getTilt() == Tilt.NONE || data.getTilt() == Tilt.UNSTABLE) {
            box.add(new HexCollisionBox(0.0, 11.0, 0.0, 16.0, 15.0, 16.0));
        } else if (data.getTilt() == Tilt.PARTIAL) {
            box.add(new HexCollisionBox(0.0, 11.0, 0.0, 16.0, 13.0, 16.0));
        }

        return box;

    }, StateTypes.BIG_DRIPLEAF),

    FENCE_GATE((player, item, version, data, x, y, z) -> {
        // This technically should be taken from the block data/made multi-version/run block updates... but that's too far even for me
        // This way is so much easier and works unless the magic stick wand is used
        boolean isInWall;
        boolean isXAxis = data.getFacing() == BlockFace.WEST || data.getFacing() == BlockFace.EAST;
        if (isXAxis) {
            boolean zPosWall = Materials.isWall(player.compensatedWorld.getStateTypeAt(x, y, z + 1));
            boolean zNegWall = Materials.isWall(player.compensatedWorld.getStateTypeAt(x, y, z - 1));
            isInWall = zPosWall || zNegWall;
        } else {
            boolean xPosWall = Materials.isWall(player.compensatedWorld.getStateTypeAt(x + 1, y, z));
            boolean xNegWall = Materials.isWall(player.compensatedWorld.getStateTypeAt(x - 1, y, z));
            isInWall = xPosWall || xNegWall;
        }

        if (isInWall) {
            return isXAxis ? new HexCollisionBox(6.0D, 0.0D, 0.0D, 10.0D, 13.0D, 16.0D) : new HexCollisionBox(0.0D, 0.0D, 6.0D, 16.0D, 13.0D, 10.0D);
        }

        return isXAxis ? new HexCollisionBox(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D) : new HexCollisionBox(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    }, BlockTags.FENCE_GATES.getStates().toArray(new StateType[0])),


    FENCE((player, item, version, data, x, y, z) -> {
        WrappedBlockState state = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);

        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            int i = 0;
            if (data.getSouth() == South.TRUE) {
                i |= 0b1;
            }
            if (data.getWest() == West.TRUE) {
                i |= 0b10;
            }
            if (data.getNorth() == North.TRUE) {
                i |= 0b100;
            }
            if (data.getEast() == East.TRUE) {
                i |= 0b1000;
            }

            return DynamicFence.LEGACY_BOUNDING_BOXES[i].copy();
        }

        List<SimpleCollisionBox> boxes = new ArrayList<>();
        CollisionData.getData(state.getType()).getMovementCollisionBox(player, version, state, x, y, z).downCast(boxes);

        for (SimpleCollisionBox box : boxes) {
            box.maxY = 1;
        }

        return new ComplexCollisionBox(boxes.toArray(new SimpleCollisionBox[0]));
    }, BlockTags.FENCES.getStates().toArray(new StateType[0])),

    WALL((player, item, version, data, x, y, z) -> {
        WrappedBlockState state = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
        return new DynamicWall().fetchRegularBox(player, state, version, x, y, z);
    }, BlockTags.WALLS.getStates().toArray(new StateType[0])),

    HONEY_BLOCK(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true), StateTypes.HONEY_BLOCK),

    POWDER_SNOW(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true), StateTypes.POWDER_SNOW),

    SOUL_SAND(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true), StateTypes.SOUL_SAND),

    CACTUS(new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D), StateTypes.CACTUS),

    SNOW((player, item, version, data, x, y, z) -> {
        return new SimpleCollisionBox(0, 0, 0, 1, data.getLayers() * 0.125, 1);
    }, StateTypes.SNOW),

    LECTERN_BLOCK((player, item, version, data, x, y, z) -> {
        ComplexCollisionBox common = new ComplexCollisionBox(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
                new HexCollisionBox(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D));

        if (data.getFacing() == BlockFace.WEST) {
            common.add(new HexCollisionBox(1.0D, 10.0D, 0.0D, 5.333333D, 14.0D, 16.0D));
            common.add(new HexCollisionBox(5.333333D, 12.0D, 0.0D, 9.666667D, 16.0D, 16.0D));
            common.add(new HexCollisionBox(9.666667D, 14.0D, 0.0D, 14.0D, 18.0D, 16.0D));
        } else if (data.getFacing() == BlockFace.NORTH) {
            common.add(new HexCollisionBox(0.0D, 10.0D, 1.0D, 16.0D, 14.0D, 5.333333D));
            common.add(new HexCollisionBox(0.0D, 12.0D, 5.333333D, 16.0D, 16.0D, 9.666667D));
            common.add(new HexCollisionBox(0.0D, 14.0D, 9.666667D, 16.0D, 18.0D, 14.0D));
        } else if (data.getFacing() == BlockFace.EAST) {
            common.add(new HexCollisionBox(10.666667D, 10.0D, 0.0D, 15.0D, 14.0D, 16.0D));
            common.add(new HexCollisionBox(6.333333D, 12.0D, 0.0D, 10.666667D, 16.0D, 16.0D));
            common.add(new HexCollisionBox(2.0D, 14.0D, 0.0D, 6.333333D, 18.0D, 16.0D));
        } else { // SOUTH
            common.add(new HexCollisionBox(0.0D, 10.0D, 10.666667D, 16.0D, 14.0D, 15.0D));
            common.add(new HexCollisionBox(0.0D, 12.0D, 6.333333D, 16.0D, 16.0D, 10.666667D));
            common.add(new HexCollisionBox(0.0D, 14.0D, 2.0D, 16.0D, 18.0D, 6.333333D));
        }

        return common;
    }, StateTypes.LECTERN),

    WALL_HANGING_SIGNS((player, item, version, data, x, y, z) -> {
        switch (data.getFacing()) {
            case NORTH:
            case SOUTH:
                return new ComplexCollisionBox(new HexCollisionBox(0.0D, 14.0D, 6.0D, 16.0D, 16.0D, 10.0D),
                        new HexCollisionBox(1.0D, 0.0D, 7.0D, 15.0D, 10.0D, 9.0D));
            case WEST:
            case EAST:
                return new ComplexCollisionBox(new HexCollisionBox(6.0D, 14.0D, 0.0D, 10.0D, 16.0D, 16.0D),
                        new HexCollisionBox(7.0D, 0.0D, 1.0D, 9.0D, 10.0D, 15.0D));
            default:
                return NoCollisionBox.INSTANCE;
        }
    }, BlockTags.WALL_HANGING_SIGNS.getStates().toArray(new StateType[0])),

    PITCHER_CROP((player, item, version, data, x, y, z) -> {
        final SimpleCollisionBox FULL_UPPER_SHAPE = new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 15.0D, 13.0D);
        final SimpleCollisionBox FULL_LOWER_SHAPE = new HexCollisionBox(3.0D, -1.0D, 3.0D, 13.0D, 16.0D, 13.0D);
        final SimpleCollisionBox COLLISION_SHAPE_BULB = new HexCollisionBox(5.0D, -1.0D, 5.0D, 11.0D, 3.0D, 11.0D);
        final SimpleCollisionBox COLLISION_SHAPE_CROP = new HexCollisionBox(3.0D, -1.0D, 3.0D, 13.0D, 5.0D, 13.0D);
        final SimpleCollisionBox[] UPPER_SHAPE_BY_AGE = new SimpleCollisionBox[]{new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 11.0D, 13.0D), FULL_UPPER_SHAPE};
        final SimpleCollisionBox[] LOWER_SHAPE_BY_AGE = new SimpleCollisionBox[]{COLLISION_SHAPE_BULB, new HexCollisionBox(3.0D, -1.0D, 3.0D, 13.0D, 14.0D, 13.0D), FULL_LOWER_SHAPE, FULL_LOWER_SHAPE, FULL_LOWER_SHAPE};

        return data.getHalf() == Half.UPPER ? UPPER_SHAPE_BY_AGE[Math.min(Math.abs(4 - (data.getAge() + 1)), UPPER_SHAPE_BY_AGE.length - 1)] : LOWER_SHAPE_BY_AGE[data.getAge()];
    }, StateTypes.PITCHER_CROP),

    WHEAT_BEETROOTS((player, item, version, data, x, y, z) -> {
        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, (data.getAge() + 1) * 2, 16.0D);
    }, StateTypes.WHEAT, StateTypes.BEETROOTS),

    CARROT_POTATOES((player, item, version, data, x, y, z) -> {
        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, data.getAge() + 2, 16.0D);
    }, StateTypes.CARROTS, StateTypes.POTATOES),

    NETHER_WART((player, item, version, data, x, y, z) -> {
        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0, 5 + (data.getAge() * 3), 16.0D);
    }, StateTypes.NETHER_WART),

    ATTACHED_PUMPKIN_STEM((player, item, version, data, x, y, z) -> {
        if (version.isOlderThan(ClientVersion.V_1_13))
            return new HexCollisionBox(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);

        switch (data.getFacing()) {
            case SOUTH:
                return new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 10.0D, 16.0D);
            case WEST:
                return new HexCollisionBox(0.0D, 0.0D, 6.0D, 10.0D, 10.0D, 10.0D);
            case NORTH:
                return new HexCollisionBox(6.0D, 0.0D, 0.0D, 10.0D, 10.0D, 10.0D);
            case EAST:
            default:
                return new HexCollisionBox(6.0D, 0.0D, 6.0D, 16.0D, 10.0D, 10.0D);
        }
    }, StateTypes.ATTACHED_MELON_STEM, StateTypes.ATTACHED_PUMPKIN_STEM),

    PUMPKIN_STEM((player, item, version, data, x, y, z) -> {
        return new HexCollisionBox(7, 0, 7, 9, 2 * (data.getAge() + 1), 9);
    }, StateTypes.PUMPKIN_STEM, StateTypes.MELON_STEM),

    // Hitbox/Outline is Same as Collision
    COCOA_BEANS((player, item, version, data, x, y, z) -> {
        return CollisionData.getCocoa(version, data.getAge(), data.getFacing());
    }, StateTypes.COCOA),

    BUTTON((player, item, version, data, x, y, z) -> {
        final BlockFace facing = data.getFacing();
        final boolean powered = data.isPowered();
        switch (data.getFace()) {
            case FLOOR:
                // x axis
                if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
                    return powered ? new HexCollisionBox(6.0, 0.0, 5.0, 10.0, 1.0, 11.0) : new HexCollisionBox(6.0, 0.0, 5.0, 10.0, 2.0, 11.0);
                }

                return powered ? new HexCollisionBox(5.0, 0.0, 6.0, 11.0, 1.0, 10.0) : new HexCollisionBox(5.0, 0.0, 6.0, 11.0, 2.0, 10.0);
            case WALL:
                CollisionBox shape;
                switch (facing) {
                    case EAST:
                        shape = powered ? new HexCollisionBox(0.0, 6.0, 5.0, 1.0, 10.0, 11.0) : new HexCollisionBox(0.0, 6.0, 5.0, 2.0, 10.0, 11.0);
                        break;
                    case WEST:
                        shape = powered ? new HexCollisionBox(15.0, 6.0, 5.0, 16.0, 10.0, 11.0) : new HexCollisionBox(14.0, 6.0, 5.0, 16.0, 10.0, 11.0);
                        break;
                    case SOUTH:
                        shape = powered ? new HexCollisionBox(5.0, 6.0, 0.0, 11.0, 10.0, 1.0) : new HexCollisionBox(5.0, 6.0, 0.0, 11.0, 10.0, 2.0);
                        break;
                    case NORTH:
                    case UP:
                    case DOWN:
                        shape = powered ? new HexCollisionBox(5.0, 6.0, 15.0, 11.0, 10.0, 16.0) : new HexCollisionBox(5.0, 6.0, 14.0, 11.0, 10.0, 16.0);
                        break;
                    default:
                        shape = NoCollisionBox.INSTANCE;
                }

                return shape;
            case CEILING:
            default:
                // x axis
                if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
                    return powered ? new HexCollisionBox(6.0, 15.0, 5.0, 10.0, 16.0, 11.0) : new HexCollisionBox(6.0, 14.0, 5.0, 10.0, 16.0, 11.0);
                } else {
                    return powered ? new HexCollisionBox(5.0, 15.0, 6.0, 11.0, 16.0, 10.0) : new HexCollisionBox(5.0, 14.0, 6.0, 11.0, 16.0, 10.0);
                }
        }
    }, BlockTags.BUTTONS.getStates().toArray(new StateType[0])),

    WALL_SIGN((player, item, version, data, x, y, z) -> {
        switch (data.getFacing()) {
            case NORTH:
                return new HexCollisionBox(0.0, 4.5, 14.0, 16.0, 12.5, 16.0);
            case SOUTH:
                return new HexCollisionBox(0.0, 4.5, 0.0, 16.0, 12.5, 2.0);
            case EAST:
                return new HexCollisionBox(0.0, 4.5, 0.0, 2.0, 12.5, 16.0);
            case WEST:
                return new HexCollisionBox(14.0, 4.5, 0.0, 16.0, 12.5, 16.0);
            default:
                return NoCollisionBox.INSTANCE;
        }
    }, BlockTags.WALL_SIGNS.getStates().toArray(new StateType[0])),

    WALL_HANGING_SIGN((player, item, version, data, x, y, z) -> {
        switch (data.getFacing()) {
            case NORTH:
            case SOUTH:
                return new ComplexCollisionBox(new HexCollisionBox(0.0D, 14.0D, 6.0D, 16.0D, 16.0D, 10.0D),
                        new HexCollisionBox(1.0D, 0.0D, 7.0D, 15.0D, 10.0D, 9.0D));
            default:
                return new ComplexCollisionBox(new HexCollisionBox(6.0D, 14.0D, 0.0D, 10.0D, 16.0D, 16.0D),
                        new HexCollisionBox(7.0D, 0.0D, 1.0D, 9.0D, 10.0D, 15.0D));
        }
    }, BlockTags.WALL_HANGING_SIGNS.getStates().toArray(new StateType[0])),

    STANDING_SIGN((player, item, version, data, x, y, z) ->
            new HexCollisionBox(4.0, 0.0, 4.0, 12.0, 16.0, 12.0),
            BlockTags.STANDING_SIGNS.getStates().toArray(new StateType[0])),

    REDSTONE_WIRE((player, item, version, data, x, y, z) ->
            // Easier to just use no collision box
            // Redstone wire is very complex with its collision shapes and has many de-syncs
            NoCollisionBox.INSTANCE,
            StateTypes.REDSTONE_WIRE),

    FIRE((player, item, version, data, x, y, z) -> {
        // Since 1.16 fire has a small hitbox
        if (version.isNewerThanOrEquals(ClientVersion.V_1_16)) {
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
        }

        return NoCollisionBox.INSTANCE;
    }, BlockTags.FIRE.getStates().toArray(new StateType[0])),

    LEVER(((player, item, version, data, x, y, z) -> {
        SimpleCollisionBox NORTH_WALL_SHAPE = new SimpleCollisionBox(0.3125, 0.25, 0.625, 0.6875, 0.75, 1.0, false);
        SimpleCollisionBox SOUTH_WALL_SHAPE = new SimpleCollisionBox(0.3125, 0.25, 0.0, 0.6875, 0.75, 6.0, false);
        SimpleCollisionBox WEST_WALL_SHAPE = new SimpleCollisionBox(0.625, 0.25, 0.3125, 1.0, 0.75, 0.6875, false);
        SimpleCollisionBox EAST_WALL_SHAPE = new SimpleCollisionBox(0.0, 0.25, 0.3125, 0.375, 0.75, 0.6875, false);
        SimpleCollisionBox FLOOR_Z_AXIS_SHAPE = new SimpleCollisionBox(0.3125, 0.0, 0.25, 0.6875, 0.375, 0.75, false);
        SimpleCollisionBox FLOOR_X_AXIS_SHAPE = new SimpleCollisionBox(0.25, 0.0, 0.3125, 0.75, 0.375, 0.6875, false);
        SimpleCollisionBox CEILING_Z_AXIS_SHAPE = new SimpleCollisionBox(0.3125, 0.625, 0.25, 0.6875, 1.0, 0.75, false);
        SimpleCollisionBox CEILING_X_AXIS_SHAPE = new SimpleCollisionBox(0.25, 0.625, 0.3125, 0.75, 1.0, 0.6875, false);

        BlockFace blockFace = data.getFacing();
        BlockFace facing = data.getFacing();
        switch (data.getFace()) {
            case FLOOR:
                if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
                        return FLOOR_X_AXIS_SHAPE;
                }
                return FLOOR_Z_AXIS_SHAPE;
            case WALL:
                switch (blockFace) {
                    case EAST:
                        return EAST_WALL_SHAPE;
                    case WEST:
                        return WEST_WALL_SHAPE;
                    case SOUTH:
                        return SOUTH_WALL_SHAPE;
                    case NORTH:
                    default:
                        return NORTH_WALL_SHAPE;
                }
            case CEILING:
            default:
                if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
                    return CEILING_X_AXIS_SHAPE;
                }
                return CEILING_Z_AXIS_SHAPE;
        }
    }), StateTypes.LEVER),

    BANNER(((player, item, version, data, x, y, z) -> new HexCollisionBox(4.0, 0.0, 4.0, 12.0, 16.0, 12.0)));


    private static final Map<StateType, HitboxData> lookup = new HashMap<>();

    static {
        for (HitboxData data : HitboxData.values()) {
            for (StateType type : data.materials) {
                lookup.put(type, data);
            }
        }
    }

    private final StateType[] materials;
    private CollisionBox box;
    private HitBoxFactory dynamic;



    HitboxData(CollisionBox box, StateType... materials) {
        this.box = box;
        Set<StateType> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new StateType[0]);
    }

    HitboxData(HitBoxFactory dynamic, StateType... materials) {
        this.dynamic = dynamic;
        Set<StateType> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new StateType[0]);
    }

    public static HitboxData getData(StateType material) {
        return lookup.get(material);
    }

    public static CollisionBox getBlockHitbox(GrimPlayer player, StateType heldItem, ClientVersion version, WrappedBlockState block, int x, int y, int z) {
        HitboxData data = getData(block.getType());

        if (data == null) {
            // Fall back to collision boxes
            return CollisionData.getRawData(block.getType()).getMovementCollisionBox(player, version, block, x, y, z);
        }

        // Simple collision box to override
        if (data.box != null)
            return data.box.copy().offset(x, y, z);

        // Allow this class to override collision boxes when they aren't the same as regular boxes
        HitBoxFactory hitBoxFactory = data.dynamic;
        CollisionBox collisionBox = hitBoxFactory.fetch(player, heldItem, version, block, x, y, z);
        collisionBox.offset(x, y, z);
        return collisionBox;
    }
}
