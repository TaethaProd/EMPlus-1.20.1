package taethaprod.emplus.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import taethaprod.emplus.ServerTaskScheduler;
import taethaprod.emplus.SummonedBossBarManager;
import taethaprod.emplus.ModItems;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class MythicalKeyItem extends Item {
	private final int level;

	public MythicalKeyItem(int level, Settings settings) {
		super(settings);
		this.level = level;
	}

	public int getLevel() {
		return level;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) {
			return TypedActionResult.success(stack);
		}

		ServerWorld serverWorld = (ServerWorld) world;
		EntityType<? extends MobEntity> type = getMobType(stack).orElseGet(() -> ModItems.getRandomMobType(world.getRandom()));
		Vec3d spawnPos = chooseSpawnPos(serverWorld, user);
		if (spawnPos == null) {
			return TypedActionResult.pass(stack);
		}

		// Delay lightning + mob by 2 seconds (40 ticks) for effect.
		ServerTaskScheduler.schedule(serverWorld, 40, w -> spawnLightningAndMob(w, spawnPos, type));

		if (!user.getAbilities().creativeMode) {
			stack.decrement(1);
		}
		user.getItemCooldownManager().set(this, 10);

		return TypedActionResult.success(stack, world.isClient);
	}

	private void spawnLightningAndMob(ServerWorld world, Vec3d spawnPos, EntityType<? extends MobEntity> type) {
		var lightning = net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(world);
		if (lightning != null) {
			lightning.refreshPositionAfterTeleport(spawnPos.x, spawnPos.y, spawnPos.z);
			lightning.setCosmetic(true);
			world.spawnEntity(lightning);
		}

		MobEntity mob = createBuffedMob(world, type);
		if (mob == null) {
			return;
		}
		mob.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, world.getRandom().nextFloat() * 360.0F, 0.0F);
		world.spawnEntity(mob);
		SummonedBossBarManager.track(mob, level);
	}

	private Vec3d chooseSpawnPos(ServerWorld world, PlayerEntity user) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i < 8; i++) {
			double radius = 3.0 + random.nextDouble(3.0); // 3-6 blocks away
			double angle = random.nextDouble(0, Math.PI * 2);
			int dx = (int) Math.round(Math.cos(angle) * radius);
			int dz = (int) Math.round(Math.sin(angle) * radius);
			BlockPos base = user.getBlockPos().add(dx, 0, dz);
			BlockPos top = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, base).up();
			if (world.isAir(top) && world.isAir(top.up())) {
				return Vec3d.ofCenter(top);
			}
		}
		return null;
	}

	private MobEntity createBuffedMob(ServerWorld world, EntityType<? extends MobEntity> type) {
		MobEntity mob = type.create(world);
		if (mob == null) {
			return null;
		}

		applyAttributeMultiplier(mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH), 1.0 + level * 0.4);
		applyAttributeMultiplier(mob.getAttributeInstance(EntityAttributes.GENERIC_ARMOR), 1.0 + level * 0.25);
		applyAttributeMultiplier(mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE), 1.0 + level * 0.35);
		applyAttributeMultiplier(mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED), movementSpeedMultiplier(level));

		mob.setHealth(mob.getMaxHealth());
		mob.setPersistent();

		int nextLevel = level + 1;
		if (nextLevel <= 10) {
			mob.addCommandTag(nextLevelTag(nextLevel));
		}

		return mob;
	}

	public static String nextLevelTag(int level) {
		return "emplus:mythical_key_next_level=" + level;
	}

	private void applyAttributeMultiplier(EntityAttributeInstance instance, double multiplier) {
		if (instance == null) {
			return;
		}
		double newBase = Math.max(1.0D, instance.getBaseValue() * multiplier);
		instance.setBaseValue(newBase);
	}

	private double movementSpeedMultiplier(int level) {
		// Level 1 = 1x, Level 10 ≈ 2x, linear scale.
		int clampedLevel = Math.max(1, Math.min(10, level));
		double step = 1.0 / 9.0;
		return 1.0 + (clampedLevel - 1) * step;
	}

	@Override
	public String getTranslationKey() {
		return "item.emplus.mythical_key_" + level;
	}

	@Override
	public net.minecraft.text.Text getName(ItemStack stack) {
		var base = super.getName(stack);
		var mobName = getMobType(stack)
				.map(type -> (net.minecraft.text.Text) type.getName())
				.orElse(net.minecraft.text.Text.literal("Unknown"));
		return net.minecraft.text.Text.translatable("item.emplus.mythical_key.with_mob", base, mobName);
	}

	public Optional<EntityType<? extends MobEntity>> getMobType(ItemStack stack) {
		if (!stack.hasNbt() || !stack.getNbt().contains("Mob")) {
			return Optional.empty();
		}
		Identifier identifier = Identifier.tryParse(stack.getNbt().getString("Mob"));
		if (identifier == null) {
			return Optional.empty();
		}
		for (EntityType<? extends MobEntity> candidate : ModItems.KEY_MOBS) {
			if (net.minecraft.registry.Registries.ENTITY_TYPE.getId(candidate).equals(identifier)) {
				return Optional.of(candidate);
			}
		}
		return Optional.empty();
	}

	public void setMob(ItemStack stack, EntityType<? extends MobEntity> type) {
		Identifier id = net.minecraft.registry.Registries.ENTITY_TYPE.getId(type);
		if (id != null) {
			stack.getOrCreateNbt().putString("Mob", id.toString());
		}
	}

	@Override
	public String toString() {
		return ("MythicalKeyItem[level=%d]").formatted(level);
	}
}
