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
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import taethaprod.emplus.ModItems;
import taethaprod.emplus.ServerTaskScheduler;
import taethaprod.emplus.SummonedBossBarManager;
import taethaprod.emplus.config.BossScalingConfig;
import taethaprod.emplus.config.BossScalingConfigManager;
import taethaprod.emplus.config.ConfigManager;
import taethaprod.emplus.config.ModConfig;
import taethaprod.emplus.origin.OriginLookup;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class MythicalKeyItem extends Item {
	private static final String LEVEL_KEY = "Level";

	public MythicalKeyItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) {
			return TypedActionResult.success(stack);
		}

		ServerWorld serverWorld = (ServerWorld) world;
		int level = getLevel(stack);
		Optional<Identifier> mobId = getMobId(stack);
		Optional<EntityType<? extends MobEntity>> explicitType = mobId.flatMap(id -> resolveMobType(serverWorld, id));
		if (mobId.isPresent() && explicitType.isEmpty()) {
			user.sendMessage(Text.literal("Invalid mob on key: " + mobId.get()), true);
			return TypedActionResult.fail(stack);
		}
		EntityType<? extends MobEntity> type = explicitType.orElseGet(() -> ModItems.getRandomMobType(world.getRandom()));
		String originId = getPlayerOriginIfEnabled(user);
		Vec3d spawnPos = chooseSpawnPos(serverWorld, user);
		if (spawnPos == null) {
			return TypedActionResult.pass(stack);
		}

		// Delay lightning + mob by 2 seconds (40 ticks) for effect.
		ServerTaskScheduler.schedule(serverWorld, 40, w -> spawnLightningAndMob(w, spawnPos, type, originId, level));

		if (!user.getAbilities().creativeMode) {
			stack.decrement(1);
		}
		user.getItemCooldownManager().set(this, 10);

		return TypedActionResult.success(stack, world.isClient);
	}

	private void spawnLightningAndMob(ServerWorld world, Vec3d spawnPos, EntityType<? extends MobEntity> type, String originId, int level) {
		var lightning = net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(world);
		if (lightning != null) {
			lightning.refreshPositionAfterTeleport(spawnPos.x, spawnPos.y, spawnPos.z);
			lightning.setCosmetic(true);
			world.spawnEntity(lightning);
		}

		MobEntity mob = createBuffedMob(world, type, level);
		if (mob == null) {
			return;
		}
		mob.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, world.getRandom().nextFloat() * 360.0F, 0.0F);
		if (originId != null && !originId.isEmpty()) {
			mob.addCommandTag("emplus:origin=" + originId);
		}
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

	private MobEntity createBuffedMob(ServerWorld world, EntityType<? extends MobEntity> type, int level) {
		MobEntity mob = type.create(world);
		if (mob == null) {
			return null;
		}

		BossScalingConfig.TierScaling scale = BossScalingConfigManager.getTierScaling(level);
		applyAttributeMultiplier(mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH), scale.maxHealth);
		applyAttributeMultiplier(mob.getAttributeInstance(EntityAttributes.GENERIC_ARMOR), scale.armor);
		applyAttributeMultiplier(mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE), scale.attackDamage);
		applyAttributeMultiplier(mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED), scale.movementSpeed);
		applyAttributeMultiplier(mob.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE), scale.knockbackResistance);
		applyAttributeMultiplier(mob.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE), scale.followRange);

		mob.setHealth(mob.getMaxHealth());
		mob.setPersistent();
		mob.addCommandTag(levelTag(level));

		int maxTier = BossScalingConfigManager.getMaxTier();
		int nextLevel = level + 1;
		if (nextLevel <= maxTier) {
			mob.addCommandTag(nextLevelTag(nextLevel));
		}

		return mob;
	}

	public static String nextLevelTag(int level) {
		return "emplus:mythical_key_next_level=" + level;
	}

	public static String levelTag(int level) {
		return "emplus:mythical_key_level=" + level;
	}

	private void applyAttributeMultiplier(EntityAttributeInstance instance, double multiplier) {
		if (instance == null) {
			return;
		}
		double newBase = Math.max(0.0D, instance.getBaseValue() * multiplier);
		instance.setBaseValue(newBase);
	}

	@Override
	public String getTranslationKey() {
		return "item.emplus.mythical_key";
	}

	@Override
	public Text getName(ItemStack stack) {
		int level = getLevel(stack);
		return Text.translatable(getTranslationKey(), level);
	}

	@Override
	public void appendTooltip(ItemStack stack, World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
		Identifier id = stack.hasNbt() && stack.getNbt().contains("Mob") ? Identifier.tryParse(stack.getNbt().getString("Mob")) : null;
		Text mobName = null;
		if (id != null) {
			mobName = getMobType(stack)
					.<Text>map(EntityType::getName)
					.orElse(Text.translatable("entity." + id.getNamespace() + "." + id.getPath()));
		}
		if (mobName != null) {
			tooltip.add(Text.translatable("tooltip.emplus.summons", mobName).formatted(Formatting.DARK_PURPLE));
		}
		super.appendTooltip(stack, world, tooltip, context);
	}

	public Optional<EntityType<? extends MobEntity>> getMobType(ItemStack stack) {
		return getMobId(stack).flatMap(this::resolveMobType);
	}

	public void setMob(ItemStack stack, EntityType<? extends MobEntity> type) {
		Identifier id = net.minecraft.registry.Registries.ENTITY_TYPE.getId(type);
		if (id != null) {
			stack.getOrCreateNbt().putString("Mob", id.toString());
		}
	}

	public int getLevel(ItemStack stack) {
		if (!stack.hasNbt() || !stack.getNbt().contains(LEVEL_KEY)) {
			return 1;
		}
		int level = stack.getNbt().getInt(LEVEL_KEY);
		return Math.max(1, level);
	}

	public void setLevel(ItemStack stack, int level) {
		stack.getOrCreateNbt().putInt(LEVEL_KEY, Math.max(1, level));
	}

	private String getPlayerOriginIfEnabled(PlayerEntity player) {
		ModConfig config = ConfigManager.get();
		if (!config.originsSpecificLoot) {
			return null;
		}
		return OriginLookup.getOriginId(player).orElse(null);
	}

	private Optional<Identifier> getMobId(ItemStack stack) {
		if (!stack.hasNbt() || !stack.getNbt().contains("Mob")) {
			return Optional.empty();
		}
		return Optional.ofNullable(Identifier.tryParse(stack.getNbt().getString("Mob")));
	}

	private Optional<EntityType<? extends MobEntity>> resolveMobType(Identifier id) {
		EntityType<?> type = net.minecraft.registry.Registries.ENTITY_TYPE.get(id);
		if (type != null && net.minecraft.entity.mob.MobEntity.class.isAssignableFrom(type.getBaseClass())) {
			@SuppressWarnings("unchecked")
			EntityType<? extends MobEntity> mobType = (EntityType<? extends MobEntity>) type;
			return Optional.of(mobType);
		}
		return Optional.empty();
	}

	private Optional<EntityType<? extends MobEntity>> resolveMobType(ServerWorld world, Identifier id) {
		Optional<EntityType<? extends MobEntity>> direct = resolveMobType(id);
		if (direct.isPresent()) {
			return direct;
		}
		EntityType<?> type = net.minecraft.registry.Registries.ENTITY_TYPE.get(id);
		if (type != null) {
			var created = type.create(world);
			if (created instanceof MobEntity) {
				@SuppressWarnings("unchecked")
				EntityType<? extends MobEntity> mobType = (EntityType<? extends MobEntity>) type;
				return Optional.of(mobType);
			}
		}
		return Optional.empty();
	}

	@Override
	public String toString() {
		return "MythicalKeyItem";
	}
}
