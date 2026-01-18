package taethaprod.emplus.games.artifacts;

import net.minecraft.client.item.CompassAnglePredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.CompassItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import taethaprod.emplus.ModItems;

public final class ArtifactClient {
	private ArtifactClient() {
	}

	public static void register() {
		ModelPredicateProviderRegistry.register(ModItems.ARTIFACT_LOCATOR, new Identifier("angle"),
				new CompassAnglePredicateProvider((world, stack, entity) -> {
					if (stack == null) {
						return null;
					}
					NbtCompound nbt = stack.getNbt();
					if (nbt == null) {
						return null;
					}
					return CompassItem.createLodestonePos(nbt);
				}));
	}
}
